package com.example.grabstandalone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.grabstandalone.databinding.ActivityCallBinding
import com.realsil.sdk.audioconnect.smartwear.ActionStateNotification
import com.realsil.sdk.audioconnect.smartwear.SmartWearModelCallback
import com.realsil.sdk.audioconnect.smartwear.SmartWearModelProxy
import com.realsil.sdk.bbpro.MultiPeripheralConnectionManager

private const val TAG = "GlassButton"

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding

    private val GRAB_PACKAGE = "com.grabtaxi.passenger"
    private val DEFAULT_NUMBER = "+6590855038"

    private val modelClient by lazy {
        SmartWearModelProxy.getInstance().getModelClient(SmartWearDataStore.btAddress)
    }

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

        modelClient?.registerCallback(buttonCallback)

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
            disconnectAndReturn()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        modelClient?.unregisterCallback(buttonCallback)
    }

    private val buttonCallback = object : SmartWearModelCallback() {
        override fun onReceivedDeviceAction(actionType: Int) {
            super.onReceivedDeviceAction(actionType)
            Log.d(TAG, "onReceivedDeviceAction actionType=$actionType")
        }

        override fun onDeviceTriggeredTakePhoto() {
            super.onDeviceTriggeredTakePhoto()
            Log.d(TAG, "onDeviceTriggeredTakePhoto (actionType=1 alias)")
            runOnUiThread {
                if (!isGrabInstalled()) {
                    AlertDialog.Builder(this@CallActivity)
                        .setTitle("Grab not installed")
                        .setMessage("Please install and register for Grab to use this feature.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@runOnUiThread
                }
                requestCallPermissionAndDial()
            }
        }

        override fun onActionStateNotificationChanged(notification: ActionStateNotification) {
            super.onActionStateNotificationChanged(notification)
            Log.d(TAG, "onActionStateNotificationChanged action=${notification.action} state=${notification.state}")
        }
    }

    private fun disconnectAndReturn() {
        MultiPeripheralConnectionManager.getInstance(applicationContext)
            .getPeripheralConnectionManager(SmartWearDataStore.btAddress)
            .disconnect()
        SmartWearDataStore.saveBtAddress(applicationContext, "")
        ConnectStateLiveData.value = false
        startActivity(Intent(this, ConnectActivity::class.java))
        finish()
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
