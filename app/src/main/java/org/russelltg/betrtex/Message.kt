package org.russelltg.betrtex

data class Message (
        val person: Int,
        val threadid: Int,
        val message: String,
        val timestamp: Long
)