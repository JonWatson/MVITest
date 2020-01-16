package com.devorion.mvitest

import android.app.Application
import com.devorion.mvitest.storage.GameStorage
import com.devorion.mvitest.storage.GameStorageImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Android context
            androidContext(this@App)
            // modules
            modules(module)
        }
    }

    private val module = module {
        single<GameStorage> { GameStorageImpl(get()) }
    }
}