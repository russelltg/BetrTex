package org.russelltg.bridge

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.ByteArrayOutputStream

class GetImage(service: ServerService) :  Command(service) {
    override fun process(params: JsonElement): JsonElement? {
        val imageUri = Uri.parse(params.asString)

        val cr = service.contentResolver

        // load the image from the URI
        val bm = MediaStore.Images.Media.getBitmap(cr, imageUri)

        // encode as jpeg
        val stream = ByteArrayOutputStream()
        val compressed = bm.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        if (compressed) {
            // base64 encode it
            return  Gson().toJsonTree(Image(
                    base64Data = Base64.encodeToString(stream.toByteArray(), 0),
                    mimeType = "data:image/jpeg;base64"))
        }

        return null
    }
}