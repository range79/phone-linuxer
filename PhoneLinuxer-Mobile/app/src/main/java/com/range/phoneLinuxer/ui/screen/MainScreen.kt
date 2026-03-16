package com.range.phoneLinuxer.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.range.phoneLinuxer.ui.theme.PhoneLinuxerTheme
import com.range.phoneLinuxer.viewModel.LinuxViewModel

@Composable
fun MainScreen(
    vm: LinuxViewModel,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit
) {
    val downloadPath by vm.downloadPath.collectAsState()
    val progress by vm.downloadProgress.collectAsState()
    val status by vm.downloadStatus.collectAsState()
    val isDownloading by vm.isDownloading.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm.chooseDownloadPath(it) } }

    MainContent(
        downloadPath = downloadPath,
        progress = progress,
        status = status,
        isDownloading = isDownloading,
        onSelectFolder = { launcher.launch(null) },
        onDownloadArch = { vm.downloadArch() },
        onDownloadUbuntu = { vm.downloadUbuntu() },
        onNavigateToSettings = onNavigateToSettings,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    downloadPath: Uri?,
    progress: Int,
    status: String,
    isDownloading: Boolean,
    onSelectFolder: () -> Unit,
    onDownloadArch: () -> Unit,
    onDownloadUbuntu: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = onSelectFolder,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Select Download Folder")
            }

            Text(
                text = "Path: ${downloadPath?.path ?: "Not Selected"}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onDownloadArch,
                enabled = !isDownloading && downloadPath != null,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Download Arch Linux")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onDownloadUbuntu,
                enabled = !isDownloading && downloadPath != null,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Download Ubuntu 24.04")
            }

            Spacer(modifier = Modifier.height(40.dp))

            LinearProgressIndicator(
                progress = { if (progress > 0) progress / 100f else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            val textColor = if (progress == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            Text(
                text = status,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "Downloading State")
@Composable
fun MainPreviewDownloading() {
    PhoneLinuxerTheme {
        MainContent(
            downloadPath = Uri.EMPTY,
            progress = 65,
            status = "Downloading Ubuntu ARM64...",
            isDownloading = true,
            onSelectFolder = {},
            onDownloadArch = {},
            onDownloadUbuntu = {},
            onNavigateToSettings = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
fun MainPreviewError() {
    PhoneLinuxerTheme {
        MainContent(
            downloadPath = Uri.EMPTY,
            progress = -1,
            status = "Error: Connection timed out",
            isDownloading = false,
            onSelectFolder = {},
            onDownloadArch = {},
            onDownloadUbuntu = {},
            onNavigateToSettings = {},
            onBack = {}
        )
    }
}