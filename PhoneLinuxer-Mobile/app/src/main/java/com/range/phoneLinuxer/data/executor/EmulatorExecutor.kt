package com.range.phoneLinuxer.data.executor

import android.content.Context
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

    fun getLogStream(vmId: String): SharedFlow<String> {
        return _logStreams.getOrPut(vmId) {
            MutableSharedFlow(replay = 100, extraBufferCapacity = 500)
        }.asSharedFlow()
    }

    suspend fun executeCommand(
        vmId: String,
        fullCommand: List<String>
    ): Result<Long> = withContext(Dispatchers.IO) {

        if (isAlive(vmId)) {
            return@withContext Result.failure(Exception("VM '$vmId' is already running."))
        }

        try {
            val nativeLibDir = context.applicationInfo.nativeLibraryDir
            val systemLibDir = "/system/lib64"
            val vendorLibDir = "/vendor/lib64"

            val qemuBinaryName = "libqemu_system.so"
            val qemuFile = File(nativeLibDir, qemuBinaryName)

            if (!qemuFile.exists()) {
                Timber.tag(TAG).e("CRITICAL: Binary not found at ${qemuFile.absolutePath}")
                return@withContext Result.failure(Exception("Binary missing!"))
            }

            qemuFile.setExecutable(true, false)

            val finalCommand = fullCommand.toMutableList().apply { this[0] = qemuFile.absolutePath }
            val pb = ProcessBuilder(finalCommand)

            val env = pb.environment()
            env["LD_LIBRARY_PATH"] = "$nativeLibDir:$systemLibDir:$vendorLibDir:${env["LD_LIBRARY_PATH"] ?: ""}"
            env["TMPDIR"] = context.cacheDir.absolutePath
            env["HOME"] = context.filesDir.absolutePath

            pb.directory(context.filesDir)
            pb.redirectErrorStream(true)

            Timber.tag(TAG).i("Launching VM: $vmId")

            val process = pb.start()
            runningProcesses[vmId] = process

            logJobs[vmId] = launchLogReader(vmId, process.inputStream)

            Result.success(1L)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start VM: $vmId")
            Result.failure(e)
        }
    }

    private fun launchLogReader(vmId: String, inputStream: InputStream): Job = executorScope.launch {
        val flow = _logStreams.getOrPut(vmId) {
            MutableSharedFlow(replay = 100, extraBufferCapacity = 500)
        }

        try {
            inputStream.bufferedReader().use { reader ->
                while (isActive) {
                    val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break

                    flow.emit(line)
                }
            }
        } catch (e: Exception) {
            if (isActive) {
                Timber.tag(TAG).e("Log stream broken for $vmId: ${e.message}")
                flow.emit("!! LOG ERROR: ${e.message}")
            }
        } finally {
            cleanup(vmId)
            Timber.tag(TAG).i("Log reader stopped for $vmId")
        }
    }

    fun killProcess(vmId: String) {
        logJobs[vmId]?.cancel()
        runningProcesses[vmId]?.let { process ->
            if (process.isAlive) {
                Timber.tag(TAG).w("Killing VM: $vmId")
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
            Timber.tag(TAG).i("Cleaning up all processes...")
            runningProcesses.keys.forEach { killProcess(it) }
        }
    }

    private fun cleanup(vmId: String) {
        runningProcesses.remove(vmId)
        logJobs.remove(vmId)
    }

    fun isAlive(vmId: String): Boolean = runningProcesses[vmId]?.isAlive ?: false
}