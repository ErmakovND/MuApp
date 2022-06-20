package com.ermakov.nd.muapp.service

import android.service.notification.NotificationListenerService
import android.util.Log
import javax.inject.Inject

class NotificationListener @Inject constructor() : NotificationListenerService() {
    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationListener", "Created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Connected")
    }
}