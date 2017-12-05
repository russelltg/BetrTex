package org.russelltg.betrtex

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.SmsManager
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class ServerService : Service() {

    var serv: WsServer? = null
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

data class TextSentMessage (
        val type: String = "sms.sent",
        val number: String,
        val message: String
)

class WsServer(addr : InetSocketAddress, serv: ServerService) : WebSocketServer(addr) {

    var commands = HashMap<String, ()->Command>()
    var service: ServerService? = null
    var connections = HashMap<String,WebSocket>()

    init {
        service = serv

        // register commands
        commands["send-text"] = {
            SendTextCommand()
        }
    }



    // send new texts
    fun textSent(number: String, message: String) {
        // build TextSentMessage
        val message = TextSentMessage(number=number, message=message)

        // to JSON
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val json = moshi.adapter(TextSentMessage::class.java).toJson(message)

        // send it to all connected devices
        for (conn in connections) {
            conn.value.send(json)
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

    data class Message (
            val type: String
    )

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (message == null) {
            return
        }
        // to JSON
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val parsedMessage = moshi.adapter(Message::class.java).fromJson(message)

        // try to find it in commands
        if (commands.containsKey(parsedMessage?.type)) {
            var cmd = commands[parsedMessage?.type]?.invoke()
            val returnMessage = cmd?.process(service!!, message)

            if (returnMessage != null) {
                conn?.send(returnMessage)
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


