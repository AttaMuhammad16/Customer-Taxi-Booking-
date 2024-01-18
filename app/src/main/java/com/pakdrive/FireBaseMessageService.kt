package com.pakdrive

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.Rating
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
import com.pakdrive.MyConstants.CUSTOMER
import com.pakdrive.MyConstants.CUSTOMER_TOKEN_NODE
import com.pakdrive.MyConstants.DRIVERUID
import com.pakdrive.MyConstants.TITLE
import com.pakdrive.MyConstants.approvedConst
import com.pakdrive.ui.activities.LiveDriverViewActivity
import com.pakdrive.ui.activities.MainActivity
import com.pakdrive.ui.activities.RatingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.annotation.Nullable
import javax.inject.Inject

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class FireBaseMessageService : FirebaseMessagingService() {
    val auth: FirebaseAuth by lazy{
        FirebaseAuth.getInstance()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        val approve=data[approvedConst]

        if (approve=="false"){
            remoteMessage.notification?.let {
                showCancelNotification(it.title?:"Pak Drive",it.body?:"Ride cancelled")
            }
        }else if (approve=="true"){
            remoteMessage.notification?.let {
                var driverUid=data[DRIVERUID]
                showRideCompletedNotification(it.title?:"Pak Drive",it.body?:"Ride Completed",driverUid!!)
            }
        }else if (approve=="reached"){
            remoteMessage.notification?.let {
                showPickUpPointNotification(it.title?:"Pak Drive",it.body?:"Ride Completed")
            }
        }

    }

    override fun onNewToken(token: String) {
        if (auth.currentUser!=null){
            super.onNewToken(token)
            CoroutineScope(Dispatchers.IO).launch {
                if (InternetChecker().isInternetConnectedWithPackage(this@FireBaseMessageService)&&auth.currentUser!=null){
                    Utils.updateFCMToken(CUSTOMER,CUSTOMER_TOKEN_NODE,token,auth)
                }
            }
        }
    }

    private fun showCancelNotification(title: String,messageBody: String){
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(TITLE,title)
        val pendingIntent = PendingIntent.getActivity(this, 5, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        com.pakdrive.service.notification.NotificationManager.showNotification(5, "Pak Drive ride cancel", pendingIntent, this, title, messageBody)
    }

    private fun showRideCompletedNotification(title: String,messageBody: String,driverUid:String){
        val intent = Intent(this, RatingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(TITLE,title)
        intent.putExtra(DRIVERUID,driverUid)
        val pendingIntent = PendingIntent.getActivity(this, 6, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        com.pakdrive.service.notification.NotificationManager.showNotification(6, "Pak Drive ride completed", pendingIntent, this, title, messageBody)
    }

    private fun showPickUpPointNotification(title: String,messageBody: String){
        val intent = Intent(this, LiveDriverViewActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(TITLE,title)
        val pendingIntent = PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        com.pakdrive.service.notification.NotificationManager.showNotification(7, "Pak Drive pick up point", pendingIntent, this, title, messageBody)
    }


}
