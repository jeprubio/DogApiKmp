package com.telefonica.androidApp

import android.app.Application
import io.github.aakira.napier.Napier
import io.github.aakira.napier.DebugAntilog

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
    }
}

