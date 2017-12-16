package org.russelltg.betrtex

import android.content.ContentResolver
import android.provider.ContactsContract

fun numberToContact(num: String, cr: ContentResolver): Long? {

    // get contact id from phone number
    val contactsCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone._ID),
            "PHONE_NUMBERS_EQUAL(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ",?)",
            arrayOf(num), null)

    if (!contactsCursor.moveToFirst()) {
        return null
    }

    return contactsCursor.getLong(0)
}