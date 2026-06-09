package com.crosspoint.reader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.readium.r2.streamer.Streamer

@HiltAndroidApp
class CrossPointApp : Application() {

    lateinit var streamer: Streamer
        private set

    override fun onCreate() {
        super.onCreate()
        streamer = Streamer(this)
    }
}
