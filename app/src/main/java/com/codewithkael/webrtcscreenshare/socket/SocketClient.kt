package com.codewithkael.webrtcscreenshare.socket

import android.util.Log
import com.codewithkael.webrtcscreenshare.utils.DataModel
import com.codewithkael.webrtcscreenshare.utils.DataModelType
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Exception

@Singleton
class SocketClient @Inject constructor(
    private val gson:Gson
){
    private var username:String?=null
    companion object {
//        private var webSocket: WebSocketClient?=null
        private var socket: Socket?=null
    }

    var listener:Listener?=null
    fun init(username:String){
        this.username = username

        initSocket(this.username!!)

    }

    fun initSocket(username: String) {
        val options = IO.Options()
        options.reconnection = true
        options.reconnectionAttempts = Integer.MAX_VALUE
        options.reconnectionDelay = 1000
        options.timeout = 20000
        options.forceNew = true

        try {
            socket = IO.socket("http://97.74.95.127:8001", options)

            socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
                Log.d("socket", "Connected")
                sendMessageToSocket(
                    DataModel(
                        type = DataModelType.SignIn,
                        username = username,
                        null,
                        null
                    )
                )
            })?.on("message", Emitter.Listener { args ->
                val message = args[0] as String
                val model = try {
                    gson.fromJson(message, DataModel::class.java)
                } catch (e: Exception) {
                    null
                }
                Log.d("socket", "onMessage: $model")
                model?.let {
                    listener?.onNewMessageReceived(it)
                }
            })?.on(Socket.EVENT_DISCONNECT, Emitter.Listener {
                Log.d("socket", "Disconnected")
            })?.on(Socket.EVENT_CONNECT_ERROR, Emitter.Listener { args ->
                val error = args[0] as Exception
                Log.d("socket", "Connection error: ${error.message}")
            })

            socket?.connect()
        } catch (e: Exception) {
            Log.d("socket", "Error: ${e.message}")
        }
    }


    fun sendMessageToSocket(message:Any?){
        try {
            socket?.send(gson.toJson(message))
//            webSocket?.send(gson.toJson(message))
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun onDestroy(){
        socket?.close()
//        webSocket?.close()
    }

    interface Listener {
        fun onNewMessageReceived(model:DataModel)
    }
}