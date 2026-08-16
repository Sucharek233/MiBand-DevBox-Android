package com.sucharek.miband_interconnect_test.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.node.DataItem
import com.xiaomi.xms.wearable.node.DataQueryResult
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi

// Only connection works on Band 10
// Don't know why :)

class Status (
    context: Context,
    private val node: Node?
)  {
    private val nodeApi: NodeApi = Wearable.getNodeApi(context)

    private fun getResult(
        dataItem: DataItem,
        dataQueryResult: DataQueryResult
    ): Int {

        return when(dataItem) {
            DataItem.ITEM_CONNECTION ->
                if (dataQueryResult.isConnected) 1 else 0

            DataItem.ITEM_CHARGING ->
                if (dataQueryResult.isCharging) 1 else 0

            DataItem.ITEM_SLEEP ->
                if (dataQueryResult.isSleeping) 1 else 0

            DataItem.ITEM_WEARING ->
                if (dataQueryResult.isWearing) 1 else 0

            DataItem.ITEM_BATTERY ->
                dataQueryResult.battery

            else -> 0
        }
    }

    private fun query(dataItem: DataItem, message: String) {
        nodeApi.query(node!!.id, dataItem)
            ?.addOnSuccessListener { data ->
                println("query $message = ${getResult(dataItem, data)}")
            }?.addOnFailureListener {
                println("query $message failed : message =  ${it.message}")
            }
    }

    fun connection() {
        query(DataItem.ITEM_CONNECTION, "connection")
    }

    fun charging() {
        query(DataItem.ITEM_CHARGING, "charging")
    }

    fun sleep() {
        query(DataItem.ITEM_SLEEP, "sleep")
    }

    fun wearing() {
        query(DataItem.ITEM_WEARING, "wearing")
    }

    fun battery() {
        query(DataItem.ITEM_BATTERY, "battery")
    }
}