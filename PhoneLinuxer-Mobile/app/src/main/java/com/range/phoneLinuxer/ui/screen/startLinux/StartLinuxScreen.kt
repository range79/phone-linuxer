package com.range.phoneLinuxer.ui.screen.startLinux

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.range.phoneLinuxer.data.model.VirtualMachineSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartLinuxScreen(
    onBack: () -> Unit,
    onAddEmulator: () -> Unit,
    onStartVM: (VirtualMachineSettings) -> Unit,
    onDeleteVM: (String) -> Unit,
    vms: List<VirtualMachineSettings> = emptyList()
) {
    var vmToDelete by remember { mutableStateOf<VirtualMachineSettings?>(null) }

    vmToDelete?.let { vm ->
        AlertDialog(
            onDismissRequest = { vmToDelete = null },
            title = { Text("Delete VM") },
            text = { Text("Are you sure you want to delete '${vm.vmName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteVM(vm.id)
                        vmToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { vmToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Virtual Machines") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onAddEmulator) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("New VM")
                    }
                }
            )
        }
    ) { padding ->
        if (vms.isEmpty()) {
            EmptyVMState(onAddEmulator, Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vms, key = { it.id }) { vm ->
                    VMCard(
                        vm = vm,
                        onStart = { onStartVM(vm) },
                        onDelete = { vmToDelete = vm }
                    )
                }
            }
        }
    }
}

@Composable
fun VMCard(
    vm: VirtualMachineSettings,
    onStart: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vm.vmName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Resolution: ${vm.screenResolution}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete VM",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    FilledIconButton(
                        onClick = onStart,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start VM")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(icon = Icons.Default.Memory, label = "${vm.ramMB} MB")
                InfoChip(icon = Icons.Default.SettingsInputComponent, label = "${vm.cpuCores} Cores")
                if (vm.isGpuEnabled) {
                    InfoChip(icon = Icons.Default.Terminal, label = "GPU On")
                }
            }
        }
    }
}

@Composable
fun InfoChip(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun EmptyVMState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No virtual machines found", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAdd) {
            Text("Create Your First VM")
        }
    }
}