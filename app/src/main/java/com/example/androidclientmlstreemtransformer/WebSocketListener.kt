package com.example.androidclientmlstreemtransformer
import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import  okhttp3.WebSocketListener
import okio.ByteString

class WebSocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.e("wsConnected", "connected")
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
    }

//    override fun onMessage(webSocket: WebSocket, text: String) {
//        super.onMessage(webSocket, text)
//    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("WS_onFailure", "OnFailure: " + response + ", " + t);
        super.onFailure(webSocket, t, response)
    }

}