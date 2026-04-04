package com.range.rangeEmulator.util

import android.content.Context
import android.os.Build
import android.os.PerformanceHintManager
import timber.log.Timber

object PerformanceHintManagerHelper {

    private var session: Any? = null // Using Any to avoid compile errors on older SDKs

    fun createPerformanceSession(context: Context, tids: IntArray): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false

        return try {
            val manager = context.getSystemService(Context.PERFORMANCE_HINT_SERVICE) as? PerformanceHintManager
            if (manager == null) return false

            // Target duration in nanoseconds (4ms for aggressive scaling)
            val targetDurationNanos = 4000000L
            
            // On API 31+, we can create a session
            @Suppress("NewApi")
            session = manager.createHintSession(tids, targetDurationNanos)
            
            Timber.i("ADPF: Aggressive PerformanceHintSession created for TIDs: ${tids.joinToString()}")
            true
        } catch (e: Exception) {
            Timber.e(e, "ADPF: Failed to create PerformanceHintSession")
            false
        }
    }

    fun reportHeavyWork() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val currentSession = session ?: return

        try {
            // Signal a massive workload gap (20ms work for a 4ms target)
            // This forces the Governor to stay at peak frequency
            @Suppress("NewApi")
            if (currentSession is PerformanceHintManager.Session) {
                currentSession.reportActualWorkDuration(20000000L) 
            }
        } catch (e: Exception) {
            // Silently fail to avoid log flooding
        }
    }

    fun closeSession() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val currentSession = session ?: return
        
        try {
            @Suppress("NewApi")
            if (currentSession is PerformanceHintManager.Session) {
                currentSession.close()
            }
            session = null
            Timber.i("ADPF: PerformanceHintSession closed")
        } catch (e: Exception) {
            Timber.e(e, "ADPF: Failed to close session")
        }
    }
}
