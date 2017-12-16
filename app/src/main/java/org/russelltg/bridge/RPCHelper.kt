package org.russelltg.bridge

import com.google.gson.Gson

data class RPCMessage<T> (
    val method: String,
    val id: Int,
    val params: T
)

fun <T> BuildRPCCall(method: String, id: Int, params: T): String {

    val gson = Gson()
    return gson.toJson(RPCMessage<T>(method, id, params))

}

data class StrippedRPCMessage (
    val method: String,
    val id: Int
)

fun BreakRPCCall(message: String): Pair<String, Int> {

    val message = Gson().fromJson(message, StrippedRPCMessage::class.java)

    return Pair(message.method, message.id)
}

