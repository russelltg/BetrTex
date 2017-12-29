package org.russelltg.bridge

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Get the person sending an MMS part
 *
 * @param partID The MMS part ID
 * @param cr The content resolver to fetch the data with
 *
 * @return The
 */
private fun getPersonPart(partID: Int, cr: ContentResolver): Person? {

    // construct uri
    val uriAddress = Uri.withAppendedPath(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, partID.toString()), "addr")

    val cursorAddress = cr.query(uriAddress, arrayOf(Telephony.Mms.Addr.CONTACT_ID, Telephony.Mms.Addr.ADDRESS),
            "msg_id=" + partID, null, null)

    // if the entry was found
    if (cursorAddress.moveToFirst()) {

        // cache a person so we can close the cursor
        val ret = Person(
                contactID = cursorAddress.getLong(0),
                number = cursorAddress.getString(1))

        // close the cursor
        cursorAddress.close()

        return ret
    }

    // if moveToFirst failed
    return null
}


/**
 * Extract MMS text data from a part
 *
 * @param partID The part ID in `content://mms/part`
 * @param cr The content resolver to get the part data with
 */
private fun getMmsText(partID: Int, cr: ContentResolver): String {
    val partURI = Uri.parse("content://mms/part/" + partID)
    val sb = StringBuilder()

    val stream = cr.openInputStream(partURI)
    if (stream != null) {
        val isr = InputStreamReader(stream, "UTF-8")
        val reader = BufferedReader(isr)
        var temp = reader.readLine()
        while (temp != null) {
            sb.append(temp)
            temp = reader.readLine()
        }
    }

    stream?.close()

    return sb.toString()
}


fun messagesFromMms(cr: ContentResolver, messageID: Int): Array<Message> {

    val messages = mutableListOf<Message>()

    // get read
    val messageCursor = cr.query(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, messageID.toString()),
            arrayOf(Telephony.Mms.READ, Telephony.Mms.DATE, Telephony.Mms.THREAD_ID), null, null, null)

    if (!messageCursor.moveToFirst()) {
        return arrayOf()
    }

    val read = messageCursor.getInt(0) != 0
    val timestamp = messageCursor.getLong(1)
    val threadID = messageCursor.getInt(2)

    // close the cursor--we're done here
    messageCursor.close()

    // get number
    val person = getPersonPart(messageID, cr)!!

    // get parts
    val partCursor = cr.query(Uri.parse("content://mms/part"),
            arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT), "mid=" + messageID, null, null)

    if (!partCursor.moveToFirst()) {
        return arrayOf()
    }

    do {
        val partID = partCursor.getInt(0)
        val contentType = partCursor.getString(1)
        val data = partCursor.getString(2)

        val messageData = when (contentType) {
            "text/plain" -> {
                val body = if (data != null) getMmsText(partID, cr) else partCursor.getString(3)

                MessageData.Text(
                        message = body)
            }
            "image/png", "image/bmp", "image/jpeg", "image/jpg", "image/gif" -> {
                MessageData.Image (
                        image = ImageLocation(
                            uri = "content://mms/part/" + partID,
                            width = 0, // TODO:
                            height = 0)
                )
            }
            else -> null
        }

        if (messageData != null) {
            messages.add(Message(
                    person = person,
                    read = read,
                    threadID = threadID,
                    timestamp = timestamp,
                    data = messageData
            ))
        }

    } while (partCursor.moveToNext())

    partCursor.close()

    return messages.toTypedArray()
}
