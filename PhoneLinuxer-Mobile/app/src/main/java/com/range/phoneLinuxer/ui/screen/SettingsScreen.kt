package com.range.phoneLinuxer.ui.screen

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.range.phoneLinuxer.BuildConfig
import com.range.phoneLinuxer.data.model.AppSettings
import com.range.phoneLinuxer.data.model.DarkModeEnum
import com.range.phoneLinuxer.data.repository.SettingsRepository
import com.range.phoneLinuxer.util.CacheCleaner
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit,
    repository: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val cacheCleaner = remember { CacheCleaner(context) }

    val settings by repository.settingsFlow.collectAsState(initial = AppSettings())

    var cacheSize by remember { mutableStateOf("Calculating...") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var showDarkModeSheet by remember { mutableStateOf(false) }
    var showColorSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        cacheSize = cacheCleaner.getTotalCacheSizeFormatted()
    }

    if (showDarkModeSheet) {
        ModalBottomSheet(onDismissRequest = { showDarkModeSheet = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
                DarkModeEnum.entries.forEach { mode ->
                    ListItem(
                        headlineContent = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        leadingContent = {
                            Icon(
                                imageVector = when(mode) {
                                    DarkModeEnum.LIGHT -> Icons.Default.LightMode
                                    DarkModeEnum.DARK -> Icons.Default.DarkMode
                                    DarkModeEnum.SYSTEM -> Icons.Default.SettingsSuggest
                                },
                                contentDescription = null
                            )
                        },
                        trailingContent = { RadioButton(selected = settings.darkMode == mode, onClick = null) },
                        modifier = Modifier.clickable {
                            scope.launch {
                                repository.updateDarkMode(mode)
                                showDarkModeSheet = false
                            }
                        }
                    )
                }
            }
        }
    }

    if (showColorSheet) {
        val colorOptions = listOf(
            Color(0xFF6750A4), Color(0xFF388E3C), Color(0xFF1976D2),
            Color(0xFFD32F2F), Color(0xFFFBC02D), Color(0xFF0097A7)
        )

        ModalBottomSheet(onDismissRequest = { showColorSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 24.dp)) {
                Text("Theme Color", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colorOptions.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    scope.launch {
                                        repository.updateSettings(settings.copy(themeColorArgb = color.toArgb()))
                                        showColorSheet = false
                                    }
                                }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.themeColorArgb == color.toArgb()) {
                                Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Clear Cache?") },
            text = { Text("Temporary installation files and logs will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val recovered = cacheCleaner.clearAllCache()
                        cacheSize = "0.00 B"
                        showDeleteDialog = false
                        scope.launch { snackBarHostState.showSnackbar("Recovered: $recovered") }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            item { SettingHeader("Appearance") }
            item {
                SettingItem(
                    title = "Theme Mode",
                    subtitle = settings.darkMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.DarkMode,
                    onClick = { showDarkModeSheet = true },
                    trailing = { Icon(Icons.Default.ChevronRight, null) }
                )
            }
            item {
                SettingSwitchItem(
                    title = "Dynamic Colors",
                    subtitle = "Use system wallpaper colors (Android 12+)",
                    icon = Icons.Default.Palette,
                    checked = settings.useDynamicColors,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateSettings(settings.copy(useDynamicColors = checked)) }
                    }
                )
            }
            item {
                AnimatedVisibility(
                    visible = !settings.useDynamicColors,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SettingItem(
                        title = "App Color",
                        subtitle = "Choose your primary theme color",
                        icon = Icons.Default.ColorLens,
                        onClick = { showColorSheet = true },
                        trailing = {
                            Box(Modifier.size(20.dp).clip(CircleShape).background(Color(settings.themeColorArgb)))
                        }
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingHeader("Network & Downloads") }
            item {
                SettingSwitchItem(
                    title = "Allow Mobile Data",
                    subtitle = "Download Linux ISOs using cellular data",
                    icon = Icons.Default.SignalCellularAlt,
                    checked = settings.allowDownloadOnMobileData,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateSettings(settings.copy(allowDownloadOnMobileData = checked)) }
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingHeader("Storage & Diagnostics") }
            item {
                SettingSwitchItem(
                    title = "Keep Screen On",
                    subtitle = "Prevent device sleep during installation",
                    icon = Icons.Default.LightMode,
                    checked = settings.keepScreenOn,
                    onCheckedChange = { checked ->
                        scope.launch { repository.updateSettings(settings.copy(keepScreenOn = checked)) }
                    }
                )
            }
            item {
                SettingItem(
                    title = "View Session Logs",
                    subtitle = "Check for errors and VM status",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    onClick = onNavigateToLogs,
                    trailing = { Icon(Icons.Default.ChevronRight, null) }
                )
            }
            item {
                SettingItem(
                    title = "Clear Cache",
                    subtitle = "Current usage: $cacheSize",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { showDeleteDialog = true }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingHeader("About") }
            item {
                SettingItem(title = "Developer", subtitle = "range79", icon = Icons.Default.Person)
            }
            item {
                SettingItem(
                    title = "GitHub",
                    subtitle = "github.com/range79",
                    icon = Icons.Default.Code,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/range79".toUri())
                        context.startActivity(intent)
                    },
                    trailing = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(18.dp)) }
                )
            }
            item {
                SettingItem(title = "App Version", subtitle = BuildConfig.VERSION_NAME, icon = Icons.Default.Info)
            }
        }
    }
}

@Composable
fun SettingHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp, 8.dp)
    )
}

@Composable
fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(onClick = { onClick?.invoke() }, enabled = onClick != null) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle) },
            leadingContent = { Icon(icon, null) },
            trailingContent = trailing
        )
    }
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) }
    )
}