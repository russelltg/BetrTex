package org.russelltg.betrtex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class TextReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context?, intent: Intent?) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val orig = messages[0].displayOriginatingAddress
        var message = ""
        for (m in messages) {
            message += m.displayMessageBody
        }
        val timestamp = messages[0].timestampMillis

        // get from db
        val cursor = ctx!!.contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.Inbox.PERSON, Telephony.Sms.Inbox.THREAD_ID, Telephony.Sms.BODY, Telephony.Sms.DATE_SENT),
                "address=? AND body=? AND date_sent=?",
                arrayOf(orig, message, timestamp.toString()), null)


        if (cursor.moveToFirst() && ctx is ServerService) {
            ctx.serv?.textReceived(Message(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getLong(3)))
        }

    }
}
