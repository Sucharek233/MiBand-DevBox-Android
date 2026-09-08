package com.sucharek.miband_interconnect_test.ui.screens.activities.moduleCompatibility

import org.json.JSONObject
import com.sucharek.miband_interconnect_test.models.MessageStates

enum class CompatStatus {
    UNKNOWN,
    CHECKING,
    SUPPORTED,
    UNSUPPORTED
}

data class ModuleItem(
    val name: String,
    val isSelected: Boolean = true,
    val isExpanded: Boolean = false,
    val status: CompatStatus = CompatStatus.UNKNOWN,
    val functionsResult: Any? = null,
    val isLoadingFuncs: Boolean = false
)

sealed class ModuleResponse {
    data class CompatResult(val results: Map<String, Boolean>) : ModuleResponse()
    data class FuncsResult(val moduleName: String, val functions: Any?) : ModuleResponse()
    data class Error(val message: String) : ModuleResponse()

    companion object {
        fun parse(payloadString: String, pendingFuncsModule: String?): ModuleResponse {
            return try {
                val outer = JSONObject(payloadString)
                val state = outer.optString("state", "")

                if (state == MessageStates.ERROR) {
                    return Error(outer.optString("msg", "Unknown error"))
                }

                val resString = outer.optString("res", null)

                if (resString != null) {
                    val trimmed = resString.trim()

                    if (trimmed.startsWith("{")) {
                        val parsedObj = JSONObject(trimmed)
                        val isCompatMap = parsedObj.keys().asSequence().all { key ->
                            parsedObj.opt(key) is Boolean
                        }

                        if (isCompatMap) {
                            val map = mutableMapOf<String, Boolean>()
                            parsedObj.keys().forEach { key ->
                                map[key] = parsedObj.optBoolean(key)
                            }
                            return CompatResult(map)
                        } else if (pendingFuncsModule != null) {
                            return FuncsResult(pendingFuncsModule, parsedObj)
                        }
                    }
                }

                val map = mutableMapOf<String, Boolean>()
                val resObj = outer.optJSONObject("res") ?: outer
                resObj.keys().forEach { key ->
                    if (key != "type" && key != "state" && key != "res") {
                        map[key] = resObj.optBoolean(key)
                    }
                }

                if (map.isNotEmpty()) {
                    CompatResult(map)
                } else if (pendingFuncsModule != null) {
                    FuncsResult(pendingFuncsModule, resString)
                } else {
                    Error("Unrecognized response structure")
                }
            } catch (e: Exception) {
                Error("Failed to parse: ${e.localizedMessage}")
            }
        }
    }
}