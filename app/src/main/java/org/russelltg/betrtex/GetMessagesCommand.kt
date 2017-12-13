package org.russelltg.betrtex

import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement
import android.util.Base64
import java.io.*


private fun getMmsText(id: Int, cr: ContentResolver): String {
    val partURI = Uri.parse("content://mms/part/" + id)
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

private fun getPersonPart(id: Int, cr: ContentResolver): Person? {

    // construct uri
    val uriAddress = Uri.withAppendedPath(Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, id.toString()), "addr")

    val cAdd = cr.query(uriAddress, arrayOf(Telephony.Mms.Addr.CONTACT_ID, Telephony.Mms.Addr.ADDRESS),
            "msg_id=" + id, null, null)


    var ret: Person? = null

    if (cAdd!!.moveToFirst()) {
        do {
            ret = Person(cAdd.getLong(0), cAdd.getString(1))
        } while (cAdd.moveToNext())
    }
    cAdd.close()

    return ret
}

private fun b64EncodePart(partid: Int, cr: ContentResolver): String {
    val partURI = Uri.parse("content://mms/part/" + partid)
    val inStream = cr.openInputStream(partURI)

    // base64 encode it
    return Base64.encodeToString(inStream.readBytes(), 0)
}

class GetMessagesCommand(serv: ServerService): Command(serv) {


    override fun process(params: JsonElement): JsonElement? {

        val threadID = params.asInt


        val cr = service.contentResolver
        var cursor = cr.query(Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, threadID.toString()),
            arrayOf(Telephony.MmsSms._ID, "ct_t"),
                null, null, null)


        if (cursor.moveToFirst()) {

            // limit to last 100
            if (cursor.count > 100) {
                cursor.moveToPosition(cursor.count - 100)
            }

            var messages = mutableListOf<Message>()

            do {


                // https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android
                val messageID = cursor.getInt(0)
                val type = cursor.getString(1)

                if ("application/vnd.wap.multipart.related" == type) {
                    // mms

                    // get read
                    val messageCursor = cr.query(Telephony.Mms.CONTENT_URI,
                            arrayOf(Telephony.Mms.READ, Telephony.Mms.DATE), Telephony.Mms._ID + "=" + messageID, null, null)

                    if (!messageCursor.moveToFirst()) {
                        continue
                    }

                    val read = messageCursor.getInt(0) != 0
                    val timestamp = messageCursor.getLong(1)


                    // get number
                    val person = getPersonPart(messageID, cr)!!

                    // get parts
                    val partCursor = cr.query(Uri.parse("content://mms/part"),
                            arrayOf(Telephony.Mms.Part._ID, Telephony.Mms.Part.CONTENT_TYPE, Telephony.Mms.Part._DATA, Telephony.Mms.Part.TEXT), "mid=" + messageID, null, null)

                    if (!partCursor.moveToFirst()) {
                        continue
                    }

                    do {
                        val partid = partCursor.getInt(0)
                        val contentType = partCursor.getString(1)
                        val data = partCursor.getString(2)

                        val messageData = when (contentType) {
                            "text/plain" -> {
                                val body = if (data != null) getMmsText(partid, cr) else partCursor.getString(3)

                                MmsData(
                                    type = MmsType.TEXT,
                                    data = body)
                            }
                            "image/png" -> {
                                MmsData(
                                        type = MmsType.IMAGE,
                                        data = "data:image/png;base64, " + b64EncodePart(partid, cr))

                            }
                            "image/bmp" -> {
                                MmsData(
                                        type = MmsType.IMAGE,
                                        data = "data:image/bmp;base64, " + b64EncodePart(partid, cr))

                            }
                            "image/jpeg", "image/jpg" -> {
                                MmsData(
                                    type = MmsType.IMAGE,
                                    data = "data:image/jpeg;base64, " + b64EncodePart(partid, cr))
                            }
                            "image/gif" -> {
                                MmsData(
                                    type = MmsType.IMAGE,
                                    data = "data:image/gif;base64, " + b64EncodePart(partid, cr))
                            }
                            else -> null
                        }

                        if (messageData != null) {
                            messages.add(Message(
                                    person = person,
                                    read = read,
                                    threadid = threadID,
                                    timestamp = timestamp,
                                    data = messageData
                            ))
                        }

                    } while (partCursor.moveToNext())

                } else {
                    // sms
                    val messageCursor = cr.query(Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageID.toString()),
                            arrayOf(Telephony.Sms.PERSON, Telephony.Sms.ADDRESS, Telephony.Sms.READ, Telephony.Sms.DATE, Telephony.Sms.BODY), null, null, null)


                    if (messageCursor.moveToFirst()) {

                        val num = messageCursor.getString(1)

                        val p = messageCursor.getInt(0)


                        messages.add(Message(
                                person= if (p == 0) Person(0, "") else Person(numberToContact(num, cr)?: -1, num),
                                read = messageCursor.getInt(2) != 0,
                                threadid = threadID,
                                timestamp = messageCursor.getLong(3),
                                data = SmsData(messageCursor.getString(4))
                        ))
                    }
                }

            } while (cursor.moveToNext())

            return Gson().toJsonTree(messages)
        }
        return null
    }
}