package com.example.websocketintegration

import android.app.Application
import android.content.Context

/**
 * @Created_by: Shishir
 * @Created_on: 05,February,2025
 */
class AppManager : Application() {

    init {
        mAppInstance = this
    }

    companion object {
        private var mAppInstance: AppManager? = null
        val appContext: Context? get() = mAppInstance?.applicationContext
    }
}