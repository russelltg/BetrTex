package org.russelltg.bridge

import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class WsServer(addr : InetSocketAddress, serv: ServerService) : WebSocketServer(addr) {

    private var commands = HashMap<String, Command>()
    private var service: ServerService = serv
    private var connections = HashMap<String, WebSocket>()

    init {

        // register commands
        commands["send-text"] = SendTextCommand(service)
        commands["list-conversations"] = ListConversationsCommand(service)
        commands["contact-info"] = GetContactInfo(service)
        commands["get-messages"] = GetMessagesCommand(service)
    }

    // send new texts to the client
    fun textReceived(message: Message) {
        // build TextSentMessage
        val rpcMessage = Gson().toJson(RPCMessage(id = -1, method = "text-received", params = message))

        // send it to all connected devices
        propagateMessage(rpcMessage)
    }


    // WebSocketServer implementation
    override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
        // add it
        if (conn != null) {
            connections[conn.remoteSocketAddress.toString()] = conn
        }
    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
        if (conn != null) {
            connections.remove(conn.remoteSocketAddress.toString())
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
            val cmd = commands[method]

            try {
                val returnMessage = cmd?.process(jsonData.get("params"))


                if (returnMessage != null) {

                    val obj = JsonObject()
                    obj["jsonrpc"] = 2.0
                    obj["result"] = returnMessage
                    obj["id"] = id

                    conn?.send(Gson().toJson(obj))
                }

            } catch (e: Exception) {
                e.printStackTrace()
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


    // send a message to all connected clients
    private fun propagateMessage(message: String) {
        for (conn in connections) {
            conn.value.send(message)
        }
    }

    override fun stop() {
        // close commands
        for (comm in commands.values) {
            comm.close()
        }

        super.stop()
    }
}

