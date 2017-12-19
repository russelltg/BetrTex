package org.russelltg.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ServiceStarter: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.startService(Intent(context, ServerService::class.java))
    }
}