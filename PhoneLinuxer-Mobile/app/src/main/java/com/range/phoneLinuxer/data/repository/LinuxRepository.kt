package com.range.phoneLinuxer.data.repository

interface LinuxRepository {
    suspend fun downloadLinux(url: String, onProgress: (Int) -> Unit)
}