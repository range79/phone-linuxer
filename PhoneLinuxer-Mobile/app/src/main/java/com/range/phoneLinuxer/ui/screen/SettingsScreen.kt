package com.range.phoneLinuxer.ui.screen

import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.range.phoneLinuxer.BuildConfig
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val context = LocalContext.current

    val hasFullStorageAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingHeader("Storage & Permissions") }
            item {
                SettingItem(
                    title = "All Files Access",
                    subtitle = if (hasFullStorageAccess) "Granted" else "Required for large ISOs",
                    icon = Icons.Default.Folder,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        }
                    },
                    trailing = {
                        Icon(
                            imageVector = if (hasFullStorageAccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (hasFullStorageAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingHeader("Diagnostics") }
            item {
                SettingItem(
                    title = "View Session Logs",
                    subtitle = "Debug download and VM errors",
                    icon = Icons.AutoMirrored.Filled.ListAlt,
                    onClick = onNavigateToLogs,
                    trailing = { Icon(Icons.Default.ChevronRight, null) }
                )
            }

            item {
                SettingItem(
                    title = "Clear Cache",
                    subtitle = "Delete temporary download chunks",
                    icon = Icons.Default.DeleteSweep,
                    onClick = { }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

            item { SettingHeader("About") }
            item {
                SettingItem(
                    title = "App Version",
                    subtitle = BuildConfig.VERSION_NAME ,
                    icon = Icons.Default.Info
                )
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
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
            leadingContent = { Icon(icon, contentDescription = null) },
            trailingContent = trailing
        )
    }
}