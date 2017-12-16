package org.russelltg.bridge

import com.google.gson.JsonElement

abstract class Command {

    var service: ServerService

    constructor(service: ServerService) {
        this.service = service
    }

    // process a command
    abstract fun process(params: JsonElement) : JsonElement?

    open fun close() {}
}
