package org.russelltg.bridge

import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement

class ListConversationsCommand(serv: ServerService): Command(serv) {

    data class Conversation (
            val threadid: Int,
            val messagecount: Int,
            val snippet: String,
            val people: Array<Person>, // canonical addresses
            val read: Boolean
    )
    override fun process(params: JsonElement): JsonElement? {

        val cr = service.contentResolver!!

        var c = cr.query(Uri.parse("content://mms-sms/conversations?simple=true"),
                arrayOf(Telephony.MmsSms._ID, Telephony.Threads.MESSAGE_COUNT, Telephony.Threads.SNIPPET, Telephony.Threads.RECIPIENT_IDS, Telephony.Threads.READ),
                Telephony.Threads.ARCHIVED + "=0", null, null)


        var convos = mutableListOf<Conversation>()

        try {

            if (c!!.moveToFirst()) {

                do {

                    val people = c.getString(3).split(' ').map {

                        // get phone number to get contact id
                        val canonCursor = cr.query(Uri.parse("content://mms-sms/canonical-address/" + it),
                                arrayOf(Telephony.CanonicalAddressesColumns.ADDRESS), null, null, null)

                        if (!canonCursor.moveToFirst()) {
                            Person(-1, "")
                        } else {

                            val phoneNumber = canonCursor.getString(0)

                            val id = numberToContact(phoneNumber, cr)
                            Person(id?: -1, phoneNumber)
                        }
                    }

                    convos.add(Conversation(
                            threadid = c.getInt(0),
                            messagecount = c.getInt(1),
                            people = people.toTypedArray(),
                            read = c.getInt(4) != 0,
                            snippet = if (c.isNull(2)) "" else c.getString(2)))

                } while (c.moveToNext())
            }
            return Gson().toJsonTree(convos)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        c.close()

        return null

    }
}




