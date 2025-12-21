package com.anand.prohands

import android.app.Application
import com.anand.prohands.data.local.AppDatabase
import com.anand.prohands.utils.SessionManager

class ProHandsApplication : Application() {

    lateinit var sessionManager: SessionManager
        private set

    lateinit var database: AppDatabase
        private set

    companion object {
        lateinit var instance: ProHandsApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)
        database = AppDatabase.getDatabase(this)
    }
}
