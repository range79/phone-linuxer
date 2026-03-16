package com.range.phoneLinuxer.viewModel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.range.phoneLinuxer.data.repository.LinuxRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LinuxViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = getApplication<Application>().applicationContext
    private var repo: LinuxRepositoryImpl? = null

    private val _downloadPath = MutableStateFlow<Uri?>(null)
    val downloadPath: StateFlow<Uri?> = _downloadPath

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _downloadStatus = MutableStateFlow("Idle")
    val downloadStatus: StateFlow<String> = _downloadStatus

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    fun chooseDownloadPath(uri: Uri) {
        _downloadPath.value = uri
        repo = LinuxRepositoryImpl(appContext, uri)
    }

    fun downloadArch() {
        startDownload(
            "Arch Linux ARM",
            "https://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz"
        )
    }

    fun downloadUbuntu() {
        startDownload(
            "Ubuntu 24.04 ARM64",
            "https://cdimage.ubuntu.com/ubuntu-server/noble/daily-live/current/noble-live-server-arm64.iso"
        )
    }

    private fun startDownload(label: String, url: String) {
        val repository = repo ?: run {
            _downloadStatus.value = "Error: Select folder first"
            return
        }

        viewModelScope.launch {
            _isDownloading.value = true
            _downloadStatus.value = "Downloading $label..."
            _downloadProgress.value = 0

            repository.downloadLinux(url) { progress ->
                _downloadProgress.value = progress

                when (progress) {
                    100 -> {
                        _downloadStatus.value = "Success: $label ready"
                        _isDownloading.value = false
                    }
                    -1 -> {
                        _downloadStatus.value = "Error: Download failed"
                        _isDownloading.value = false
                    }
                }
            }
        }
    }
}