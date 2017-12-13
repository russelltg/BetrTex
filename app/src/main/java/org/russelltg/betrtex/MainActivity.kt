package org.russelltg.betrtex

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val requiredPermissions = arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)

        if (!requiredPermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(requiredPermissions, 123)
        }

        val stopped = stopService(Intent(this, ServerService::class.java))
        val comp = startService(Intent(this, ServerService::class.java))


        var c = contentResolver.query(Uri.parse("content://mms-sms/conversations?simple=true"), null, null, null, null);

        c.columnNames

        print("Hi")

    }
}
