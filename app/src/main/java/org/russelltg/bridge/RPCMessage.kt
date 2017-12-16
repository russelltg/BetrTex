package org.russelltg.bridge


data class RPCMessage<out T>(
        val jsonrpc: Int = 2,
        val id: Int,
        val method: String,
        val params: T
)