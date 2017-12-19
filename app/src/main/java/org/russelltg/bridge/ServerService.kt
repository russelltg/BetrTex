package org.russelltg.bridge

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v4.app.NotificationCompat
import java.net.InetSocketAddress

class ServerService : Service() {

    var server: WsServer? = null

    private var textReceiver: TextReceiver? = null

    override fun onCreate() {
        // start the text receiver
        textReceiver = TextReceiver(this)
        registerReceiver(textReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

        // start ws server
        try {
            server = WsServer(InetSocketAddress("0.0.0.0", 14563), this)

            server?.start()

        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }



    override fun onDestroy() {

        unregisterReceiver(textReceiver)

        try {
            server?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}


