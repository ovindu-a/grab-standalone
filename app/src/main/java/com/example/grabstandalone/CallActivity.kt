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
        const val EXTRA_SHOW_GRAB_NOT_INSTALLED = "show_grab_not_installed"
        const val EXTRA_SHOW_NO_CELLULAR = "show_no_cellular"
    }

    private lateinit var binding: ActivityCallBinding
    private lateinit var connectivityManager: ConnectivityManager

    private val GRAB_PACKAGE = "com.grabtaxi.passenger"

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

        SmartWearDataStore.loadMode(this)
        binding.toggleMode.check(if (SmartWearDataStore.isProd) R.id.btnProd else R.id.btnSandbox)
        updateToggleColors(if (SmartWearDataStore.isProd) R.id.btnProd else R.id.btnSandbox)
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                SmartWearDataStore.saveMode(this, checkedId == R.id.btnProd)
                updateToggleColors(checkedId)
            }
        }

        GlassButtonService.start(this)

        if (intent.getBooleanExtra(EXTRA_REQUEST_CALL_PERMISSION, false)) {
            requestCallPermissionAndDial()
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_GRAB_NOT_INSTALLED, false)) {
            showGrabNotInstalledDialog()
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_NO_CELLULAR, false)) {
            showNoCellularDialog()
        }

        binding.btnCall.setOnClickListener {
            if (!isCellularAvailable) {
                showNoCellularDialog()
                return@setOnClickListener
            }
            if (!isGrabInstalled()) {
                showGrabNotInstalledDialog()
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
        if (intent.getBooleanExtra(EXTRA_SHOW_GRAB_NOT_INSTALLED, false)) {
            showGrabNotInstalledDialog()
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_NO_CELLULAR, false)) {
            showNoCellularDialog()
        }
    }

    private fun updateToggleColors(checkedId: Int) {
        listOf(binding.btnProd, binding.btnSandbox).forEach { btn ->
            val selected = btn.id == checkedId
            btn.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, if (selected) R.color.accent_purple_light else android.R.color.transparent)
            )
            btn.setTextColor(
                ContextCompat.getColor(this, if (selected) R.color.white else R.color.text_muted)
            )
        }
    }

    private fun showNoCellularDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cellular network not available")
            .setMessage("Connect to mobile network to take a call")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showGrabNotInstalledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Grab not installed")
            .setMessage("Please install and register for Grab to use this feature.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun disconnectAndReturn() {
        GlassButtonService.stop(this)
        MultiPeripheralConnectionManager.getInstance(applicationContext)
            .getPeripheralConnectionManager(SmartWearDataStore.btAddress)
            .disconnect()
        SmartWearDataStore.saveBtAddress(applicationContext, "")
        ConnectStateLiveData.value = false
        startActivity(
            Intent(this, ConnectActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
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
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${SmartWearDataStore.activeNumber()}")))
    }
}
