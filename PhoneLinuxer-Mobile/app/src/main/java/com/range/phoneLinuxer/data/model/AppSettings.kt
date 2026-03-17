package com.range.phoneLinuxer.data.model

data class AppSettings(
    val darkMode: DarkModeEnum = DarkModeEnum.SYSTEM,
    val useDynamicColors: Boolean = true,

    val themeColorArgb: Int = 0xFF6750A4.toInt(),

    val allowDownloadOnMobileData: Boolean = false,
    val phoneArchitecture: String = "arm64",
    val keepScreenOn: Boolean = true,

    val loggingEnabled: Boolean = true,
    val biometricLock: Boolean = false
)