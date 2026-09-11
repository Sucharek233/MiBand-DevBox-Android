package com.sucharek.devbox.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.service.ServiceApi
import com.xiaomi.xms.wearable.service.OnServiceConnectionListener

class Service(
    context: Context,
)  {
    private val serviceApi: ServiceApi = Wearable.getServiceApi(context)
    lateinit var listener: OnServiceConnectionListener

    fun apiLevel() {
        serviceApi.getServiceApiLevel()
            .addOnSuccessListener { level ->
                println("Service API level: $level")
            }
            .addOnFailureListener {
                println("Failed to get service API level: ${it.message}")
            }
    }

    fun listen() {
        listener = object : OnServiceConnectionListener {
            override fun onServiceConnected() {
                println("Service connected")
            }
            override fun onServiceDisconnected() {
                println("Service disconnected")
            }
        }

        serviceApi.registerServiceConnectionListener(listener)
    }

    fun stopListening() {
        serviceApi.unregisterServiceConnectionListener(listener)
    }
}