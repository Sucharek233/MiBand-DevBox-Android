package com.sucharek.miband_interconnect_test.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.message.MessageApi
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import com.xiaomi.xms.wearable.node.Node
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Messages(
    context: Context,
    private val node: Node, // Make this non-null since we require a node to exist anyway
    private val onMessageReceived: (ByteArray) -> Unit // Fed directly from our ViewModel
) {
    private val messageApi: MessageApi = Wearable.getMessageApi(context)

    private val messageListener = OnMessageReceivedListener { _, bytes ->
        val received = bytes
        onMessageReceived(received)
    }

    suspend fun sendMessage(message: String) {
        return suspendCancellableCoroutine { continuation ->
            val task = messageApi.sendMessage(node.id, message.toByteArray())

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to initialize send task"))
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

    suspend fun sendRawMessage(message: ByteArray) {
        return suspendCancellableCoroutine { continuation ->
            val task = messageApi.sendMessage(node.id, message)

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to initialize raw send task"))
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

    suspend fun addIncomingMessageListener() {
        return suspendCancellableCoroutine { continuation ->
            val task = messageApi.addListener(node.id, messageListener)

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to register listener task"))
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

    suspend fun removeIncomingMessageListener() {
        return suspendCancellableCoroutine { continuation ->
            val task = messageApi.removeListener(node.id)

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to remove listener task"))
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