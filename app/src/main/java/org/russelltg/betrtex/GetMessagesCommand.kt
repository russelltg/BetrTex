package org.russelltg.betrtex

import android.net.Uri
import android.provider.Telephony
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement

class GetMessagesCommand(serv: ServerService): Command(serv) {


    override fun process(params: JsonElement): JsonElement? {

        val cr = service.contentResolver


        // parse params
        // convert these phone numbers to canonical addresses
        var numbers = Gson().fromJson(params, listOf<String>().javaClass).map {
            val cleanNum = cleanNumber(it)

            var cursor = cr.query(Uri.parse("content://mms-sms/canonical-addresses"),
                    arrayOf(Telephony.CanonicalAddressesColumns._ID, Telephony.CanonicalAddressesColumns.ADDRESS), null, null, null)

            var id = -1

            if (cursor.moveToFirst()) {
                do {

                    if (cleanNum == cleanNumber(cursor.getString(1))) {
                        id = cursor.getInt(0)
                    }

                } while (cursor.moveToNext())
            }

            id
        }

        // sort the list
        numbers = numbers.sorted()

        // convert into string
        val numberString = numbers.fold("",  { running, next -> running + " " + next})


        var cursor = cr.query(Uri.parse("content://mms-sms/conversations?simple=true"),
            arrayOf(Telephony.Threads.),
                Telephony.Threads.RECIPIENT_IDS + " = " + numberString, null, null)


        if (cursor.moveToFirst()) {

            var messages = mutableListOf<Message>()

            do {

                messages.add()

            } while (cursor.moveToNext())

        }
    }
}