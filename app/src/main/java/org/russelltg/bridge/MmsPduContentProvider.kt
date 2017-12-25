package org.russelltg.bridge

import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

class MmsPduContentProvider : ContentProvider() {

    companion object {
        private val uriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        val CONTENT_URI: Uri = Uri.parse("content://org.russelltg.bridge.mms/mms")

        init {
            uriMatcher.addURI("org.russelltg.bridge.mms", "mms/#", 1)
        }

        fun makeTemporaryUri(): Uri {
            return ContentUris.withAppendedId(CONTENT_URI, System.currentTimeMillis())
        }
    }

    override fun insert(uri: Uri?, contentValues: ContentValues?): Uri? {
        return null
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sorting: String?): Cursor? {
        return null
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun update(uri: Uri?, values: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
    }

    override fun delete(uri: Uri?, p1: String?, p2: Array<out String>?): Int {
        if (uri == null) {
            return 0
        }

        if (uriMatcher.match(uri) == 1) {
            return if (getFile(uri).delete()) 1 else 0
        }
        return 0
    }

    override fun getType(p0: Uri?): String? {
        return null
    }

    private fun getFile(uri: Uri): File {
        val id = uri.pathSegments[1].toLong()
        return File(context.cacheDir, "$id.mmsbody")
    }

    override fun openFile(uri: Uri?, mode: String?): ParcelFileDescriptor? {
        if (uri == null) {
            return null
        }

        if (uriMatcher.match(uri) == 1) {
            return ParcelFileDescriptor.open(getFile(uri), when(mode) {
                "w" -> ParcelFileDescriptor.MODE_TRUNCATE or
                        ParcelFileDescriptor.MODE_CREATE or
                        ParcelFileDescriptor.MODE_WRITE_ONLY
                "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                else -> 0
            })
        }
        return null
    }
}