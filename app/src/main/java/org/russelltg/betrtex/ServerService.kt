package org.russelltg.betrtex

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.telephony.SmsManager
import java.net.InetSocketAddress

class ServerService : Service() {

    var serv: WsServer?= null
    var manager: SmsManager? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // get the sms manager
        manager = SmsManager.getDefault()

        // start the text receiver
        registerReceiver(TextReceiver(), IntentFilter("android.provider.Telephony.SMS_RECEIVED"))

        // start ws server
        try {
            serv = WsServer(InetSocketAddress("0.0.0.0", 14563), this)

            serv?.start()

        } catch(e: Exception) {
            e.printStackTrace()
        }

        return Service.START_NOT_STICKY
    }



    override fun onDestroy() {

        try {
            serv?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}


