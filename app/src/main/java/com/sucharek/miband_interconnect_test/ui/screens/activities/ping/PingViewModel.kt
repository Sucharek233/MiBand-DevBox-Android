package com.sucharek.miband_interconnect_test.ui.screens.activities.ping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharek.miband_interconnect_test.ui.screens.maindashboard.WatchViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class PingStatus {
    PENDING, SUCCESS, TIMEOUT, ERROR
}

data class PingResult(
    val id: Long,
    val androidStartTime: Long,
    val qjsAckTime: Long? = null,
    val luaAckTime: Long? = null,
    val receivedTime: Long? = null,
    val androidTotalTime: Long? = null,
    val status: PingStatus = PingStatus.PENDING,
    val errorMsg: String? = null
)

class PingViewModel(
    private val watchViewModel: WatchViewModel
) : ViewModel() {

    private val _pings = MutableStateFlow<List<PingResult>>(emptyList())
    val pings: StateFlow<List<PingResult>> = _pings.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private var nextId = 0L

    init {
        viewModelScope.launch {
            watchViewModel.pingMessages.collectLatest { rawJson ->
                handlePingResponse(rawJson)
            }
        }
    }

    fun sendPing() {
        if (_isPinging.value) return

        val id = nextId++
        val newPing = PingResult(
            id = id,
            androidStartTime = System.currentTimeMillis()
        )

        _pings.value = (_pings.value + newPing).takeLast(50)
        _isPinging.value = true
        
        watchViewModel.pingDevice()
    }

    private fun handlePingResponse(rawJson: String) {
        try {
            val json = JSONObject(rawJson)
            val state = json.optString("state")
            
            val currentPings = _pings.value.toMutableList()
            val pendingIndex = currentPings.indexOfLast { it.status == PingStatus.PENDING }
            
            if (pendingIndex != -1) {
                val pending = currentPings[pendingIndex]
                val updated = when (state) {
                    "done" -> pending.copy(
                        status = PingStatus.SUCCESS,
                        qjsAckTime = if (json.has("startTime")) json.optLong("startTime") else null,
                        luaAckTime = json.optLong("ackTime"),
                        receivedTime = json.optLong("totalTime"),
                        androidTotalTime = System.currentTimeMillis() - pending.androidStartTime
                    )
                    "timeout" -> pending.copy(status = PingStatus.TIMEOUT)
                    else -> pending.copy(status = PingStatus.ERROR, errorMsg = json.optString("msg"))
                }
                currentPings[pendingIndex] = updated
                _pings.value = currentPings
            }
            
            _isPinging.value = false
        } catch (e: Exception) {
            _isPinging.value = false
        }
    }
}
