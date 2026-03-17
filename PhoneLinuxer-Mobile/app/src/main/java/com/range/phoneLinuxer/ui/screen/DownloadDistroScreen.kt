package com.range.phoneLinuxer.ui.screen

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.range.phoneLinuxer.data.model.AppSettings
import com.range.phoneLinuxer.data.repository.SettingsRepository
import com.range.phoneLinuxer.util.NetworkObserver
import com.range.phoneLinuxer.viewModel.LinuxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    vm: LinuxViewModel,
    settingsRepository: SettingsRepository,
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
    val settings by settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

    val networkObserver = remember { NetworkObserver(context) }
    val networkStatus by networkObserver.observe.collectAsState(initial = NetworkObserver.Status.Available)
    val isOnline = networkStatus == NetworkObserver.Status.Available

    var showMobileDataWarning by remember { mutableStateOf(false) }
    var selectedOsToDownload by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { vm.chooseDownloadPath(it) } }

    fun isCellularActive(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }

    fun triggerDownload(os: String, force: Boolean = false) {
        if (os == "ARCH") vm.downloadArch(force)
        else if (os == "UBUNTU") vm.downloadUbuntu(force)
    }

    fun handleButtonClick(os: String) {
        if (isCellularActive() && !settings.allowDownloadOnMobileData) {
            selectedOsToDownload = os
            showMobileDataWarning = true
        } else {
            triggerDownload(os, force = false)
        }
    }

    if (showMobileDataWarning) {
        AlertDialog(
            onDismissRequest = {
                showMobileDataWarning = false
                selectedOsToDownload = null
            },
            title = { Text("Mobile Data Warning") },
            text = { Text("Mobile data downloads are restricted. Do you want to download this file using your cellular plan anyway?") },
            confirmButton = {
                Button(
                    onClick = {
                        selectedOsToDownload?.let { triggerDownload(it, force = true) }
                        showMobileDataWarning = false
                        selectedOsToDownload = null
                    }
                ) { Text("Download Anyway") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showMobileDataWarning = false
                    selectedOsToDownload = null
                }) { Text("Cancel") }
            }
        )
    }

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
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = !isOnline) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("No Internet Connection", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("STORAGE LOCATION", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = downloadPath?.path ?: "No directory selected",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { launcher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloading && !isPaused
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (downloadPath == null) "Select Folder" else "Change Folder")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            DownloadButton(
                text = "Download Arch Linux ARM",
                enabled = isOnline && !isDownloading && !isPaused && downloadPath != null,
                onClick = { handleButtonClick("ARCH") }
            )

            Spacer(Modifier.height(16.dp))

            DownloadButton(
                text = "Download Ubuntu 24.04 (LTS)",
                enabled = isOnline && !isDownloading && !isPaused && downloadPath != null,
                onClick = { handleButtonClick("UBUNTU") }
            )

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = isDownloading || isPaused || progress > 0,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Column {
                    SupportSmallCard()
                    Spacer(Modifier.height(16.dp))
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
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun SupportSmallCard() {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Surface(
        onClick = {
            val intent = Intent(Intent.ACTION_VIEW, "https://buymeacoffee.com/darkrange6s".toUri())
            context.startActivity(intent)
        },
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Favorite, null, tint = Color(0xFFE91E63), modifier = Modifier.size(16.dp).graphicsLayer(scaleX = scale, scaleY = scale))
            Spacer(Modifier.width(12.dp))
            Text("Support the developer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DownloadProgressPanel(
    progress: Int, status: String, isDownloading: Boolean, isPaused: Boolean,
    speed: String, eta: String, onTogglePause: () -> Unit, onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        text = if (progress >= 0) "$progress%" else "Wait",
                        fontSize = 32.sp, fontWeight = FontWeight.Black,
                        color = if (progress == -1) MaterialTheme.colorScheme.error else if (isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (!isPaused && isDownloading && progress > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(speed, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                        Text(eta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            LinearProgressIndicator(
                progress = { if (progress > 0) progress / 100f else 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = if (isPaused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalIconButton(onClick = onTogglePause, enabled = progress != -1 && !status.contains("Success")) {
                    Icon(if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, "Control")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onCancel, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Close, "Cancel")
                }
            }
        }
    }
}

@Composable
fun DownloadButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.medium) {
        Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}