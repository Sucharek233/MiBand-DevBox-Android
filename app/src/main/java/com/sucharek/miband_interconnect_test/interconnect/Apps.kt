package com.sucharek.miband_interconnect_test.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Apps(
    context: Context,
    private val node: Node?
) {
    private val nodeApi: NodeApi = Wearable.getNodeApi(context)

    suspend fun isAppInstalled(): Boolean {
        val nodeId = node?.id ?: throw IllegalStateException("Node is not available")

        return suspendCancellableCoroutine { continuation ->
            val task = nodeApi.isWearAppInstalled(nodeId)

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to init app installation"))
                return@suspendCancellableCoroutine
            }

            task.addOnSuccessListener { result ->
                continuation.resume(result)
            }
            task.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    suspend fun launchApp() {
        val nodeId = node?.id ?: throw IllegalStateException("Node is not available")

        return suspendCancellableCoroutine { continuation ->
            val task = nodeApi.launchWearApp(nodeId, "/home")

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to init app launch"))
                return@suspendCancellableCoroutine
            }

            task.addOnSuccessListener {
                continuation.resume(Unit)
            }
            task.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }
}