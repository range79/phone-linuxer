package com.range.phoneLinuxer.data.model

enum class DarkModeEnum {
    LIGHT,
    DARK,
    SYSTEM;

    override fun toString(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}