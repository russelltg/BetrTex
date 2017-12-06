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


class SendTextCommand(service: ServerService): Command(service) {

    val SENT_ACTION = "SMS_SENT_ACTION"

    data class SendTextParams (
            val to: String,
            val message: String
    )

    private val receiver: BroadcastReceiver

    init {

        receiver = object: BroadcastReceiver() {
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
        }

        // register callbacks
        service.registerReceiver(receiver, IntentFilter(SENT_ACTION))

    }

    override fun close() {
        service.unregisterReceiver(receiver)
    }

    override fun process(params: JsonElement): JsonElement? {

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