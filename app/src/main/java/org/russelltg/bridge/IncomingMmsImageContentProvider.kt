package org.russelltg.bridge

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.nio.charset.Charset
import kotlin.math.absoluteValue

class IncomingMmsImageContentProvider : ContentProvider() {

    companion object {
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
        val CONTENT_URI = Uri.parse("content://org.russelltg.bridge.mms-image/image")

        init {
            uriMatcher.addURI("org.russelltg.bridge.mms-image", "image/#", 1)
        }

        fun getUri(transactionId: ByteArray, attachmentName: String): Uri {
            val id = transactionId.toString(Charset.defaultCharset()) + ":" + attachmentName
            return ContentUris.withAppendedId(CONTENT_URI, id.hashCode().toLong().absoluteValue)
        }
    }

    private fun getFile(uri: Uri): File? {
        if (uriMatcher.match(uri) == 1) {
            val id = uri.pathSegments[1].toLong()
            return File(context.cacheDir, "$id.data")
        }
        return null
    }

    override fun insert(p0: Uri?, p1: ContentValues?): Uri? {
        return null
    }

    override fun query(p0: Uri?, p1: Array<out String>?, p2: String?, p3: Array<out String>?, p4: String?): Cursor? {
        return null
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun update(p0: Uri?, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int {
        return 0
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

    override fun delete(uri: Uri?, p1: String?, p2: Array<out String>?): Int {
        if (uri == null) {
            return 0
        }
        if (uriMatcher.match(uri) == 1) {
            return if (getFile(uri)!!.delete()) 1 else 0
        }
        return 0
    }

    override fun getType(p0: Uri?): String? {
        return null
    }
}