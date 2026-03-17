package com.range.phoneLinuxer.data.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.range.phoneLinuxer.data.model.AppSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream

class LinuxRepositoryImpl(
    private val context: Context,
    private val folderUri: Uri
) {
    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            socketTimeoutMillis = 30000
            connectTimeoutMillis = 60000
            requestTimeoutMillis = 100000
        }
    }

    suspend fun downloadLinux(
        url: String,
        settings: AppSettings,
        onProgress: (Long, Long, Boolean) -> Unit
    ) {
        try {
            if (!canDownload(settings)) {
                onProgress(0, 0, true)
                return
            }

            val pickedDir = DocumentFile.fromTreeUri(context, folderUri)
                ?: throw Exception("Invalid folder selection")

            val fileName = url.substringAfterLast("/")
            var file = pickedDir.findFile(fileName)

            val startByte = file?.length() ?: 0L

            if (file == null) {
                file = pickedDir.createFile("application/octet-stream", fileName)
                    ?: throw Exception("Could not create file")
            }

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(file.uri, "wa")?.use { rawStream ->
                    BufferedOutputStream(rawStream).use { output ->
                        client.prepareGet(url) {
                            if (startByte > 0) {
                                header(HttpHeaders.Range, "bytes=$startByte-")
                            }
                        }.execute { response ->
                            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent) {
                                throw Exception("Server error: ${response.status.value}")
                            }

                            val contentLength = response.contentLength() ?: 0L
                            val totalBytes = if (startByte > 0) contentLength + startByte else contentLength

                            val channel = response.bodyAsChannel()
                            val buffer = ByteArray(64 * 1024)
                            var bytesCopied = startByte

                            while (!channel.isClosedForRead) {
                                if (!currentCoroutineContext().isActive) break

                                if (!canDownload(settings)) {
                                    throw Exception("Mobile data restriction triggered")
                                }

                                val read = channel.readAvailable(buffer)
                                if (read == -1) break

                                if (read > 0) {
                                    output.write(buffer, 0, read)
                                    bytesCopied += read
                                    onProgress(bytesCopied, totalBytes, false)
                                }
                            }
                            output.flush()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val isCancelled = e is kotlinx.coroutines.CancellationException
            if (!isCancelled) {
                e.printStackTrace()
                onProgress(0, 0, true)
            }
        }
    }

    private fun canDownload(settings: AppSettings): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        val isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        return when {
            isWifi -> true
            isMobile -> settings.allowDownloadOnMobileData
            else -> false
        }
    }

    fun close() {
        client.close()
    }
}