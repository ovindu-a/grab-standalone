package com.example.grabstandalone

import android.app.Application
import com.realsil.sdk.audioconnect.smartwear.SmartWearModelProxy
import com.realsil.sdk.bbpro.BeeProParams
import com.realsil.sdk.bbpro.MultiPeripheralConnectionManager
import com.realsil.sdk.bbpro.core.transportlayer.TransportLayer
import com.realsil.sdk.core.RtkConfigure
import com.realsil.sdk.core.RtkCore
import com.realsil.sdk.core.logger.ZLogger
import com.realsil.sdk.dfu.RtkDfu
import com.realsil.sdk.support.RtkSupport

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        val configure = RtkConfigure.Builder()
            .debugEnabled(false)
            .printLog(true)
            .globalLogLevel(ZLogger.DEBUG)
            .logTag("GrabStandalone")
            .devModeEnabled(true)
            .build()
        RtkCore.initialize(this, configure)

        RtkDfu.initialize(this, true)
        RtkSupport.initialize(this, false)

        val beeProParams = BeeProParams.Builder()
            .syncDataWhenConnected(true)
            .connectA2dp(true)
            .listenHfp(true)
            .build()
        MultiPeripheralConnectionManager.getInstance(this).initialize(beeProParams)

        SmartWearModelProxy.initialize(this)

        TransportLayer.TDBG = false
    }
}
