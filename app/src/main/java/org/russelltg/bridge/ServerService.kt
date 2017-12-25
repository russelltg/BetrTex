package org.russelltg.bridge

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.provider.Telephony
import java.net.InetSocketAddress

class ServerService : Service() {

    var server: WsServer? = null

    private var smsReceiver: SmsReceiver? = null
    private var mmsReceiver: MmsReceiver? = null

    override fun onCreate() {
        // start the text receiver
        smsReceiver = SmsReceiver(this)
        mmsReceiver = MmsReceiver(this)

        registerReceiver(smsReceiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
        registerReceiver(mmsReceiver, IntentFilter("android.provider.Telephony.WAP_PUSH_RECEIVED", "application/vnd.wap.mms-message"), "android.permission.BROADCAST_WAP_PUSH", null)


        // start ws server
        try {
            server = WsServer(InetSocketAddress("0.0.0.0", 14566), this)

            server?.start()

        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }



    override fun onDestroy() {

        unregisterReceiver(smsReceiver)
        unregisterReceiver(mmsReceiver)

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


