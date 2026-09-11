package com.sucharek.devbox.interconnect

import android.content.Context
import com.xiaomi.xms.wearable.Wearable
import com.xiaomi.xms.wearable.auth.AuthApi
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.auth.Permission

class Permissions(
    context: Context,
    private val node: Node?
)  {
    private val authApi: AuthApi = Wearable.getAuthApi(context)

    fun checkPermissions(onResult: (List<String>) -> Unit = {}) {
        val currentNode = node ?: return
        val permissions = arrayOf<Permission>(Permission.DEVICE_MANAGER, Permission.NOTIFY)
        authApi.checkPermissions(currentNode.id, permissions)?.addOnSuccessListener {
            val isPermissionGranted = mutableListOf<String>()
            for((index, permission) in permissions.withIndex()){
                isPermissionGranted.add("${permission.name} grant status is ${it[index]}")
            }
            println("check permissions result is $isPermissionGranted")
            onResult(isPermissionGranted)
        }?.addOnFailureListener {
            println("check permissions failed:${it.message}")
        }
    }

    fun requestPermissions(onSuccess: (List<String>) -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val currentNode = node ?: return
        authApi.requestPermission(currentNode.id, Permission.DEVICE_MANAGER, Permission.NOTIFY)
            ?.addOnSuccessListener { permissions ->
                val permissionGrantedList = mutableListOf<String>()
                for(permission in permissions){
                    permissionGrantedList.add(permission.name)
                }
                println("granted permission is $permissionGrantedList")
                onSuccess(permissionGrantedList)
            }?.addOnFailureListener {
                println("request permission failed:${it.message}")
                onFailure(it)
            }
    }
}
