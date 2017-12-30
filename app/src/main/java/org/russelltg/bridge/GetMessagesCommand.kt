package org.russelltg.bridge

import android.annotation.SuppressLint
import android.net.Uri
import android.provider.Telephony
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName


class GetMessagesCommand(service: ServerService) : Command(service) {

    data class Params (
            @SerializedName("threadid")
            val threadID: Int,
            val from: Int,
            val to: Int
    )

    override fun process(params: JsonElement): JsonElement? {

        val parameters = Gson().fromJson<Params>(params)

        val cr = service.contentResolver

        val cursor = cr.query(Uri.withAppendedPath(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, parameters.threadID.toString()),
            arrayOf(Telephony.MmsSms._ID, "ct_t"),
                null, null, null)

        if (cursor.moveToFirst()) {

            // clamp the end
            val to = Integer.min(parameters.to, cursor.count)

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
                    messages.addAll(messagesFromMms(cr, messageID))
                } else {
                    // sms
                    val messageCursor = cr.query(Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageID.toString()),
                            arrayOf(Telephony.Sms.PERSON, Telephony.Sms.ADDRESS, Telephony.Sms.READ, Telephony.Sms.DATE, Telephony.Sms.BODY),
                            null, null, null)


                    if (messageCursor.moveToFirst()) {

                        val num = messageCursor.getString(1)
                        val p = messageCursor.getInt(0)

                        messages.add(Message(
                                person= if (p == 0) Person(0, "") else Person(numberToContact(num, cr)?: -1, num),
                                read = messageCursor.getInt(2) != 0,
                                threadID = parameters.threadID,
                                timestamp = messageCursor.getLong(3),
                                data = MessageData.Text(message = messageCursor.getString(4))
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