package org.russelltg.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Telephony
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.klinker.android.send_message.Settings
import com.klinker.android.send_message.Transaction
import java.util.*


/**
 * Command to send a text
 *
 * @property service The service to initialize [Command] with
 *
 * # Parameters
 * ```json
 * {
 *   "thread": 3, // thread ID
 *   "numbers": [
 *      "+11234138537",
 *      "+12123124121"
 *   ],
 *   message: "Hello mum!"
 * }
 * ```
 *
 * # Returns
 * ```json
 * {}
 * ```
 *
 */
class SendTextCommand(service: ServerService): Command(service) {

    companion object {
        private const val SENT_ACTION: String = "SMS_SENT_ACTION"
    }

    sealed class NewMessageData {
        data class Text (
                val message: String
        ) : NewMessageData()

        data class Image (
                @SerializedName("base64_image")
                val base64Image: String,

                @SerializedName("mime_type")
                val mimeType: String
        ) : NewMessageData()
    }

    data class SendTextParams (

            // The thread ID (_id in content://mms-sms/conversations)
            val thread: Long,

            // The phone numbers to send to
            val numbers: Array<String>,

            // The message to send
            val message: NewMessageData
    )

    private val receiver: BroadcastReceiver

    init {

        // initialize the receiver so we know when the message was sent
        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {



            }
        }

        // register callbacks
        service.registerReceiver(receiver, IntentFilter(SENT_ACTION))

    }

    override fun close() {
        service.unregisterReceiver(receiver)
    }

    override fun process(params: JsonElement): JsonElement? {


        // from json
        val msg = Gson().fromJson(params, SendTextParams::class.java)

        val s = Settings()

        s.useSystemSending = true

        val transaction = Transaction(service, s)

        val message = when (msg.message) {
            is NewMessageData.Text -> {
                com.klinker.android.send_message.Message(msg.message.message, msg.numbers)
            }
            is NewMessageData.Image -> {
                val message = com.klinker.android.send_message.Message("", msg.numbers)

                // decode the base64
                val decoded = Base64.decode(msg.message.base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)

                message.setImage(bitmap)

                message
            }

        }

//        transaction.setExplicitBroadcastForSentMms(Intent(SENT_ACTION))
        transaction.setExplicitBroadcastForSentSms(Intent(SENT_ACTION))

        transaction.sendNewMessage(message, msg.thread)



        return null
    }

}