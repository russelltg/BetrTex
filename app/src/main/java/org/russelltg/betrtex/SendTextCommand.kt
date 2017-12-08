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
            val to: Array<Int>,
            val message: String
    )

    private val receiver: BroadcastReceiver

    init {

        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                val uri = intent?.extras?.getString("uri")


                var c = context?.contentResolver?.query(Uri.parse(uri),
                        arrayOf(Telephony.Sms.Inbox.PERSON, Telephony.Sms.Inbox.THREAD_ID, Telephony.Sms.Inbox.BODY, Telephony.Sms.Inbox.DATE_SENT),
                        null, null, null)


                if (c!!.moveToFirst()) {
                    service.serv?.textReceived(Message(c.getInt(0), c.getInt(1), c.getString(2), c.getLong(3)));
                }

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

        // convert canonical IDS to real numbers

        // TODO: implement this
        assert(msg.to.size == 1)
        val number = GetContactInfo(service).process(Gson().toJsonTree(msg.to[0]))!!.asJsonObject["number"].asString

        // send it
        // TODO: use multipart
        service.manager?.sendTextMessage(number, null, msg.message, sentIntent, null)

        return null
    }

}