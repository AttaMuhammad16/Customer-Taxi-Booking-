package com.pakdrive.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.pakdrive.R
import com.pakdrive.models.CustomerModel
import com.pakdrive.service.SendNotification.sendNotifyFromCustomer

class NotificationManager {

    companion object{
        fun showNotification(id:Int , channelId:String , pendingIntent: PendingIntent?, context: Context , title:String , message:String){
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.app_ic)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId, "Pak drive notification", NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }
            notificationManager.notify(id, notificationBuilder.build())
        }
        suspend fun sendNotification(listOfTokens:ArrayList<String>, userModel:CustomerModel, comment:String, time:String, distance:String, priceRange:String){
            val notificationTitle = "PakDrive Ride Notification"
            val notificationMessage = buildString {
                append("Price: $priceRange")
                appendLine()
                if (comment.isNotEmpty()){
                    append("Comment: $comment")
                }
                appendLine()
                append("Time: $time")
                appendLine()
                append("Distance: $distance KM")
            }
            sendNotifyFromCustomer(notificationTitle, notificationMessage,listOfTokens,userModel.uid!!,comment!!,time,distance,priceRange)
        }
    }


}