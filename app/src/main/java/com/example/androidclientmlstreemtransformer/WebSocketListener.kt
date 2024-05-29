package com.example.androidclientmlstreemtransformer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okhttp3.Response
import okhttp3.WebSocket
import  okhttp3.WebSocketListener
import okio.ByteString

class WebSocketListener : WebSocketListener() {
    private val _liveDataText = MutableLiveData<String>()
    val liveDataText: LiveData<String> = _liveDataText

    internal var  _liveDataByteString : ByteString? = null
//    val liveDataByteString: LiveData<ByteString> = _liveDataByteString

    // Overridden methods


    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.e("wsConnected", "connected")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        super.onMessage(webSocket, text)
//        Log.e("onText", text)
        onReceiveText(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
//        Log.e("onBytes", java.lang.StringBuilder().append(bytes).toString())
        _liveDataByteString = bytes
    }

    private fun onReceiveText(string : String){
        _liveDataText.postValue(string)
    }
//    private fun onReceiveBytes(bytes : ByteString  ){
//        _liveDataByteString.postValue(bytes)
//    }

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