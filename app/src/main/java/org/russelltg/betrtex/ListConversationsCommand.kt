package org.russelltg.betrtex

import android.net.Uri
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement

class ListConversationsCommand(serv: ServerService): Command(serv) {

    data class Conversation (
            val threadid: Int,
            val messagecount: Int,
            val snippet: String,
            val addresses: Array<Int>, // canonical addresses
            val read: Boolean
    )
    override fun process(params: JsonElement): JsonElement? {

        val cr = service.contentResolver!!

        try {
            var c = service.contentResolver.query(Uri.parse("content://mms-sms/conversations?simple=true"),
                    arrayOf(Telephony.Threads._ID, Telephony.Threads.MESSAGE_COUNT, Telephony.Threads.SNIPPET, Telephony.Threads.RECIPIENT_IDS, Telephony.Threads.READ),
                    Telephony.Threads.ARCHIVED + "=0", null, null)

            var convos = mutableListOf<Conversation>()

            if (c!!.moveToFirst()) {

                do {

                    // just return canonical addresses
                    val canonicalAddresses = c.getString(3).split(' ').map { it.toInt() }

                    convos.add(Conversation(
                            c.getInt(0),
                            c.getInt(1),
                            if (c.isNull(2)) "" else c.getString(2), canonicalAddresses.toTypedArray(),
                            c.getInt(4) != 0))

                } while (c.moveToNext())
            }
            return Gson().toJsonTree(convos)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}




