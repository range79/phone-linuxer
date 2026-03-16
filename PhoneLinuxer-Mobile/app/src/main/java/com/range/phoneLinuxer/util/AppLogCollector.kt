package com.range.phoneLinuxer.util

import androidx.compose.runtime.mutableStateListOf
import timber.log.Timber

object AppLogCollector : Timber.Tree() {
    val logs = mutableStateListOf<String>()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (logs.size > 200) logs.removeAt(0)
        logs.add("[$tag] $message")
    }
}