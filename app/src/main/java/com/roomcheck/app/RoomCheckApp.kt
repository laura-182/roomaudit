package com.roomcheck.app

import android.app.Application
import com.roomcheck.app.data.AppDatabase
import com.roomcheck.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RoomCheckApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = Repository(AppDatabase.create(this), this)
        appScope.launch { repository.seedIfFirstLaunch() }
    }
}
