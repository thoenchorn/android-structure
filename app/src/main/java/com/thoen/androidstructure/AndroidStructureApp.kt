package com.thoen.androidstructure

import android.app.Application
import com.thoen.androidstructure.di.AppContainer

class AndroidStructureApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            environment = BuildConfig.FLAVOR,
            baseUrl = BuildConfig.API_BASE_URL,
        )
    }
}
