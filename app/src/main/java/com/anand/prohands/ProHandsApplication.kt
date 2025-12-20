package com.anand.prohands

import android.app.Application
import com.anand.prohands.utils.SessionManager

class ProHandsApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    companion object {
        lateinit var instance: ProHandsApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)
    }
}
