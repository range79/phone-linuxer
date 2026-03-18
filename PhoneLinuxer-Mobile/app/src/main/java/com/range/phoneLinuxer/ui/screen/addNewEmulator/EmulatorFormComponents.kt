package com.range.phoneLinuxer.ui.screen.addNewEmulator

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.range.phoneLinuxer.data.enums.CpuModel

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