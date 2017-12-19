package org.russelltg.bridge

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.net.NetworkInterface
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

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val requiredPermissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)

        if (!requiredPermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(requiredPermissions, 123)
        }

        startService(Intent(this, ServerService::class.java))

        // get IP
        val ip = getIP()

        if (ip != null) {
            ipView.text = resources.getString(R.string.ip_address, ip)
        }

        portView.text = resources.getString(R.string.port, 14563)
    }
}
