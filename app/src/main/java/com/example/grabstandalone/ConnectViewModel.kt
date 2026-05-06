package com.example.grabstandalone

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.realsil.sdk.bbpro.MultiPeripheralConnectionManager
import com.realsil.sdk.bbpro.PeripheralConnectionManager
import com.realsil.sdk.bbpro.core.peripheral.ConnectionParameters
import com.realsil.sdk.bbpro.core.peripheral.PeripheralParameters
import com.realsil.sdk.bbpro.vendor.VendorModelCallback

object ConnectStateLiveData : MutableLiveData<Boolean>()

class ConnectViewModel : ViewModel() {

    private var mConnectionManager: PeripheralConnectionManager? = null

    fun startConnect(context: Context?, deviceAddress: String?) {
        mConnectionManager = MultiPeripheralConnectionManager
            .getInstance(context)
            .getPeripheralConnectionManager(deviceAddress)

        mConnectionManager?.registerVendorModelCallback(mVendorModelCallback)

        if (mConnectionManager?.isConnected == true) {
            ConnectStateLiveData.postValue(true)
            return
        }

        val peripheralParams = PeripheralParameters.Builder()
            .syncDataWhenConnected(true)
            .connectA2dp(true)
            .listenHfp(true)
            .build()

        val connectParams = ConnectionParameters.Builder(deviceAddress!!)
            .channelType(ConnectionParameters.CHANNEL_TYPE_SPP)
            .peripheralParameters(peripheralParams)
            .build()

        mConnectionManager?.startConnect(connectParams)
    }

    fun getConnectedAddress(): String? = mConnectionManager?.deviceAddress

    fun clear() {
        mConnectionManager?.unregisterVendorModelCallback(mVendorModelCallback)
    }

    private val mVendorModelCallback = object : VendorModelCallback() {
        override fun onStateChanged(state: Int) {
            when (state) {
                PeripheralConnectionManager.STATE_DATA_PREPARED ->
                    ConnectStateLiveData.postValue(true)
                PeripheralConnectionManager.STATE_DEVICE_DISCONNECTED ->
                    ConnectStateLiveData.postValue(false)
            }
        }
    }
}
