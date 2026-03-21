package com.range.phoneLinuxer.data.executor

import android.content.Context
import android.system.Os
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

    private val injectLibDir = File(context.filesDir, "injected_libs")

    private val systemBlacklist = listOf(
        "libEGL.so", "libGLESv1_CM.so", "libGLESv2.so", "libGLESv3.so",
        "libvulkan.so", "libgui.so", "libui.so", "libandroid.so",
        "libutils.so", "libc++.so", "libm.so", "libc.so", "libdl.so"
    )

    fun getLogStream(vmId: String): SharedFlow<String> = _logStreams.getOrPut(vmId) {
        MutableSharedFlow(replay = 100, extraBufferCapacity = 500)
    }.asSharedFlow()

    private fun injectAndLinkLibs() {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        try {
            if (!injectLibDir.exists()) injectLibDir.mkdirs()
            injectLibDir.listFiles()?.forEach { it.delete() }

            File(nativeLibDir).listFiles()?.forEach { file ->
                val fileName = file.name

                if (systemBlacklist.contains(fileName)) return@forEach

                if (fileName == "libz.so" || fileName == "libz_so_1.so") {
                    createSymlink(file, "libz.so.1")
                }

                if (fileName.contains("_so") && fileName.endsWith(".so")) {
                    val originalName = fileName.substringBeforeLast(".so")
                        .replace("_so", ".so")
                        .replace("_", ".")
                    createSymlink(file, originalName)
                }

                createSymlink(file, fileName)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Linking failed")
        }
    }

    private fun createSymlink(target: File, linkName: String) {
        val linkFile = File(injectLibDir, linkName)
        try {
            Os.symlink(target.absolutePath, linkFile.absolutePath)
        } catch (e: Exception) {
            Timber.tag(TAG).v("Symlink skip: $linkName")
        }
    }

    suspend fun executeCommand(vmId: String, fullCommand: List<String>): Result<Long> = withContext(Dispatchers.IO) {
        if (isAlive(vmId)) return@withContext Result.failure(Exception("Already running"))

        try {
            injectAndLinkLibs()

            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val qemuBinary = File(nativeLibDir, "libqemu_system.so")
            qemuBinary.setExecutable(true, false)

            val pb = ProcessBuilder(fullCommand.toMutableList().apply { this[0] = qemuBinary.absolutePath })
            val env = pb.environment()

            val ldPath = "${injectLibDir.absolutePath}:$nativeLibDir:/system/lib64:/vendor/lib64"
            env["LD_LIBRARY_PATH"] = ldPath

            val libz = File(injectLibDir, "libz.so.1")
            if (libz.exists()) {
                env["LD_PRELOAD"] = libz.absolutePath
                Timber.tag(TAG).i("🚀 Force-loading libz: ${libz.absolutePath}")
            }

            env["TMPDIR"] = context.cacheDir.absolutePath
            env["HOME"] = context.filesDir.absolutePath

            pb.directory(context.filesDir)
            pb.redirectErrorStream(true)

            val process = pb.start()
            runningProcesses[vmId] = process
            logJobs[vmId] = launchLogReader(vmId, process.inputStream)

            Result.success(1L)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun launchLogReader(vmId: String, inputStream: InputStream): Job = executorScope.launch {
        val flow = _logStreams.getOrPut(vmId) { MutableSharedFlow(replay = 100, extraBufferCapacity = 500) }
        try {
            inputStream.bufferedReader().use { reader ->
                while (isActive) {
                    val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                    flow.emit(line)
                }
            }
        } finally { cleanup(vmId) }
    }

    fun killProcess(vmId: String) {
        logJobs[vmId]?.cancel()
        runningProcesses[vmId]?.let { process ->
            if (process.isAlive) {
                process.destroy()
                executorScope.launch {
                    delay(500)
                    if (process.isAlive) process.destroyForcibly()
                }
            }
        }
        cleanup(vmId)
    }

    fun killAll() {
        if (runningProcesses.isNotEmpty()) {
            runningProcesses.keys.forEach { killProcess(it) }
        }
    }

    private fun cleanup(vmId: String) {
        runningProcesses.remove(vmId)
        logJobs.remove(vmId)
    }

    fun isAlive(vmId: String): Boolean = runningProcesses[vmId]?.isAlive ?: false
}