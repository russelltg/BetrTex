package org.russelltg.betrtex

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

abstract class Command {

    var service: ServerService

    constructor(service: ServerService) {
        this.service = service
    }

    // process a command
    abstract fun process(message: JsonObject) : JsonElement?
}

class SendTextCommand(service: ServerService): Command(service) {

    val SENT_ACTION = "SMS_SENT_ACTION"
    val DELIVERED_ACTION = "SMS_DELIVRED_ACTION"

    data class SendTextParams (
            val to: String,
            val message: String
    )

    init {

        // register callbacks
        service.registerReceiver(object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                val uri = intent?.extras?.getString("uri")


                var c = context?.contentResolver?.query(Uri.parse(uri), arrayOf(Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE, Telephony.Sms.Inbox.ADDRESS), null, null, null)

                var message = ""
                var timestamp = -1L
                var number = ""

                if (c!!.moveToFirst()) {
                    message = c.getString(0)
                    timestamp = c.getLong(1)
                    number = c.getString(2)
                }

                service.serv?.sentTextSent(number, message, timestamp)
            }
        }, IntentFilter(SENT_ACTION))

    }

    override fun process(params: JsonObject): JsonElement? {

        // from json
        val msg = Gson().fromJson(params, SendTextParams::class.java)

        // build intents
        val sentIntent = PendingIntent.getBroadcast(service, 0, Intent(SENT_ACTION), 0)

        // send it
        // TODO: use multipart
        service.manager?.sendTextMessage(msg!!.to, null, msg.message, sentIntent, null)

        return null
    }

}

class ListConversationsCommand(serv: ServerService): Command(serv) {

    data class Conversation (
            val threadid: Int,
            val address: String,
            val person: String
    )

    override fun process(message: JsonObject): JsonElement? {

        var c = service.contentResolver?.query(Telephony.Sms.Conversations.CONTENT_URI,
                arrayOf(Telephony.Sms.Conversations.THREAD_ID, Telephony.Sms.Conversations.ADDRESS, Telephony.Sms.Conversations.PERSON),
                null, null, null)

        var convos = mutableListOf<Conversation>()

        try {
            if (c!!.moveToFirst()) {
                do {
                    convos.add(Conversation(c!!.getInt(0), c!!.getString(1), c!!.getString(2)))

                } while (c.moveToNext())
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }

        return Gson().toJsonTree(convos)
    }
}




