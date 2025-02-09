package com.example.websocketintegration.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.websocketintegration.AppManager
import com.example.websocketintegration.NetworkConnectivityServiceImpl
import com.example.websocketintegration.NetworkStatus
import com.example.websocketintegration.websocket.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @Created_by: Shishir
 * @Created_on: 04,February,2025
 */

sealed interface MainScreenUiEvent {
    data object ConnectBtnTapped : MainScreenUiEvent
    data object DisConnectBtnTapped : MainScreenUiEvent
    data class SendBtnTapped(val message: String) : MainScreenUiEvent
}

class MainScreenVm : ViewModel() {

    private val socketUrl = "wss://echo.websocket.org"

    // network status
    private val networkConnectivityService = NetworkConnectivityServiceImpl(AppManager.appContext!!)
    val networkStatus: StateFlow<NetworkStatus> = networkConnectivityService.networkStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkStatus.Unknown
    )

    // socket status
    private val _socketStatus = MutableStateFlow(false)
    val socketStatus: StateFlow<Boolean> = _socketStatus.asStateFlow()

    // messages
    private val _messages = MutableStateFlow("")
    val messages: StateFlow<String> = _messages.asStateFlow()

    private val socket = Socket.Builder.withSocketUrl(socketUrl).build()
        .setOnConnectionStatusChangeListener(object : Socket.OnStatusChangeListener {
            override fun onConnectionStatusChange(isConnected: Boolean) {
                _socketStatus.value = isConnected
            }
        })
        .setOnMessageListener(object : Socket.MessageListener {
            override fun onMessageReceived(message: String) {
                _messages.value = message
            }
        })


    fun onUiEvent(event: MainScreenUiEvent) {
        when (event) {
            is MainScreenUiEvent.ConnectBtnTapped -> {
                socket.connect()
            }

            is MainScreenUiEvent.DisConnectBtnTapped -> {
                disConnect()
            }

            is MainScreenUiEvent.SendBtnTapped -> {
                val message: String = event.message
                sendMessage(message)
            }
        }
    }

    private fun sendMessage(message: String) = viewModelScope.launch {
        socket.send(message)
    }

    fun connect() {
        if (networkStatus.value == NetworkStatus.Connected) {
            socket.connect()
        }
    }

    fun disConnect() {
        socket.disConnect(1001, "")
    }

    fun terminate() {
        socket.terminate()
    }

}