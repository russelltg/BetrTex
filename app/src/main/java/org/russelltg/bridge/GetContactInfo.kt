package org.russelltg.bridge

import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Base64
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.io.ByteArrayOutputStream

class GetContactInfo(serv: ServerService) : Command(serv) {

    override fun process(params: JsonElement): JsonElement? {
        val contactID = params.asInt

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

            val json = JsonObject()

            json["name"] = name
            json["b64_image"] = base64image

            // free the cursor
            contactsCursor.close()

            return json
        }

        val json = JsonObject()

        json["name"] = ""
        json["b64_image"] = ""
        return json
    }
}