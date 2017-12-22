package org.russelltg.bridge

import com.google.gson.annotations.SerializedName

data class Image (
        @SerializedName("base64_data")
        val base64Data: String,

        @SerializedName("mime_type")
        val mimeType: String
)
