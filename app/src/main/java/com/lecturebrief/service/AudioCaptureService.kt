package com.lecturebrief.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Foreground service scaffold for concurrent microphone + AudioPlaybackCapture (API 29+).
 */
class AudioCaptureService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }
}
