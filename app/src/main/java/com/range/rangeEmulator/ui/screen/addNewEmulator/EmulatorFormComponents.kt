package com.range.rangeEmulator.ui.screen.addNewEmulator

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.range.rangeEmulator.data.enums.Architecture
import com.range.rangeEmulator.data.enums.CpuModel
import com.range.rangeEmulator.data.enums.OsType

@Composable
fun SectionHeader(title: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
    }
}

@Composable
fun KvmStatusCard(hasKvm: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (hasKvm) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (hasKvm) "✅ KVM Supported" else "⚠️ KVM Not Detected",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun SettingSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    icon: ImageVector
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

fun getCpuDescription(model: CpuModel): String = when (model) {
    CpuModel.HOST ->
        "KVM Passthrough: Native hardware access. | Performance: 95-99% (Native Speed)"

    CpuModel.CORTEX_A76 ->
        "Modern ARM64: Optimized for 64-bit distros. | Performance: ~45-55%"

    CpuModel.CORTEX_A72 ->
        "Standard ARM64: High stability, broad support. | Performance: ~35-40% (Stable Execution)"

    CpuModel.CORTEX_A53 ->
        "Power Efficient: Low thermal profile. | Performance: ~25-30% (Best for Background)"

    CpuModel.MAX ->
        "Full Feature Emulation: Software-based features. | Performance: ~15-20% (Heavy Load)"

    CpuModel.QEMU64 ->
        "Binary Translation (x86_64): Cross-arch overhead. | Performance: ~5-10% (Usable for CLI)"

    CpuModel.HASWELL ->
        "Complex x86_64: AVX2 translation stress. | Performance: ~2-5% (High Heat)"

    CpuModel.NEOVERSE_N1 ->
        "Cloud-Class ARM: Multi-threaded workloads. | Performance: ~40-45% (High Throughput)"

    else -> "Generic Model: Standard software emulation. | Performance: ~20-25% (Basic Use)"
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpuModelDropdown(
    selectedModel: CpuModel,
    hasKvm: Boolean,
    arch: Architecture = Architecture.AARCH64,
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
            CpuModel.entries.filter { it.getArch() == arch.toQemuArch() }.forEach { model ->
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

@Composable
fun SystemConfigPanel(
    selectedOs: OsType,
    selectedArch: Architecture,
    selectedCpu: CpuModel,
    isTpmEnabled: Boolean,
    hasKvm: Boolean,
    onOsSelected: (OsType) -> Unit,
    onArchSelected: (Architecture) -> Unit,
    onCpuSelected: (CpuModel) -> Unit,
    onTpmSelected: (Boolean) -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("System Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OsType.entries.forEach { os ->
                    OsCard(
                        os = os,
                        isSelected = selectedOs == os,
                        onClick = { onOsSelected(os) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target Architecture", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Architecture.entries.forEach { arch ->
                        val isSelected = selectedArch == arch
                        Surface(
                            onClick = { onArchSelected(arch) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (arch == Architecture.AARCH64) Icons.Default.Memory else Icons.Default.Computer,
                                        null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        arch.toString().substringBefore(" ("),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Processor Model", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                CpuModelDropdown(
                    selectedModel = selectedCpu,
                    hasKvm = hasKvm,
                    arch = selectedArch,
                    onModelSelected = onCpuSelected
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("TPM 2.0 Security", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text("Required for Windows 11 features.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isTpmEnabled,
                        onCheckedChange = onTpmSelected
                    )
                }
            }
        }
    }
}

@Composable
private fun OsCard(
    os: OsType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (os) {
                OsType.LINUX -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                OsType.WINDOWS -> Color(0xFF0078D4).copy(alpha = 0.1f)
                OsType.ANDROID -> Color(0xFF3DDC84).copy(alpha = 0.1f)
                OsType.OTHER -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        } else MaterialTheme.colorScheme.surface,
        label = "bgColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            when (os) {
                OsType.LINUX -> MaterialTheme.colorScheme.secondary
                OsType.WINDOWS -> Color(0xFF0078D4)
                OsType.ANDROID -> Color(0xFF3DDC84)
                OsType.OTHER -> MaterialTheme.colorScheme.outline
            }
        } else MaterialTheme.colorScheme.outlineVariant,
        label = "borderColor"
    )

    val elevation by animateDpAsState(targetValue = if (isSelected) 4.dp else 0.dp, label = "elevation")

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, borderColor),
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor),
        elevation = CardDefaults.outlinedCardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = when (os) {
                    OsType.LINUX -> Icons.Default.Terminal
                    OsType.WINDOWS -> Icons.Default.Window
                    OsType.ANDROID -> Icons.Default.Android
                    OsType.OTHER -> Icons.Default.MoreHoriz
                },
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = os.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ISOListItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val fileName = remember(uri) {
        uri.path?.substringAfterLast("/") ?: "Selected ISO"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove ISO",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}