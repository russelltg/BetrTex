package org.russelltg.betrtex

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.SmsManager
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class ServerService : Service() {

    var serv: WsServer?= null
    var manager: SmsManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // get the sms manager
        manager = SmsManager.getDefault()

        // start the text receiver
        registerReceiver(TextReceiver(), IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

        // start ws server
        serv = WsServer(InetSocketAddress("0.0.0.0", 9834), this)

        serv?.start()

        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}


class WsServer(addr : InetSocketAddress, serv: ServerService) : WebSocketServer(addr) {

    var commands = HashMap<String, Command>()
    var service: ServerService
    var connections = HashMap<String,WebSocket>()

    init {
        service = serv

        // register commands
        commands["send-text"] = SendTextCommand(service)

        commands["list-conversations"] = ListConversationsCommand(service)
    }


    data class TextReceivedMessage (
        val number: String,
        val message: String,
        val timestamp: Long
    )

    // send new texts to the client
    fun textReceived(number: String, message: String, timestamp: Long) {
        // build TextSentMessage
        val message = BuildRPCCall("text-received", -1, TextReceivedMessage(number, message, timestamp))

        // send it to all connected devices
        propagateMessage(message)
    }

    fun sentTextSent(number: String, message: String, timestamp: Long) {
        val message = BuildRPCCall("sent-text-sent", -1, TextReceivedMessage(number, message, timestamp))

        propagateMessage(message)
    }

    // send a message to all connected clients
    fun propagateMessage(message: String) {
        for (conn in connections) {
            conn.value.send(message)
        }
    }

    // WebSocketServer implementation
    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        // add it
        if (conn != null) {
            connections[conn?.remoteSocketAddress.toString()] = conn!!
        }
    }

    override fun onCloseInitiated(conn: WebSocket?, code: Int, reason: String?) {
        if (conn != null) {
            connections.remove(conn?.remoteSocketAddress.toString())
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if (conn != null) {
            connections.remove(conn?.remoteSocketAddress.toString());
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {

    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (message == null) {
            return
        }


        val jsonData = Gson().fromJson(message, JsonObject::class.java)
        val method = jsonData.get("method").asString
        val id = jsonData.get("id").asInt


        // try to find it in commands
        if (commands.containsKey(method)) {
            var cmd = commands[method]
            val returnMessage = cmd?.process(jsonData.get("params").asJsonObject)

            if (returnMessage != null) {

                var obj = JsonObject()
                obj["jsonrpc"] = 2.0
                obj["result"] = returnMessage
                obj["id"] = id

                conn?.send(Gson().toJson(returnMessage))
            }

        } else {
            conn?.send("""
                {
                    "type": "error",
                    "message": "Unrecognized command {parsedMessage?.type}
                }
            """.trimIndent())
        }

    }

    override fun onStart() {
        println("Started!")
    }

}


