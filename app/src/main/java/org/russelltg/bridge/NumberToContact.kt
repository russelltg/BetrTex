package org.russelltg.bridge

import android.content.ContentResolver
import android.provider.ContactsContract

/**
 * From a phone number, get the contact ID
 *
 * @param num The phone number
 * @param cr the ContentResolver to query with
 *
 * @return The contact ID, or null if it wasn't found
 */
fun numberToContact(num: String, cr: ContentResolver): Long? {

    // get contact id from phone number
    val contactsCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone._ID),
            "PHONE_NUMBERS_EQUAL(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ",?)",
            arrayOf(num), null)

    if (!contactsCursor.moveToFirst()) {
        return null
    }

    // cache the result so we can close the cursor
    val ret = contactsCursor.getLong(0)

    // close that cursor
    contactsCursor.close()

    return ret
}