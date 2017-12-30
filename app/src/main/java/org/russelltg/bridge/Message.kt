package org.russelltg.bridge

import com.google.gson.annotations.SerializedName

sealed class MessageData {
    data class Text(
            val type: String = "text",
            val message: String) : MessageData()
    data class Image(
            val type: String = "image",
            val image: String
    ) : MessageData()
}

data class Person (
        @SerializedName("contactid")
        val contactID: Long,
        val number: String
)

data class Message (
        val person: Person,
        @SerializedName("threadid")
        val threadID: Int,
        val timestamp: Long,
        val read: Boolean,
        val data: MessageData
)