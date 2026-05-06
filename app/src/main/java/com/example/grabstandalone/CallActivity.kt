package com.example.grabstandalone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.grabstandalone.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

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
            val input = binding.etPhoneNumber.text?.toString()?.trim() ?: ""
            if (input.isEmpty()) {
                binding.etPhoneNumber.hint = "Using default: $DEFAULT_NUMBER"
            }
            requestCallPermissionAndDial()
        }

        binding.btnDisconnect.setOnClickListener {
            SmartWearDataStore.saveBtAddress(applicationContext, "")
            startActivity(Intent(this, ConnectActivity::class.java))
            finish()
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
        val input = binding.etPhoneNumber.text?.toString()?.trim() ?: ""
        val number = if (input.isNotEmpty()) input else DEFAULT_NUMBER
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        startActivity(intent)
    }
}
