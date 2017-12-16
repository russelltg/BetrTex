package org.russelltg.bridge

import android.net.Uri
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement

/** A command to list the conversations
 * @property service The service to initialize the command with
 *
 * params: `{}`
 *    no parameters
 * returns `[Conversation, Conversation]`
 *    example:
 *    ```json
 *    [
 *        {
 *           "threadid": 3,
 *           "messagecount": 40,
 *           "snippet": "Hello mother!",
 *           "people": [
 *              "contactid": 3,
 *              "number": "+11234567890"
 *           ],
 *           read: true
 *        },
 *        ...
 *    ]
 *    ```
 */
class ListConversationsCommand(service: ServerService) : Command(service) {

    // this class is the data for a conversation.
    // used to convert to JSON and send over the WS RPC
    data class Conversation (
            // The thread ID
            // this ID is the _id in the content://mms-sms/conversations table
            val threadid: Int,

            // how many messages are in the conversation
            val messagecount: Int,

            // a small string snippet showing a brief of the conversation
            val snippet: String,

            // the people that are in this conversation (excluding the user)
            val people: Array<Person>, // canonical addresses

            // if all the messages in the conversation have been read
            val read: Boolean
    )

    override fun process(params: JsonElement): JsonElement? {

        val cr = service.contentResolver!!

        // query from all the sms and mms conversations which aren't archived
        val c = cr.query(Uri.parse("content://mms-sms/conversations?simple=true"),
                arrayOf(Telephony.MmsSms._ID, Telephony.Threads.MESSAGE_COUNT, Telephony.Threads.SNIPPET, Telephony.Threads.RECIPIENT_IDS, Telephony.Threads.READ),
                Telephony.Threads.ARCHIVED + "=0", null, null)

        val conversations = mutableListOf<Conversation>()

        if (c!!.moveToFirst()) {

            do {

                val people = c.getString(3).split(' ').map {

                    // get phone number to get contact id
                    val canonCursor = cr.query(Uri.parse("content://mms-sms/canonical-address/" + it),
                            arrayOf(Telephony.CanonicalAddressesColumns.ADDRESS), null, null, null)

                    if (!canonCursor.moveToFirst()) {

                        canonCursor.close()
                        Person(-1, "")
                    } else {

                        val phoneNumber = canonCursor.getString(0)

                        val id = numberToContact(phoneNumber, cr)

                        canonCursor.close()
                        Person(id ?: -1, phoneNumber)
                    }
                }

                conversations.add(Conversation(
                        threadid = c.getInt(0),
                        messagecount = c.getInt(1),
                        people = people.toTypedArray(),
                        read = c.getInt(4) != 0,
                        snippet = if (c.isNull(2)) "" else c.getString(2)))

            } while (c.moveToNext())
        }
        c.close()

        return Gson().toJsonTree(conversations)


    }
}




