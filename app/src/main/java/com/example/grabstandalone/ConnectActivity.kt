package com.example.grabstandalone

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.grabstandalone.databinding.ActivityConnectBinding
import com.realsil.sdk.bbpro.MultiPeripheralConnectionManager
import com.realsil.sdk.core.bluetooth.scanner.ScannerParams
import com.realsil.sdk.core.bluetooth.scanner.SpecScanRecord
import com.realsil.sdk.support.base.BaseActivity
import com.realsil.sdk.support.scanner.BluetoothScannerActivity
import com.realsil.sdk.support.scanner.LegacyScannerActivity

class ConnectActivity : BaseActivity() {

    private lateinit var binding: ActivityConnectBinding
    private lateinit var viewModel: ConnectViewModel

    private val requiredPermissions: Array<String>
        get() {
            val perms = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                perms += Manifest.permission.BLUETOOTH_SCAN
                perms += Manifest.permission.BLUETOOTH_CONNECT
            } else {
                perms += Manifest.permission.ACCESS_FINE_LOCATION
            }
            perms += Manifest.permission.READ_PHONE_STATE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                perms += Manifest.permission.ANSWER_PHONE_CALLS
            }
            return perms.toTypedArray()
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            checkAlreadyConnectedOrScan()
        } else {
            binding.tvStatus.text = "Bluetooth permission denied"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ConnectViewModel::class.java]

        ConnectStateLiveData.observe(this) { connected ->
            if (connected == true) {
                viewModel.getConnectedAddress()?.let { address ->
                    SmartWearDataStore.saveBtAddress(applicationContext, address)
                }
                startActivity(Intent(this, CallActivity::class.java))
                finish()
            }
        }

        requestPermissionsThenContinue()
    }

    private fun requestPermissionsThenContinue() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            checkAlreadyConnectedOrScan()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun checkAlreadyConnectedOrScan() {
        val savedAddress = SmartWearDataStore.loadBtAddress(applicationContext)
        if (savedAddress.isNotEmpty()) {
            val peripheralManager = MultiPeripheralConnectionManager
                .getInstance(applicationContext)
                .getPeripheralConnectionManager(savedAddress)
            if (peripheralManager.isConnected) {
                SmartWearDataStore.btAddress = savedAddress
                ConnectStateLiveData.postValue(true)
                return
            }
        }
        startScan()
    }

    private fun startScan() {
        binding.tvStatus.text = "Scanning for glasses…"
        val params = ScannerParams(ScannerParams.SCAN_MODE_SPP).apply {
            isNameNullable = false
        }
        val intent = Intent(this, LegacyScannerActivity::class.java)
        intent.putExtra(BluetoothScannerActivity.EXTRA_KEY_SCAN_PARAMS, params)
        startBluetoothScanner(intent)
    }

    override fun onBtScannerCallback(device: BluetoothDevice, scanRecord: SpecScanRecord?) {
        binding.tvStatus.text = "Connecting…"
        viewModel.startConnect(applicationContext, device.address)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clear()
    }
}
