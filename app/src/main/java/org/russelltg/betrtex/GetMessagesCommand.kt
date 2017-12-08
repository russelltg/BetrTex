package org.russelltg.betrtex

import android.net.Uri
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement

class GetMessagesCommand(serv: ServerService): Command(serv) {


    override fun process(params: JsonElement): JsonElement? {

        val threadID = params.asInt


        val cr = service.contentResolver
        var cursor = cr.query(Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.PERSON, Telephony.Sms.THREAD_ID, Telephony.Sms.BODY, Telephony.Sms.DATE_SENT),
                Telephony.Sms.THREAD_ID + "=?",
                arrayOf(threadID.toString()),
                Telephony.Sms.DATE_SENT)


        if (cursor.moveToFirst()) {


            var messages = mutableListOf<Message>()

            do {
                messages.add(Message(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), cursor.getLong(3)))
            } while (cursor.moveToNext())

            return Gson().toJsonTree(messages)
        }
        return null
    }
}