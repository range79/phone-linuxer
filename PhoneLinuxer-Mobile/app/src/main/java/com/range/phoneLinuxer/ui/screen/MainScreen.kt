package com.range.phoneLinuxer.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.range.phoneLinuxer.util.NetworkObserver
import com.range.phoneLinuxer.viewModel.LinuxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: LinuxViewModel,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val downloadPath by vm.downloadPath.collectAsState()
    val progress by vm.downloadProgress.collectAsState()
    val status by vm.downloadStatus.collectAsState()
    val isDownloading by vm.isDownloading.collectAsState()
    val isPaused by vm.isPaused.collectAsState()
    val downloadSpeed by vm.downloadSpeed.collectAsState()
    val remainingTime by vm.remainingTime.collectAsState()

    val networkObserver = remember { NetworkObserver(context) }
    val networkStatus by networkObserver.observe.collectAsState(initial = NetworkObserver.Status.Available)
    val isOnline = networkStatus == NetworkObserver.Status.Available

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm.chooseDownloadPath(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Center", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = !isOnline) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("No Internet Connection", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(Modifier.weight(0.5f))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Download Location", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = downloadPath?.path ?: "No folder selected",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { launcher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading && !isPaused
                    ) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (downloadPath == null) "Select Folder" else "Change Folder")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            DownloadButton(
                text = "Download Arch Linux ARM",
                enabled = isOnline && !isDownloading && !isPaused && downloadPath != null,
                onClick = { vm.downloadArch() }
            )

            Spacer(Modifier.height(12.dp))

            DownloadButton(
                text = "Download Ubuntu 24.04",
                enabled = isOnline && !isDownloading && !isPaused && downloadPath != null,
                onClick = { vm.downloadUbuntu() }
            )

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = isDownloading || isPaused || progress != 0,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                DownloadProgressPanel(
                    progress = progress,
                    status = status,
                    isDownloading = isDownloading,
                    isPaused = isPaused,
                    speed = downloadSpeed,
                    eta = remainingTime,
                    onTogglePause = { vm.togglePauseResume() },
                    onCancel = { vm.cancelDownload() }
                )
            }
        }
    }
}

@Composable
fun DownloadProgressPanel(
    progress: Int,
    status: String,
    isDownloading: Boolean,
    isPaused: Boolean,
    speed: String,
    eta: String,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (progress >= 0) "$progress%" else "Error",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            progress == -1 -> MaterialTheme.colorScheme.error
                            isPaused -> Color.Gray
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Text(status, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                if (!isPaused && isDownloading && progress > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(speed, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text(eta, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { if (progress > 0) progress / 100f else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = if (isPaused) Color.Gray else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalIconButton(
                    onClick = onTogglePause,
                    enabled = progress != -1 && !status.contains("Success")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause/Resume"
                    )
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = onCancel,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
    }
}

@Composable
fun DownloadButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}