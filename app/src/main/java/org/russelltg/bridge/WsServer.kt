package org.russelltg.bridge

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.NotificationCompat
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.set
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import kotlin.system.measureTimeMillis

private const val CONNECTION_ACCEPTED = "BRIDGE_CONNECTION_ACCEPTED"
private const val CONNECTION_REJECTED = "BRIDGE_CONNECTION_REJECT"

class WsServer(addr : InetSocketAddress, val service: ServerService) : WebSocketServer(addr) {

    private val commands = HashMap<String, Command>()
    private val pendingConnections = HashMap<String, WebSocket>()
    private val verifiedConnections = HashMap<String, WebSocket>()
    private val verifyNotifictions = HashMap<String, Int>()

    private var nextNotificationID = 1

    private val acceptReceiver: BroadcastReceiver
    private val rejectReceiver: BroadcastReceiver

    init {

        acceptReceiver = object : BroadcastReceiver() {
        // receivers
            override fun onReceive(context: Context?, intent: Intent?) {
                val remote = intent!!.getStringExtra("remote")

                val ws = pendingConnections[remote] ?: return

                // tell the socket that it's been accepted
                ws.send(Gson().toJson(jsonObject(
                        "jsonrpc" to 2,
                        "method" to "connectionaccepted",
                        "id" to -1
                )))

                verifiedConnections[remote] = ws

                val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(verifyNotifictions[remote]!!)

                pendingConnections.remove(remote)
            }
        }
        service.registerReceiver(acceptReceiver, IntentFilter(CONNECTION_ACCEPTED))

        rejectReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val remote = intent!!.getStringExtra("remote")

                val ws = pendingConnections[remote] ?: return

                // tell the socket that it's been rejected
                ws.send(Gson().toJson(jsonObject(
                        "jsonrpc" to 2,
                        "method" to "connectionrejected",
                        "id" to -1
                )))

                val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(verifyNotifictions[remote]!!)

                pendingConnections.remove(remote)
            }
        }
        service.registerReceiver(rejectReceiver, IntentFilter(CONNECTION_REJECTED))


        // register commands
        commands["send-text"] = SendTextCommand(service)
        commands["list-conversations"] = ListConversationsCommand(service)
        commands["contact-info"] = GetContactInfo(service)
        commands["get-messages"] = GetMessagesCommand(service)
        commands["get-image"] = GetImage(service)
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
        if (conn == null) {
            return
        }

        val connectionID = conn.remoteSocketAddress.hostString

        val acceptedIntent = Intent(CONNECTION_ACCEPTED)
        acceptedIntent.putExtra("remote", connectionID)

        val acceptedPendingIntent = PendingIntent.getBroadcast(service, 0, acceptedIntent, 0)

        val rejectedIntent = Intent(CONNECTION_REJECTED)
        rejectedIntent.putExtra("remote", connectionID)

        val rejectedPendingIntent = PendingIntent.getBroadcast(service, 0, rejectedIntent, 0)

        val notifyBuilder = NotificationCompat.Builder(service, "default")
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(service.resources.getString(R.string.new_connection))
                .setContentText(service.resources.getString(R.string.connection_from, conn.remoteSocketAddress.hostName))
                .addAction(0, service.resources.getString(R.string.accept), acceptedPendingIntent)
                .addAction(0, service.resources.getString(R.string.reject), rejectedPendingIntent)

        val intent = Intent(service, ServerService::class.java)

        val stackBuilder = TaskStackBuilder.create(service)
        stackBuilder.addNextIntent(intent)

        val resultIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        notifyBuilder.setContentIntent(resultIntent)

        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationID = nextNotificationID++
        notificationManager.notify(notificationID, notifyBuilder.build())

        verifyNotifictions[connectionID] = notificationID
        pendingConnections[connectionID] = conn

    }

    override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {

        if (conn != null) {
            val key = conn.remoteSocketAddress.hostString

            // if its closed before being accepted
            if (pendingConnections.containsKey(key)) {
                val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(verifyNotifictions[key]!!)

                pendingConnections.remove(key)
            } else if (verifiedConnections.containsKey(key)) {
                verifiedConnections.remove(key)
            }
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
        ex?.printStackTrace()
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
        if (message == null || conn == null) {
            return
        }

        val jsonData = Gson().fromJson(message, JsonObject::class.java)
        val method = jsonData.get("method").asString
        val id = jsonData.get("id").asInt

        // make sure its verified
        if (!verifiedConnections.containsKey(conn.remoteSocketAddress.hostString)) {
            val obj = JsonObject()
            obj["jsonrpc"] = 2
            obj["result"] = "Error: unverified connection"
            obj["id"] = id

            conn.send(Gson().toJson(obj))
            return
        }

        // try to find it in commands
        if (commands.containsKey(method)) {
            val cmd = commands[method]

            try {

                // time processing
                var returnMessage: JsonElement? = null

                val time = measureTimeMillis {
                    returnMessage = cmd?.process(jsonData.get("params"))
                }

                if (returnMessage != null) {

                    val obj = JsonObject()
                    obj["jsonrpc"] = 2.0
                    obj["result"] = returnMessage
                    obj["id"] = id
                    obj["handledin"] = time

                    conn.send(Gson().toJson(obj))
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }

        } else {
            val obj = JsonObject()
            obj["jsonrpc"] = 2
            obj["result"] = "Unrecognized command: $method"
            obj["id"] = id

            conn.send(Gson().toJson(obj))
        }

    }

    override fun onStart() {
        println("Started!")
    }


    // send a message to all connected clients
    private fun propagateMessage(message: String) {
        try {
            for (conn in verifiedConnections) {
                conn.value.send(message)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

