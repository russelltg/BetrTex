package org.russelltg.bridge

data class Message (
        val id: Int,
        val person: Int,
        val threadid: Int,
        val message: String,
        val timestamp: Long
)