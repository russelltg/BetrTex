package org.russelltg.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class TextReceiver(private val service: ServerService) : BroadcastReceiver() {

    override fun onReceive(ctx: Context?, intent: Intent?) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val orig = messages[0].displayOriginatingAddress
        var message = ""
        for (m in messages) {
            message += m.displayMessageBody
        }
        val timestamp = messages[0].timestampMillis

        val cr = ctx!!.contentResolver

        // get from db
        val cursor = cr.query(Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.PERSON, Telephony.Sms.ADDRESS, Telephony.Sms.Inbox.THREAD_ID, Telephony.Sms.DATE_SENT, Telephony.Sms.READ, Telephony.Sms.BODY),
                "address=? AND body=? AND date_sent=?",
                arrayOf(orig, message, timestamp.toString()), null)


        service.server?.textReceived(Message(
                person = Person(cursor.getLong(0), cursor.getString(1)),
                threadid = cursor.getInt(2),
                timestamp = cursor.getLong(3),
                read = cursor.getInt(4) != 0,
                data = SmsData(cursor.getString(5))))

        cursor.close()

    }
}
