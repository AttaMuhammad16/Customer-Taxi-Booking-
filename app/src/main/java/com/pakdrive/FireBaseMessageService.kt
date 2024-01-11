package com.pakdrive

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.Nullable
import javax.inject.Inject

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FireBaseMessageService : FirebaseMessagingService() {
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            if (InternetChecker().isInternetConnectedWithPackage(this@FireBaseMessageService)){
                Utils.updateFCMToken(Utils.CUSTOMER,Utils.CUSTOMER_TOKEN_NODE,token,auth)
            }
        }
    }

}
