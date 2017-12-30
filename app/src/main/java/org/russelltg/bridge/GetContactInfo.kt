package org.russelltg.bridge

import android.net.Uri
import android.provider.ContactsContract
import com.google.gson.Gson
import com.google.gson.JsonElement

class GetContactInfo(service: ServerService) : Command(service) {

    data class ContactInfo(
            val name: String,
            val image: String
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

            var imageUri = ""
            if (!contactsCursor.isNull(1)) {
                imageUri = contactsCursor.getString(1)
            }

            val contactInfo = ContactInfo(name = name, image = imageUri
            )

            // free the cursor
            contactsCursor.close()

            return Gson().toJsonTree(contactInfo)
        }

        return Gson().toJsonTree(ContactInfo(name = "", image = ""))
    }
}