package com.sucharek.miband_interconnect_test.interconnect

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

    fun checkPermissions() {
        val permissions = arrayOf<Permission>(Permission.DEVICE_MANAGER,Permission.NOTIFY)
        authApi?.checkPermissions(node!!.id,permissions)?.addOnSuccessListener {
            val isPermissionGranted = mutableListOf<String>()
            for((index,permission) in permissions.withIndex()){
                isPermissionGranted.add("${permission.name} grant status is ${it[index]}")
            }
            println("check permissions result is $isPermissionGranted")
        }?.addOnFailureListener {
            println("check permissions failed:${it.message}")
        }
    }

    fun requestPermissions() {
        authApi?.requestPermission(node!!.id, Permission.DEVICE_MANAGER, Permission.NOTIFY)
            ?.addOnSuccessListener { permissions ->
                val permissionGrantedList = mutableListOf<String>()
                for(permission in permissions){
                    permissionGrantedList.add(permission.name)
                }
                println("granted permission is $permissionGrantedList")
            }?.addOnFailureListener {
                println("request permission failed:${it.message}")
            }
    }
}