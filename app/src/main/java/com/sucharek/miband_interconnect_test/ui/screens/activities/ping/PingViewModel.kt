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
    val type: String,
    val androidStartTime: Long,
    val watchStartTime: Long? = null,
    val watchEndTime: Long? = null,
    val watchAckTime: Long? = null,
    val androidEndTime: Long? = null,
    val status: PingStatus = PingStatus.PENDING,
    val errorMsg: String? = null
)

class PingViewModel(
    private val watchViewModel: WatchViewModel
) : ViewModel() {

    private val _pings = MutableStateFlow<List<PingResult>>(emptyList())
    val pings: StateFlow<List<PingResult>> = _pings.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asPingingStateFlow()

    private var nextId = 0L

    init {
        viewModelScope.launch {
            watchViewModel.pingMessages.collectLatest { rawJson ->
                handlePingResponse(rawJson)
            }
        }
    }

    private fun StateFlow<Boolean>.asPingingStateFlow(): StateFlow<Boolean> = this

    fun sendPing(type: String) {
        if (_isPinging.value) return

        val id = nextId++
        val newPing = PingResult(
            id = id,
            type = type,
            androidStartTime = System.currentTimeMillis()
        )

        _pings.value = (_pings.value + newPing).takeLast(50)
        _isPinging.value = true
        
        watchViewModel.pingDevice(type)
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
                        watchStartTime = if (json.has("startTime")) json.optLong("startTime") else null,
                        watchEndTime = if (json.has("endTime")) json.optLong("endTime") else null,
                        watchAckTime = if (json.has("ackTime")) json.optLong("ackTime") else null,
                        androidEndTime = System.currentTimeMillis()
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
