package org.russelltg.betrtex

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi

abstract class Command {

    // process a command
    abstract fun process(service: ServerService, message: String) : String?
}

class SendTextCommand: Command() {

    data class SendTextMessage (
            val type: String = "send-text",
            val to: String,
            val message: String
    )

    override fun process(service: ServerService, message: String): String? {

        // to JSON
        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

        val msg = moshi.adapter(SendTextMessage::class.java).fromJson(message)

        // send it
        for (single in service.manager?.divideMessage(message)!!.iterator()) {
            service.manager?.sendTextMessage(msg!!.to, null, msg!!.message, null, null)
        }

        // notify of this new message
        service.serv?.textSent(msg!!.to, msg!!.message)

        return null
    }

}



