package com.range.phoneLinuxer.data.model

import java.util.UUID

data class VirtualMachineSettings(
    val id: String = UUID.randomUUID().toString(),
    val vmName: String,
    val ramMB: Int = 2048,
    val cpuCores: Int = 4,
    val screenResolution: String = "1024x768",
    val isGpuEnabled: Boolean = true,
    val isoUri: String? = null,
    val diskImgPath: String? = null
)