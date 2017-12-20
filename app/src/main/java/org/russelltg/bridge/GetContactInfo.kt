package org.russelltg.bridge

import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import java.io.ByteArrayOutputStream

class GetContactInfo(service: ServerService) : Command(service) {

    data class ContactInfo(
            val name: String,

            @SerializedName("b64_image")
            val b64Image: String
    )

    override fun process(params: JsonElement): JsonElement? {
        val contactID = params.asInt

        if (contactID == -1) {
            return null
        }

        val cr = service.contentResolver

        // query
        val contactsCursor = cr.query(Uri.withAppendedPath(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, contactID.toString()),
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null)

        if (contactsCursor.moveToFirst()) {

            val name = contactsCursor.getString(0)

            var base64image = ""
            if (!contactsCursor.isNull(1)) {

                val imageUri = Uri.parse(contactsCursor.getString(1))

                // load the image from the URI
                val bm = MediaStore.Images.Media.getBitmap(cr, imageUri)

                // encode as jpeg
                val stream = ByteArrayOutputStream()
                val compressed = bm.compress(Bitmap.CompressFormat.JPEG, 100, stream)

                if (compressed) {
                    // base64 encode it
                    base64image = "data:image/jpeg;base64, " + Base64.encodeToString(stream.toByteArray(), 0)
                }
            }

            val contactInfo = ContactInfo(name = name, b64Image = base64image)

            // free the cursor
            contactsCursor.close()

            return Gson().toJsonTree(contactInfo)
        }

        return Gson().toJsonTree(ContactInfo(name = "", b64Image = ""))
    }
}