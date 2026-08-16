package com.sucharek.miband_interconnect_test

import android.util.Log

object TestActions {
    /**
     * A generic test action that logs to the console and UI.
     */
    fun performAction(actionName: String, onLog: (String) -> Any?) {
        val message = "Triggered: $actionName"
        Log.d("TestActions", message)
        onLog(message)
    }
}
