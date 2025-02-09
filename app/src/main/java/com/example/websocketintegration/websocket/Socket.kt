package com.example.websocketintegration.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @Created_by: Shishir
 * @Created_on: 04,February,2025
 */

/**
 * 1. Close websocket onDestroy()
 * 2. Implement reConnection logic on onFailure()
 * 3. Listen for connectivity change, and handle connection
 * 4. Split large messages into smaller chunks before sending because Sending large
 *    JSON payloads in a single WebSocket message, causing failures.
 * 5. Send periodic ping to keep alive the connection
 * 6. In production, always use wss: (websocket secure)
 * 7. Use background thread for running connection
 * 8. Move message processing logic to a background thread using CoroutineScope
 * */

class Socket private constructor(private val request: Request) {

    private var webSocket: WebSocket? = null
    private var statusChangeListener: OnStatusChangeListener? = null
    private var messageListener: MessageListener? = null
    private val maxReconnectAttempts = 3
    private var reconnectAttempts = 0

    private val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(httpLoggingInterceptor)
        .dispatcher(Dispatcher(Executors.newSingleThreadExecutor()))
        .pingInterval(10, TimeUnit.SECONDS) // Send periodic ping to keep alive the connection
        .build()


    class Builder private constructor(
        private val builder: Request.Builder,
    ) {
        companion object {
            fun withSocketUrl(url: String): Builder {
                require(url.startsWith("ws:") || url.startsWith("wss:")) {
                    "web socket url must start with ws: or wss"
                }
                return Builder(Request.Builder().url(url))
            }
        }

        fun addHeader(name: String, value: String): Builder {
            return apply { builder.addHeader(name, value) }
        }

        fun build(): Socket = Socket(builder.build())
    }

    fun connect(): Socket {
        if (webSocket == null || webSocket?.send("ping") == false) {
            webSocket = okHttpClient.newWebSocket(
                request = request,
                listener = webSocketListener
            )
        }
        return this
    }

    /**
     * 1000 = websocket connection closed gracefully
     * 1001 = when app goes to background and my reconnect later
     * */
    fun disConnect(): Socket {
        webSocket?.let {
            // Code 1000 ensures that the WebSocket connection closes gracefully
            webSocket?.close(1000, "Do not need connection anymore.")
            webSocket = null
        }
        return this
    }

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    fun scheduleReconnect() {
        if (reconnectAttempts < maxReconnectAttempts) {
            reconnectAttempts++
            Log.d("WebSocket", "Reconnection attempt #$reconnectAttempts")

            Handler(Looper.getMainLooper()).postDelayed({
                connect() // Try to reconnect after the delay(3 seconds)
            }, 3000)
        } else {
            Log.e("WebSocket", "Max reconnect attempts reached. Giving up.")
        }
    }

    fun disConnect(code: Int, reason: String): Socket {
        webSocket?.let {
            webSocket?.close(code, reason)
            webSocket = null
        }
        return this
    }

    fun terminate() {
        clearListeners()
        webSocket?.cancel()
        okHttpClient.dispatcher.executorService.shutdown()
        webSocket = null
    }

    /**
     * Split large messages into smaller chunks before sending because*/
    fun send(message: String) {
        try {
            val chunkSize = 16000
            val parts = message.chunked(chunkSize)
            for (part in parts) {
                if (webSocket?.send(message) == false) {
                    Log.d("Socket", "Failed to send message $part")
                }
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Exception while sending message: ${e.message}")
            e.printStackTrace()
        }
    }


    fun setOnMessageListener(messageListener: MessageListener): Socket {
        this.messageListener = messageListener
        return this
    }

    fun setOnConnectionStatusChangeListener(statusChangeListener: OnStatusChangeListener): Socket {
        this.statusChangeListener = statusChangeListener
        return this
    }

    private fun clearListeners() {
        statusChangeListener = null
        messageListener = null
    }

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "onOpen() called")
            reconnectAttempts = 0
            statusChangeListener?.onConnectionStatusChange(isConnected = true)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "onMessage() called")
            messageListener?.onMessageReceived(text)
        }

        /**
         * This method is called when the WebSocket closes gracefully (either by the client or the server) */
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "onClosed() called")
            statusChangeListener?.onConnectionStatusChange(isConnected = false)
            if (code != 1000 && reconnectAttempts < maxReconnectAttempts) {
                scheduleReconnect()
            }
        }

        /**
         *  This method is called when an unexpected error occurs with the WebSocket connection */
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.d("WebSocket", "onFailure() called")
            statusChangeListener?.onConnectionStatusChange(isConnected = false)
            if (reconnectAttempts < maxReconnectAttempts) {
                scheduleReconnect()
            }
        }
    }

    interface OnStatusChangeListener {
        fun onConnectionStatusChange(isConnected: Boolean)
    }

    interface MessageListener {
        fun onMessageReceived(message: String)
    }

}