package com.sucharek.devbox.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DeviceManager(context: Context) {
    private val nodeApi: NodeApi = Wearable.getNodeApi(context)

    suspend fun getConnectedDevices(): List<Node> {
        return suspendCancellableCoroutine { continuation ->
            val task = nodeApi.connectedNodes

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to initialize connectedNodes task"))
                return@suspendCancellableCoroutine
            }

            task.addOnSuccessListener { nodes ->
                continuation.resume(nodes ?: emptyList())
            }
            task.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }
}