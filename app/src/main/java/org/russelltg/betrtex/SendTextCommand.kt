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
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction


class SendTextCommand(service: ServerService): Command(service) {

    val SENT_ACTION = "SMS_SENT_ACTION"

    data class SendTextParams (
            val thread: Long,
            val numbers: Array<String>,
            val message: String
    )

    private val receiver: BroadcastReceiver

    init {

        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {

                val uri = intent?.extras?.getString("uri")

                val cr = context?.contentResolver!!

                var c = cr.query(Uri.parse(uri),
                        arrayOf(Telephony.Sms.Inbox.PERSON, Telephony.Sms.ADDRESS, Telephony.Sms.Inbox.THREAD_ID, Telephony.Sms.Inbox.DATE_SENT, Telephony.Sms.Inbox.READ, Telephony.Sms.Inbox.BODY),
                        null, null, null)


                if (c!!.moveToFirst()) {
                    service.serv?.textReceived(Message(
                            person = Person(c.getLong(0), c.getString(1)),
                            threadid = c.getInt(2),
                            timestamp = c.getLong(3),
                            read = c.getInt(4) != 0,
                            data = SmsData(c.getString(5))))
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

        val s = Settings()

        s.useSystemSending = true

        val transaction = Transaction(service, s)
        val message = com.klinker.android.send_message.Message(msg.message, msg.numbers)

//        transaction.setExplicitBroadcastForSentMms(Intent(SENT_ACTION))
        transaction.setExplicitBroadcastForSentSms(Intent(SENT_ACTION))
        
        transaction.sendNewMessage(message, msg.thread)



        return null
    }

}