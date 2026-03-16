package com.range.phoneLinuxer.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.CancellationException
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
        }
    }

    suspend fun downloadLinux(url: String, onProgress: (Long, Long, Boolean) -> Unit) {
        try {
            val pickedDir = DocumentFile.fromTreeUri(context, folderUri) ?: throw Exception("Invalid Folder")
            val fileName = url.substringAfterLast("/")

            var file = pickedDir.findFile(fileName)
            val startByte = file?.length() ?: 0L

            if (file == null) {
                file = pickedDir.createFile("application/octet-stream", fileName) ?: throw Exception("File Creation Failed")
            }

            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(file!!.uri, "wa")?.use { raw ->
                    BufferedOutputStream(raw).use { output ->
                        client.prepareGet(url) {
                            if (startByte > 0) header(HttpHeaders.Range, "bytes=$startByte-")
                        }.execute { response ->
                            if (!response.status.isSuccess() && response.status != HttpStatusCode.PartialContent)
                                throw Exception("HTTP ${response.status.value}")

                            val contentSize = response.contentLength() ?: 0L
                            val totalBytes = if (startByte > 0) contentSize + startByte else contentSize
                            val channel = response.bodyAsChannel()
                            val buffer = ByteArray(64 * 1024)
                            var bytesCopied = startByte

                            while (!channel.isClosedForRead) {
                                if (!currentCoroutineContext().isActive) throw CancellationException()

                                val read = channel.readAvailable(buffer)
                                if (read == -1) break
                                if (read > 0) {
                                    output.write(buffer, 0, read)
                                    bytesCopied += read
                                    onProgress(bytesCopied, totalBytes, false)
                                }
                            }
                            if (totalBytes > 0 && bytesCopied < totalBytes) throw Exception("Incomplete")
                            output.flush()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val isManual = e is CancellationException || e.cause is CancellationException
            onProgress(0, 0, !isManual)
        }
    }
}