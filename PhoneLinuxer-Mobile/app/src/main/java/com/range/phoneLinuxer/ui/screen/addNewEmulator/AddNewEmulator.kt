package com.range.phoneLinuxer.ui.screen.emulator

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.range.phoneLinuxer.data.enums.*
import com.range.phoneLinuxer.data.model.*
import com.range.phoneLinuxer.ui.screen.addNewEmulator.EasyInstallSection
import com.range.phoneLinuxer.ui.screen.addNewEmulator.KvmStatusCard
import com.range.phoneLinuxer.ui.screen.addNewEmulator.SectionHeader
import com.range.phoneLinuxer.ui.screen.addNewEmulator.SettingSlider
import com.range.phoneLinuxer.ui.screen.addNewEmulator.getCpuDescription
import com.range.phoneLinuxer.util.HardwareUtil
import com.range.phoneLinuxer.viewModel.EmulatorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewEmulatorScreen(
    viewModel: EmulatorViewModel,
    onBack: () -> Unit,
    onSave: (VirtualMachineSettings) -> Unit
) {
    val context = LocalContext.current
    var isSavingLocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            if (event is EmulatorViewModel.UiEvent.SaveSuccess) {
                onBack()
            }
        }
    }

    val deviceMaxRam = remember { HardwareUtil.getTotalRamMB(context) }
    val deviceMaxCores = remember { HardwareUtil.getTotalCores() }
    val hasKvmSupport = remember { HardwareUtil.isKvmSupported() }

    var vmName by remember { mutableStateOf("") }
    var selectedCpu by remember { mutableStateOf(CpuModel.MAX) }
    var ramAmount by remember { mutableFloatStateOf((deviceMaxRam / 4f).coerceIn(512f, deviceMaxRam.toFloat())) }
    var cpuCores by remember { mutableFloatStateOf((deviceMaxCores / 2f).coerceAtLeast(1f)) }
    var isGpuEnabled by remember { mutableStateOf(true) }
    var selectedIsoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedIsoName by remember { mutableStateOf("No ISO Selected") }

    var selectedResolution by remember { mutableStateOf("1024x768") }
    var vncPort by remember { mutableStateOf("5900") }
    var rdpPort by remember { mutableStateOf("3389") }
    var selectedScreenType by remember { mutableStateOf(ScreenType.VNC) }

    val isoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedIsoUri = it
            selectedIsoName = it.path?.substringAfterLast("/") ?: "Selected Image"
        }
    }

    var isEasyInstallEnabled by remember { mutableStateOf(false) }
    var ezUsername by remember { mutableStateOf("user") }
    var ezPassword by remember { mutableStateOf("1234") }
    var selectedDE by remember { mutableStateOf(DesktopEnvironment.XFCE) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New VM") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    Button(
                        onClick = {
                            if (vmName.isNotBlank() && !isSavingLocked) {
                                if (selectedCpu.requiresKvm() && !hasKvmSupport) {
                                    Toast.makeText(context, "HOST CPU requires KVM support!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }

                                isSavingLocked = true
                                onSave(VirtualMachineSettings(
                                    vmName = vmName,
                                    cpuModel = selectedCpu,
                                    ramMB = ramAmount.toInt(),
                                    cpuCores = cpuCores.toInt(),
                                    isoUri = selectedIsoUri?.toString(),
                                    isGpuEnabled = isGpuEnabled,
                                    screenType = selectedScreenType,
                                    screenResolution = selectedResolution,
                                    vncPort = vncPort.toIntOrNull() ?: 5900,
                                    rdpPort = rdpPort.toIntOrNull() ?: 3389,
                                    networkMode = NetworkMode.USER,
                                    easyInstall = isEasyInstallEnabled,
                                    easyInstallSettings = if (isEasyInstallEnabled) {
                                        EasyInstallSettings(ezUsername, ezPassword, selectedDE)
                                    } else null
                                ))
                            }
                        },
                        enabled = vmName.isNotBlank() && !isSavingLocked
                    ) {
                        if (isSavingLocked) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            OutlinedTextField(
                value = vmName,
                onValueChange = { vmName = it },
                label = { Text("VM Name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSavingLocked
            )

            Card(
                onClick = { if (!isSavingLocked) isoPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSavingLocked
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileOpen, null)
                    Spacer(Modifier.width(12.dp))
                    Text(selectedIsoName)
                }
            }

            SectionHeader("Display & Graphics")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScreenType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedScreenType == type,
                        onClick = { if (!isSavingLocked) selectedScreenType = type },
                        label = { Text(type.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = if (selectedScreenType == ScreenType.VNC) vncPort else rdpPort,
                    onValueChange = { if (selectedScreenType == ScreenType.VNC) vncPort = it else rdpPort = it },
                    label = { Text("Port") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSavingLocked
                )
                OutlinedTextField(
                    value = selectedResolution,
                    onValueChange = { selectedResolution = it },
                    label = { Text("Resolution") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSavingLocked
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Enable Virtio-GPU")
                    Text("Hardware acceleration (if supported)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = isGpuEnabled, onCheckedChange = { if (!isSavingLocked) isGpuEnabled = it })
            }

            EasyInstallSection(
                isEnabled = isEasyInstallEnabled, onToggle = { if (!isSavingLocked) isEasyInstallEnabled = it },
                username = ezUsername, onUsernameChange = { ezUsername = it },
                password = ezPassword, onPasswordChange = { ezPassword = it },
                selectedDE = selectedDE, onDEChange = { selectedDE = it }
            )

            SectionHeader("Resources")
            KvmStatusCard(hasKvmSupport)

            CpuModelDropdown(
                selectedModel = selectedCpu,
                hasKvm = hasKvmSupport,
                onModelSelected = { selectedCpu = it }
            )

            SettingSlider("RAM: ${ramAmount.toInt()} MB", ramAmount, { ramAmount = it }, 512f..deviceMaxRam.toFloat(), 10, Icons.Default.Memory)
            SettingSlider("Cores: ${cpuCores.toInt()}", cpuCores, { cpuCores = it }, 1f..deviceMaxCores.toFloat(), deviceMaxCores - 1, Icons.Default.SettingsInputComponent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuModelDropdown(
    selectedModel: CpuModel,
    hasKvm: Boolean,
    onModelSelected: (CpuModel) -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = if (selectedModel.requiresKvm() && !hasKvm) "${selectedModel.name} (Unsupported)" else selectedModel.name,
            onValueChange = {},
            readOnly = true,
            label = { Text("CPU Architecture") },
            supportingText = { Text(getCpuDescription(selectedModel)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            isError = selectedModel.requiresKvm() && !hasKvm
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            CpuModel.entries.forEach { model ->
                val isUnsupported = model.requiresKvm() && !hasKvm

                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = if (isUnsupported) "${model.name} (KVM Required)" else model.name,
                                color = if (isUnsupported) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = getCpuDescription(model),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnsupported) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    onClick = {
                        if (isUnsupported) {
                            Toast.makeText(context, "Device does not support KVM!", Toast.LENGTH_SHORT).show()
                        } else {
                            onModelSelected(model)
                            expanded = false
                        }
                    }
                )
            }
        }
    }
}