package org.russelltg.betrtex

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.provider.Telephony
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

abstract class Command {

    var service: ServerService

    constructor(service: ServerService) {
        this.service = service
    }

    // process a command
    abstract fun process(params: JsonElement) : JsonElement?

    open fun close() {}
}
