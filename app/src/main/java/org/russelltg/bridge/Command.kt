package org.russelltg.bridge

import com.google.gson.JsonElement

abstract class Command(val service: ServerService) {

    // process a command
    abstract fun process(params: JsonElement) : JsonElement?

    open fun close() {}
}
