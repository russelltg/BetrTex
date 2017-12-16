package org.russelltg.bridge

sealed class MessageData

data class SmsData(val message: String) : MessageData()
enum class MmsType {
    TEXT,
    IMAGE
}
data class MmsData(val type: MmsType, val data: String) : MessageData()

data class Person (val contactid: Long, val number: String)

data class Message (
        val person: Person,
        val threadid: Int,
        val timestamp: Long,
        val read: Boolean,
        val data: MessageData
)