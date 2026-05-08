package com.example.grabstandalone

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.realsil.sdk.audioconnect.smartwear.ActionStateNotification
import com.realsil.sdk.audioconnect.smartwear.SmartWearModelCallback
import com.realsil.sdk.audioconnect.smartwear.SmartWearModelProxy

private const val TAG = "GlassButton"
private const val GRAB_PACKAGE = "com.grabtaxi.passenger"
private const val NOTIFICATION_ID = 1
private const val CHANNEL_ID = "glass_button"

class GlassButtonService : Service() {

    private val modelClient by lazy {
        SmartWearModelProxy.getInstance().getModelClient(SmartWearDataStore.btAddress)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        modelClient?.registerCallback(buttonCallback)
        Log.d(TAG, "service started, callback registered")
    }

    override fun onDestroy() {
        super.onDestroy()
        modelClient?.unregisterCallback(buttonCallback)
        Log.d(TAG, "service stopped, callback unregistered")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val buttonCallback = object : SmartWearModelCallback() {
        override fun onReceivedDeviceAction(actionType: Int) {
            super.onReceivedDeviceAction(actionType)
            Log.d(TAG, "onReceivedDeviceAction actionType=$actionType")
            if (actionType == 2) endCall()
        }

        override fun onDeviceTriggeredTakePhoto() {
            super.onDeviceTriggeredTakePhoto()
            Log.d(TAG, "onDeviceTriggeredTakePhoto")
            handleButtonPress()
        }

        override fun onActionStateNotificationChanged(notification: ActionStateNotification) {
            super.onActionStateNotificationChanged(notification)
            Log.d(TAG, "onActionStateNotificationChanged action=${notification.action} state=${notification.state}")
        }
    }

    private fun handleButtonPress() {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        @Suppress("DEPRECATION")
        val callState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
        ) {
            TelephonyManager.CALL_STATE_IDLE
        } else {
            telephonyManager.callState
        }

        when (callState) {
            TelephonyManager.CALL_STATE_OFFHOOK,
            TelephonyManager.CALL_STATE_RINGING -> endCall()
            TelephonyManager.CALL_STATE_IDLE -> makeCall()
        }
    }

    private fun isCellularAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        return cm.allNetworks.any { network ->
            cm.getNetworkCapabilities(network)
                ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }
    }

    private fun makeCall() {
        if (!isCellularAvailable()) {
            Log.d(TAG, "no cellular, showing dialog via CallActivity")
            startActivity(
                Intent(this, CallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(CallActivity.EXTRA_SHOW_NO_CELLULAR, true)
            )
            return
        }
        try {
            packageManager.getPackageInfo(GRAB_PACKAGE, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Grab not installed, showing dialog via CallActivity")
            startActivity(
                Intent(this, CallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(CallActivity.EXTRA_SHOW_GRAB_NOT_INSTALLED, true)
            )
            return
        }
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "CALL_PHONE not granted, requesting via CallActivity")
            startActivity(
                Intent(this, CallActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(CallActivity.EXTRA_REQUEST_CALL_PERMISSION, true)
            )
            return
        }
        startActivity(
            Intent(Intent.ACTION_CALL, Uri.parse("tel:${SmartWearDataStore.activeNumber()}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (checkSelfPermission(Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "ANSWER_PHONE_CALLS not granted, cannot end call")
                return
            }
            (getSystemService(TELECOM_SERVICE) as TelecomManager).endCall()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Glasses", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Glasses connected")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

    companion object {
        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, GlassButtonService::class.java))

        fun stop(context: Context) =
            context.stopService(Intent(context, GlassButtonService::class.java))
    }
}
