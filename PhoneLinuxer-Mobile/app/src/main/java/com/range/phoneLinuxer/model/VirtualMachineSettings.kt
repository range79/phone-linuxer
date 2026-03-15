package com.range.phoneLinuxer.model

data class VirtualMachineSettings(
    val vmName: String = "My Linux",
    val ramMB: Int = 2048,
    val cpuCores: Int = 4,
    val screenResolution: String = "1024x768",
    val isGpuEnabled: Boolean = true,
    val isoUri: String? = null,
    val diskImgPath: String? = null
)