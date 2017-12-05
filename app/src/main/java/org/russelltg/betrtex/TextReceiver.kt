package org.russelltg.betrtex

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.provider.Telephony

class TextReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context?, intent: Intent?) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val orig = messages[0].originatingAddress
        var message = ""
        for (m in messages) {
            message += m.messageBody
        }


        // send the message

        // cast to ServerService
        if (ctx is ServerService) {
            ctx.serv?.textReceived(orig, message, messages[0].timestampMillis)
        }
    }
}
