package com.example.grabstandalone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.grabstandalone.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

    private val GRAB_PACKAGE = "com.grabtaxi.passenger"
    private val DEFAULT_NUMBER = "+6590855038"

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            placeCall()
        } else {
            Toast.makeText(this, "Phone permission required to make calls", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCall.setOnClickListener {
            if (!isGrabInstalled()) {
                AlertDialog.Builder(this)
                    .setTitle("Grab not installed")
                    .setMessage("Please install and register for Grab to use this feature.")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
            requestCallPermissionAndDial()
        }

        binding.btnDisconnect.setOnClickListener {
            SmartWearDataStore.saveBtAddress(applicationContext, "")
            startActivity(Intent(this, ConnectActivity::class.java))
            finish()
        }
    }

    private fun isGrabInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(GRAB_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun requestCallPermissionAndDial() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            placeCall()
        } else {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    private fun placeCall() {
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$DEFAULT_NUMBER")))
    }
}
