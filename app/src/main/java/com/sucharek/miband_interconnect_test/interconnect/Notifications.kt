package com.sucharek.miband_interconnect_test.interconnect

import android.content.Context

import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.notify.NotifyApi

class Notifications(
    context: Context,
    private val node: Node?
) {
    private val notifyApi: NotifyApi = Wearable.getNotifyApi(context)

    fun sendNotification(title: String, message: String) {
        notifyApi.sendNotify(node?.id, title, message)?.addOnSuccessListener {
            println("sendNotify success")
        }?.addOnFailureListener {
            println("sendNotify failed:${it.message}")
        }
    }
}