package org.russelltg.bridge

import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.google.android.mms.pdu_alt.*
import com.google.android.mms.ContentType
import java.io.FileOutputStream
import java.nio.charset.Charset


class SmsReceiver(private val service: ServerService) : BroadcastReceiver() {

    override fun onReceive(ctx: Context?, intent: Intent?) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        val orig = messages[0].displayOriginatingAddress
        var message = ""
        for (m in messages) {
            message += m.displayMessageBody
        }
        val timestamp = messages[0].timestampMillis

        val cr = ctx!!.contentResolver

        // get from db
        try {
            val cursor = cr.query(Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms.Inbox.PERSON, Telephony.Sms.ADDRESS, Telephony.Sms.Inbox.THREAD_ID, Telephony.Sms.DATE_SENT, Telephony.Sms.READ, Telephony.Sms.BODY),
                    "address=? AND body=? AND date_sent=?",
                    arrayOf(orig, message, timestamp.toString()), null)

            if (!cursor.moveToFirst()) {
                return
            }

            service.server?.textReceived(Message(
                    person = Person(cursor.getLong(0), cursor.getString(1)),
                    threadID = cursor.getInt(2),
                    timestamp = cursor.getLong(3),
                    read = cursor.getInt(4) != 0,
                    data = MessageData.Text(message = cursor.getString(5))))

            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }
}

class MmsReceiver(private val service: ServerService): BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) {
            return
        }

        Log.w("MMS", "Mms intent received")

        val pduData = intent.getByteArrayExtra("data")

        // parse the PDU
        val parser = PduParser(pduData)
        val pdu = parser.parse() ?: return

        // only handle notifications
        if (!isNotification(pdu)) {
            return
        }

        val notificationPdu = pdu as NotificationInd

        // TODO: is the default charset right?
        val contentLocation = notificationPdu.contentLocation.toString(Charset.defaultCharset())

        Log.w("MMS", "Downloading MMS at $contentLocation")

        val smsManager = SmsManager.getDefault() ?: return

        // temporary uri for PDU
        val pduUri = MmsPduContentProvider.makeTemporaryUri()

        val lock = java.lang.Object()

        // create receiver
        val broadcastReceiver = object: BroadcastReceiver() {
            override fun onReceive(msgContext: Context?, msgIntent: Intent?) {
                if (msgContext == null) {
                    lock.notifyAll()
                    return
                }

                val fin = context.contentResolver.openInputStream(pduUri)
                val pduBytes = fin.readBytes()

                // parse the new pdu
                val retreivedPdu = PduParser(pduBytes).parse() as RetrieveConf

                val allRecipientsPhoneNumber = mutableListOf<String>()

                val from = retreivedPdu.from.string
                allRecipientsPhoneNumber.add(from)

                if (retreivedPdu.to != null) {
                    retreivedPdu.to.forEach {
                        allRecipientsPhoneNumber.add(it.string)
                    }
                }

                val cr = context.contentResolver

                // get thread id
                ////////////////

                // convert each phone number to canonical address
                val allRecipientsCanon = allRecipientsPhoneNumber.map {
                    val canonCursor = cr.query(Uri.parse("content://mms-sms/canonical-addresses"),
                            arrayOf(Telephony.CanonicalAddressesColumns._ID), "${Telephony.CanonicalAddressesColumns.ADDRESS}=?", arrayOf(it), null)

                    if (canonCursor.moveToFirst()) {
                        val ret = canonCursor.getLong(0)
                        canonCursor.close()
                        ret
                    } else {
                        canonCursor.close()
                        -1
                    }
                }

                // get recipients string
                val recipientIDs = allRecipientsCanon.sorted().joinToString(separator = " ")

                val threadsCursor = cr.query(Uri.parse("content://mms-sms/conversations?simple=true"),
                        arrayOf(Telephony.MmsSms._ID), "${Telephony.Threads.RECIPIENT_IDS}=?", arrayOf(recipientIDs), null)

                val threadid = if (threadsCursor.moveToFirst()) {
                    threadsCursor.getInt(0)
                } else {
                    -1
                }
                threadsCursor.close()

                val body = retreivedPdu.body
                if (body != null) {
                    loop@ for (i in 0 until body.partsNum) {
                        val part = body.getPart(i)

                        val contentType = part.contentType.toString(Charset.defaultCharset())

                        val person = Person(contactID = numberToContact(from, msgContext.contentResolver) ?: 0, number = from)

                        val message = when {
                            ContentType.isTextType(contentType) -> {
                                val data = part.data ?: continue@loop
                                val message = data.toString(Charset.defaultCharset())

                                Message(
                                        person = person,
                                        data = MessageData.Text(message = message),
                                        threadID = threadid,
                                        read = false,
                                        timestamp = retreivedPdu.date
                                )
                            }
                            ContentType.isImageType(contentType) -> {
                                // store the image
                                val uri = IncomingMmsImageContentProvider.getUri(transactionId = retreivedPdu.transactionId, attachmentName = part.name.toString(Charset.defaultCharset()))

                                // get the file
                                val file = context.contentResolver.openFileDescriptor(uri, "w") ?: continue@loop

                                val ostream = FileOutputStream(file.fileDescriptor)
                                ostream.write(part.data)

                                Message(
                                        person = person,
                                        timestamp = retreivedPdu.date,
                                        data = MessageData.Image(
                                                image =  uri.toString()
                                        ),
                                        read = false,
                                        threadID = threadid
                                )
                            }
                            else -> null
                        }

                        if (message != null) {
                            service.server?.textReceived(message)
                        }

                    }
                }
                lock.notifyAll()
            }
        }
        context.applicationContext.registerReceiver(broadcastReceiver, IntentFilter("MMS_DOWNLOAD_COMPLETED"))

        smsManager.downloadMultimediaMessage(context, contentLocation, pduUri, null,
                PendingIntent.getBroadcast(context, 1, Intent("MMS_DOWNLOAD_COMPLETED"), PendingIntent.FLAG_ONE_SHOT))

        lock.wait()

        context.applicationContext.unregisterReceiver(broadcastReceiver)
    }

    private fun isNotification(pdu: GenericPdu): Boolean {
        return pdu.messageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND
    }
}

