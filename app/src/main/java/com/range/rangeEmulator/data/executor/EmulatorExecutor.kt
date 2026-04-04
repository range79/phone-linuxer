package com.range.rangeEmulator.data.executor

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class EmulatorExecutor(private val context: Context) {

    private val TAG = "EmulatorExecutor"
    private val executorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val runningProcesses = ConcurrentHashMap<String, Process>()
    private val logJobs = ConcurrentHashMap<String, Job>()
    private val _logStreams = ConcurrentHashMap<String, MutableSharedFlow<String>>()
    
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val libLinker = LibLinker(context)
    val downloader = EngineDownloader(context)
    val extractor = EngineExtractor(context)

    fun getLogStream(vmId: String): SharedFlow<String> = _logStreams.getOrPut(vmId) {
        MutableSharedFlow(replay = 100, extraBufferCapacity = 500)
    }.asSharedFlow()

    suspend fun downloadEngineZip(
        url: String,
        allowMobileData: Boolean,
        onProgress: (Long, Long, Boolean, Boolean) -> Unit
    ) = downloader.download(url, allowMobileData, onProgress)

    suspend fun extractEngineZip(onProgress: (Int) -> Unit): Boolean =
        extractor.extract(onProgress)

    suspend fun createDiskImage(
        path: String,
        format: String,
        sizeGB: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            libLinker.injectAndLink()
            
            val injectLibDir = libLinker.injectLibDir
            val qemuImgFiles = File(context.filesDir, "qemu-img")
            val qemuImgNative = File(context.applicationInfo.nativeLibraryDir, "qemu-img")
            val qemuImgBinary = when {
                qemuImgNative.exists() -> qemuImgNative
                qemuImgFiles.exists() -> qemuImgFiles
                else -> {
                    val fallback = File(context.filesDir, "libqemu-img.so")
                    if (fallback.exists()) fallback else null
                }
            } ?: return@withContext Result.failure(Exception("qemu-img binary not found"))

            qemuImgBinary.setExecutable(true, false)

            val cmd = mutableListOf<String>().apply {
                val linker = if (System.getProperty("os.arch")?.contains("64") == true) "/system/bin/linker64" else "/system/bin/linker"
                add(linker)
                add(qemuImgBinary.absolutePath)
                add("create")
                add("-f")
                add(format.lowercase())
                add(path)
                add("${sizeGB}G")
            }

            val pb = ProcessBuilder(cmd)
            val env = pb.environment()
            
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            env["LD_LIBRARY_PATH"] = "${injectLibDir.absolutePath}:${context.filesDir.absolutePath}:$nativeLibDir:/system/lib64:/vendor/lib64"
            
            val preloads = mutableListOf<String>()
            listOf("libz.so.1", "libz.so", "libglib-2.0.so").forEach { name ->
                val f = File(injectLibDir, name)
                if (f.exists()) preloads.add(f.absolutePath)
            }
            if (preloads.isNotEmpty()) env["LD_PRELOAD"] = preloads.joinToString(":")

            val process = pb.start()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) Result.success(Unit)
            else {
                val error = process.errorStream.bufferedReader().readText()
                Result.failure(Exception("qemu-img failed ($exitCode): $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeCommand(
        vmId: String,
        fullCommand: List<String>,
        isTurboEnabled: Boolean = false,
        onExit: ((Int) -> Unit)? = null
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (isAlive(vmId)) return@withContext Result.failure(Exception("Already running"))

        try {
            libLinker.injectAndLink()

            val qemuInFilesDir = File(context.filesDir, "libqemu_system.so")
            val qemuInNativeDir = File(context.applicationInfo.nativeLibraryDir, "libqemu_system.so")
            val qemuBinary = when {
                qemuInNativeDir.exists() -> {
                    if (qemuInFilesDir.exists()) qemuInFilesDir.delete()
                    qemuInNativeDir
                }
                qemuInFilesDir.exists() -> qemuInFilesDir
                else -> throw Exception("QEMU engine not found. Please download the engine first.")
            }
            qemuBinary.setExecutable(true, false)

            val biosDir = File(context.filesDir, "pc-bios")
            val patchedCommand = mutableListOf<String>().apply {
                val linker = if (System.getProperty("os.arch")?.contains("64") == true) "/system/bin/linker64" else "/system/bin/linker"
                add(linker)
                add(qemuBinary.absolutePath)
                addAll(fullCommand.drop(1))
                add("-L")
                add(biosDir.absolutePath)
            }

            val pb = ProcessBuilder(patchedCommand)
            val env = pb.environment()

            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val injectLibDir = libLinker.injectLibDir
            
            val ldPath = "${injectLibDir.absolutePath}:${context.filesDir.absolutePath}:$nativeLibDir:/system/lib64:/vendor/lib64"
            env["LD_LIBRARY_PATH"] = ldPath

            val preloads = mutableListOf<String>()
            val criticalLibs = listOf(
                "libz.so.1", "libz.so",
                "libnettle.so.8", "libnettle.so",
                "libhogweed.so.6", "libhogweed.so",
                "libgmp.so",
                "libidn2.so", "libunistring.so",
                "libgnutls.so.30", "libgnutls.so",
                "libglib-2.0.so.0", "libglib-2.0.so"
            )

            criticalLibs.forEach { name ->
                val file = File(injectLibDir, name)
                if (file.exists()) preloads.add(file.absolutePath)
            }

            if (preloads.isNotEmpty()) {
                env["LD_PRELOAD"] = preloads.joinToString(":")
            }

            env["PROOT_TMP_DIR"] = context.cacheDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["HOME"] = context.filesDir.absolutePath

            pb.directory(context.filesDir)
            pb.redirectErrorStream(true)

            _logStreams.getOrPut(vmId) { MutableSharedFlow(replay = 100, extraBufferCapacity = 500) }

            val process = pb.start()
            runningProcesses[vmId] = process
            logJobs[vmId] = launchLogReader(vmId, process.inputStream)

            if (isTurboEnabled) {
                launchPerformanceBooster(vmId, process)
            }

            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RangeEmulator:VM_RUNNING").apply {
                    acquire()
                }
            }

            if (onExit != null) {
                executorScope.launch {
                    try {
                        val exitCode = process.waitFor()
                        onExit(exitCode)
                    } catch (e: Exception) {
                        onExit(-1)
                    }
                }
            }

            Result.success(1L)
        } catch (e: Exception) {
            val flow = _logStreams.getOrPut(vmId) { MutableSharedFlow(replay = 100, extraBufferCapacity = 500) }
            executorScope.launch { flow.emit("LAUNCH ERROR: ${e.message}") }
            Result.failure(e)
        }
    }

    private fun launchPerformanceBooster(vmId: String, process: Process) = executorScope.launch {
        val pid = getProcessPid(process)
        if (pid <= 0) return@launch

        val boostedTids = mutableSetOf<Int>()
        var adpfInitialized = false

        while (isActive && isAlive(vmId)) {
            try {
                val taskDir = File("/proc/$pid/task")
                if (taskDir.exists()) {
                    val currentTids = taskDir.listFiles()?.mapNotNull { it.name.toIntOrNull() } ?: emptyList()
                    
                    currentTids.forEach { tid ->
                        if (tid !in boostedTids) {
                            android.os.Process.setThreadPriority(tid, -19)
                            boostedTids.add(tid)
                        }
                    }

                    if (!adpfInitialized && currentTids.isNotEmpty()) {
                        adpfInitialized = com.range.rangeEmulator.util.PerformanceHintManagerHelper.createPerformanceSession(
                            context,
                            currentTids.toIntArray()
                        )
                    }

                    if (adpfInitialized) {
                        com.range.rangeEmulator.util.PerformanceHintManagerHelper.reportHeavyWork()
                    }
                }
            } catch (e: Exception) {}
            delay(3000)
        }
        
        if (adpfInitialized) {
            com.range.rangeEmulator.util.PerformanceHintManagerHelper.closeSession()
        }
    }

    private fun getProcessPid(process: Process): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pidMethod = process.javaClass.getMethod("pid")
                (pidMethod.invoke(process) as Long).toInt()
            } else {
                val field = process.javaClass.getDeclaredField("pid")
                field.isAccessible = true
                field.getInt(process)
            }
        } catch (e: Exception) {
            -1
        }
    }

    private fun launchLogReader(vmId: String, inputStream: InputStream): Job = executorScope.launch {
        val flow = _logStreams.getOrPut(vmId) { MutableSharedFlow(replay = 100, extraBufferCapacity = 500) }
        try {
            inputStream.bufferedReader().use { reader ->
                while (isActive) {
                    val line = try {
                        withContext(Dispatchers.IO) { reader.readLine() }
                    } catch (e: java.io.IOException) {
                        null
                    } ?: break
                    
                    flow.emit(line)
                }
            }
        } catch (e: Exception) {
            if (e !is java.io.IOException && e !is java.util.concurrent.CancellationException) {
                flow.emit("LOG READER ERROR: ${e.message}")
            }
        } finally { cleanup(vmId) }
    }

    fun killProcess(vmId: String) {
        try {
            logJobs[vmId]?.cancel()
            val process = runningProcesses[vmId]
            if (process != null) {
                try { process.destroy() } catch (t: Throwable) {}
                executorScope.launch {
                    try { delay(500); process.destroy() } catch (t: Throwable) {}
                }
            }
        } catch (t: Throwable) {
        } finally { cleanup(vmId) }
    }

    fun killAll() = runningProcesses.keys.forEach { killProcess(it) }

    fun isAlive(vmId: String): Boolean {
        val process = runningProcesses[vmId] ?: return false
        return try { process.exitValue(); false }
        catch (e: IllegalThreadStateException) { true }
        catch (t: Throwable) { false }
    }

    fun hasRunningProcesses(): Boolean = runningProcesses.isNotEmpty()

    private fun cleanup(vmId: String) {
        runningProcesses.remove(vmId)
        logJobs.remove(vmId)
        
        if (runningProcesses.isEmpty()) {
            wakeLock?.let {
                if (it.isHeld) it.release()
                wakeLock = null
            }
        }
    }
}