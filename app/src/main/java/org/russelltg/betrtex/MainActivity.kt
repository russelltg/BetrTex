package org.russelltg.betrtex

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import kotlinx.android.synthetic.main.activity_main.*
import java.net.NetworkInterface
import java.net.NetworkInterface.getNetworkInterfaces
import java.util.*


fun getIP(): String? {
    val interfaces = NetworkInterface.getNetworkInterfaces()
    for (intf in interfaces) {
        val addrs = Collections.list(intf.getInetAddresses())
        for (addr in addrs) {
            if (!addr.isLoopbackAddress()) {
                val sAddr = addr.getHostAddress()
                //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                val isIPv4 = sAddr.indexOf(':') < 0
                if (isIPv4) {
                    return sAddr
                }

            }
        }
    }
    return null

}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val requiredPermissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)

        if (!requiredPermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(requiredPermissions, 123)
        }

        stopService(Intent(this, ServerService::class.java))
        startService(Intent(this, ServerService::class.java))

        // get IP
        val ip = getIP()

        if (ip != null) {
            ipView.setText("IP Address: " + ip)
        }

        portView.setText("Port: 14563")

    }
}
