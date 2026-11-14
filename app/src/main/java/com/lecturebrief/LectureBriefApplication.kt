package com.lecturebrief

import android.app.Application
import com.lecturebrief.di.AppContainer
import com.lecturebrief.di.DefaultAppContainer

class LectureBriefApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
    }
}
