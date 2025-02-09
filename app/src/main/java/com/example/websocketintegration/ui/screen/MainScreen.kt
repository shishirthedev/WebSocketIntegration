package com.example.websocketintegration.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.websocketintegration.NetworkStatus

/**
 * @Created_by: Shishir
 * @Created_on: 04,February,2025
 */

@Composable
fun MainRoute(
    modifier: Modifier,
    vm: MainScreenVm,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentLifecycleOwner = rememberUpdatedState(lifecycleOwner)
    DisposableEffect(currentLifecycleOwner) {
        val lifecycleObserver = object : DefaultLifecycleObserver {

            override fun onStart(owner: LifecycleOwner) {
                vm.connect()
            }

            override fun onStop(owner: LifecycleOwner) {
                vm.disConnect()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                vm.terminate()
            }
        }

        currentLifecycleOwner.value.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            currentLifecycleOwner.value.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    val networkStatus = vm.networkStatus.collectAsState()
    LaunchedEffect(networkStatus.value) {
        if (networkStatus.value is NetworkStatus.Connected) {
            vm.connect()
        }
    }

    val messageReceived by vm.messages.collectAsState()
    val socketStatus by vm.socketStatus.collectAsState()
    MainScreen(
        modifier = modifier.fillMaxSize(),
        messageReceived = messageReceived,
        socketStatus = socketStatus,
        onUiEvent = vm::onUiEvent
    )
}

@Composable
fun MainScreen(
    modifier: Modifier,
    messageReceived: String,
    socketStatus: Boolean,
    onUiEvent: (MainScreenUiEvent) -> Unit,
) {
    var messageTyped by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = messageReceived, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        Text(text = if (socketStatus) "Connected" else "Disconnected", fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        TextField(
            value = messageTyped,
            onValueChange = { newValue ->
                messageTyped = newValue
            }
        )

        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            onUiEvent.invoke(MainScreenUiEvent.SendBtnTapped(messageTyped))
            messageTyped = ""
        }) {
            Text("Send")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            onUiEvent.invoke(MainScreenUiEvent.ConnectBtnTapped)
        }) {
            Text("Connect")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            onUiEvent.invoke(MainScreenUiEvent.DisConnectBtnTapped)
        }) {
            Text("Disconnect")
        }
        Spacer(Modifier.height(8.dp))
    }
}
