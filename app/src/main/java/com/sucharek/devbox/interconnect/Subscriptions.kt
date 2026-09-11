package com.sucharek.devbox.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.node.DataItem
import com.xiaomi.xms.wearable.node.DataSubscribeResult
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Band 10 behavior
// Connection works fine
// Charging works fine (RESULT_CHARGING_FINISH untested)
// SLEEP and WEARING get sent when plugging in or out, they return 1 in both states
// SLEEP might work, but I haven't tested it

class Subscriptions(
    context: Context,
    private val node: Node,
    private val onSubscriptionUpdate: (DataItem, Int) -> Unit
) {
    private val nodeApi: NodeApi = Wearable.getNodeApi(context)

    private fun getResultType(
        dataItem: DataItem,
        dataSubscribeResult: DataSubscribeResult
    ): Int {
        return when (dataItem) {
            DataItem.ITEM_CONNECTION -> dataSubscribeResult.connectedStatus
            DataItem.ITEM_CHARGING -> dataSubscribeResult.chargingStatus
            DataItem.ITEM_SLEEP -> dataSubscribeResult.sleepStatus
            DataItem.ITEM_WEARING -> dataSubscribeResult.wearingStatus
            else -> 0
        }
    }

    suspend fun subscribe(dataItem: DataItem) {
        return suspendCancellableCoroutine { continuation ->
            val task = nodeApi.subscribe(node.id, dataItem) { _, _, data ->
                val resultType = getResultType(dataItem, data)
                onSubscriptionUpdate(dataItem, resultType)
            }

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to initialize subscription task for $dataItem"))
                return@suspendCancellableCoroutine
            }

            task.addOnSuccessListener {
                continuation.resume(Unit)
            }
            task.addOnFailureListener {
                continuation.resumeWithException(it)
            }
        }
    }

    suspend fun unsubscribe(dataItem: DataItem) {
        return suspendCancellableCoroutine { continuation ->
            val task = nodeApi.unsubscribe(node.id, dataItem)

            if (task == null) {
                continuation.resumeWithException(IllegalStateException("Failed to initialize unsubscription task for $dataItem"))
                return@suspendCancellableCoroutine
            }

            task.addOnSuccessListener {
                continuation.resume(Unit)
            }
            task.addOnFailureListener {
                continuation.resumeWithException(it)
            }
        }
    }

    // Helper methods for specific items
    suspend fun subConnection() = subscribe(DataItem.ITEM_CONNECTION)
    suspend fun unsubConnection() = unsubscribe(DataItem.ITEM_CONNECTION)

    suspend fun subCharging() = subscribe(DataItem.ITEM_CHARGING)
    suspend fun unsubCharging() = unsubscribe(DataItem.ITEM_CHARGING)

    suspend fun subSleep() = subscribe(DataItem.ITEM_SLEEP)
    suspend fun unsubSleep() = unsubscribe(DataItem.ITEM_SLEEP)

    suspend fun subWearing() = subscribe(DataItem.ITEM_WEARING)
    suspend fun unsubWearing() = unsubscribe(DataItem.ITEM_WEARING)
}
