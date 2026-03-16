package com.range.phoneLinuxer.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.range.phoneLinuxer.data.repository.LinuxRepositoryImpl
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class LinuxViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext
    private var repo: LinuxRepositoryImpl? = null
    private var downloadJob: Job? = null

    private val _downloadPath = MutableStateFlow<Uri?>(null)
    val downloadPath = _downloadPath.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow("Idle")
    val downloadStatus = _downloadStatus.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    private val _downloadSpeed = MutableStateFlow("0 KB/s")
    val downloadSpeed = _downloadSpeed.asStateFlow()

    private val _remainingTime = MutableStateFlow("")
    val remainingTime = _remainingTime.asStateFlow()

    private var currentUrl = ""
    private var currentLabel = ""

    fun chooseDownloadPath(uri: Uri) {
        _downloadPath.value = uri
        repo = LinuxRepositoryImpl(appContext, uri)
    }

    fun downloadArch() {
        startDownload(
            label = "Arch Linux ARM",
            url = "https://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz"
        )
    }

    fun downloadUbuntu() {
        startDownload(
            label = "Ubuntu 24.04",
            url = "https://cdimage.ubuntu.com/ubuntu-server/noble/daily-live/current/noble-live-server-arm64.iso"
        )
    }

    fun togglePauseResume() {
        if (_isPaused.value) {
            _isPaused.value = false
            startDownload(currentLabel, currentUrl)
        } else {
            _isPaused.value = true
            downloadJob?.cancel()
            _downloadStatus.value = "Paused"
            _downloadSpeed.value = "0 KB/s"
            _remainingTime.value = ""
        }
    }

    fun cancelDownload() {
        _isPaused.value = false
        downloadJob?.cancel()
        _isDownloading.value = false
        _downloadStatus.value = "Canceled"
        _downloadProgress.value = 0
        _downloadSpeed.value = "0 KB/s"
        _remainingTime.value = ""
    }

    private fun startDownload(label: String, url: String) {
        val repository = repo ?: run {
            _downloadStatus.value = "Error: Select folder first"
            return
        }

        currentUrl = url
        currentLabel = label

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _isDownloading.value = true
            _downloadStatus.value = "Connecting..."
            val startTime = System.currentTimeMillis()

            repository.downloadLinux(url) { downloaded, total, isError ->
                if (isError) {
                    _downloadStatus.value = "Error: Connection Lost"
                    _downloadProgress.value = -1
                    _isDownloading.value = false
                } else if (!_isPaused.value) {
                    val progress = if (total > 0) ((downloaded * 100) / total).toInt() else 0
                    _downloadProgress.value = progress
                    _downloadStatus.value = "Downloading $label..."

                    val timeSec = (System.currentTimeMillis() - startTime) / 1000.0
                    if (timeSec > 0.5) {
                        val speedInBytes = downloaded / timeSec
                        _downloadSpeed.value = formatSpeed(speedInBytes)

                        if (speedInBytes > 0 && total > 0) {
                            val remainingBytes = total - downloaded
                            val etaSeconds = (remainingBytes / speedInBytes).toLong()
                            _remainingTime.value = formatEta(etaSeconds)
                        }
                    }

                    if (progress == 100) {
                        _downloadStatus.value = "Success: $label ready"
                        _isDownloading.value = false
                        _downloadSpeed.value = "0 KB/s"
                        _remainingTime.value = ""
                    }
                }
            }
        }
    }

    private fun formatSpeed(speedBps: Double): String {
        return if (speedBps >= 1024 * 1024) {
            "%.1f MB/s".format(speedBps / (1024 * 1024))
        } else {
            "%.1f KB/s".format(speedBps / 1024)
        }
    }

    private fun formatEta(seconds: Long): String {
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${ (seconds % 3600) / 60}m left"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s left"
            else -> "${seconds}s left"
        }
    }
}