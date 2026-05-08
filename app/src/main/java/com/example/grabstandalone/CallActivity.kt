package com.example.grabstandalone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.grabstandalone.databinding.ActivityCallBinding
import com.realsil.sdk.bbpro.MultiPeripheralConnectionManager

class CallActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REQUEST_CALL_PERMISSION = "request_call_permission"
    }

    private lateinit var binding: ActivityCallBinding
    private lateinit var connectivityManager: ConnectivityManager

    private val GRAB_PACKAGE = "com.grabtaxi.passenger"
    private val DEFAULT_NUMBER = "+6590855038"

    private var isCellularAvailable = true

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            placeCall()
        } else {
            Toast.makeText(this, "Phone permission required to make calls", Toast.LENGTH_SHORT).show()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = checkCellular()
        override fun onLost(network: Network) = checkCellular()
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = checkCellular()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(),
            networkCallback
        )
        checkCellular()

        GlassButtonService.start(this)

        if (intent.getBooleanExtra(EXTRA_REQUEST_CALL_PERMISSION, false)) {
            requestCallPermissionAndDial()
        }

        binding.btnCall.setOnClickListener {
            if (!isCellularAvailable) {
                AlertDialog.Builder(this)
                    .setTitle("Cellular network not available")
                    .setMessage("Connect to mobile network to take a call")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }
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
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun checkCellular() {
        isCellularAvailable = connectivityManager.allNetworks.any { network ->
            connectivityManager.getNetworkCapabilities(network)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
        runOnUiThread { updateCallButton() }
    }

    private fun updateCallButton() {
        val tint = if (isCellularAvailable) R.color.accent_purple else R.color.surface
        binding.btnCall.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, tint)
        )
        binding.btnCall.alpha = if (isCellularAvailable) 1f else 0.5f
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_REQUEST_CALL_PERMISSION, false)) {
            requestCallPermissionAndDial()
        }
    }

    private fun disconnectAndReturn() {
        GlassButtonService.stop(this)
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
