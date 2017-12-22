package org.russelltg.bridge

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Integer.min

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

class GetMessagesCommand(service: ServerService) : Command(service) {

    data class Params (
            @SerializedName("threadid")
            val threadID: Int,
            val from: Int,
            val to: Int
    )

    @SuppressLint("NewApi")
    override fun process(params: JsonElement): JsonElement? {

        val parameters = Gson().fromJson<Params>(params)

        val cr = service.contentResolver

        val cursor = cr.query(Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, parameters.threadID.toString()),
            arrayOf(Telephony.MmsSms._ID, "ct_t"),
                null, null, null)

        if (cursor.moveToFirst()) {

            // clamp the end
            val to = Integer.max(parameters.to, cursor.count)

            // make sure the range is valid
            if (parameters.from > to) {
                return null
            }

            // start at currentID
            var currentID = parameters.from
            cursor.moveToPosition(currentID)

            val messages = mutableListOf<Message>()

            do {
                // https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android
                val messageID = cursor.getInt(0)
                val type = cursor.getString(1)

                if ("application/vnd.wap.multipart.related" == type) {
                    // mms

                    // get read
                    val messageCursor = cr.query(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, messageID.toString()),
                            arrayOf(Telephony.Mms.READ, Telephony.Mms.DATE), null, null, null)

                    if (!messageCursor.moveToFirst()) {
                        currentID++
                        continue
                    }

                    val read = messageCursor.getInt(0) != 0
                    val timestamp = messageCursor.getLong(1)

                    // close the cursor--we're done here
                    messageCursor.close()

                    // get number
                    val person = getPersonPart(messageID, cr)!!

                    // get parts
                    val partCursor = cr.query(Uri.parse("content://mms/part"),
                            arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT), "mid=" + messageID, null, null)

                    if (!partCursor.moveToFirst()) {
                        currentID++
                        continue
                    }

                    do {
                        val partID = partCursor.getInt(0)
                        val contentType = partCursor.getString(1)
                        val data = partCursor.getString(2)

                        val messageData = when (contentType) {
                            "text/plain" -> {
                                val body = if (data != null) getMmsText(partID, cr) else partCursor.getString(3)

                                MmsData(
                                    type = MmsType.TEXT,
                                    data = body)
                            }
                            "image/png", "image/bmp", "image/jpeg", "image/jpg", "image/gif" -> {
                                MmsData(
                                        type = MmsType.IMAGE,
                                        data = "content://mms/part/" + partID)
                            }
                            else -> null
                        }

                        if (messageData != null) {
                            messages.add(Message(
                                    id = messageID,
                                    person = person,
                                    read = read,
                                    threadID = parameters.threadID,
                                    timestamp = timestamp,
                                    data = messageData
                            ))
                        }

                    } while (partCursor.moveToNext())

                    partCursor.close()

                } else {
                    // sms
                    val messageCursor = cr.query(Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageID.toString()),
                            arrayOf(Telephony.Sms.PERSON, Telephony.Sms.ADDRESS, Telephony.Sms.READ, Telephony.Sms.DATE, Telephony.Sms.BODY),
                            null, null, null)


                    if (messageCursor.moveToFirst()) {

                        val num = messageCursor.getString(1)
                        val p = messageCursor.getInt(0)

                        messages.add(Message(
                                id = currentID,
                                person= if (p == 0) Person(0, "") else Person(numberToContact(num, cr)?: -1, num),
                                read = messageCursor.getInt(2) != 0,
                                threadID = parameters.threadID,
                                timestamp = messageCursor.getLong(3),
                                data = SmsData(messageCursor.getString(4))
                        ))
                    }

                    messageCursor.close()
                }

                currentID++
            } while (cursor.moveToNext() && currentID < to)

            cursor.close()

            return Gson().toJsonTree(messages)
        }
        return null
    }
}