package com.pakdrive.service

import android.util.Log
import android.widget.Toast
import com.pakdrive.MyConstants
import com.pakdrive.MyConstants.BODY
import com.pakdrive.MyConstants.CLICKACTION
import com.pakdrive.MyConstants.COMMENT
import com.pakdrive.MyConstants.CUSTOMERUID
import com.pakdrive.MyConstants.DISTANCE
import com.pakdrive.MyConstants.PRICERANGE
import com.pakdrive.MyConstants.TIME
import com.pakdrive.MyConstants.TITLE
import com.pakdrive.MyConstants.approvedConst
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object SendNotification {

    var key="key=AAAA7oDFO3c:APA91bGM6-AyNBT_j5fpZCJkTSu92ymRGkobdlbSpR7jB1AFPUSJoIn3ncinJVDi7h2bnvOe4rkBpC8T1aygPhFq2gM_ZnaCh8-PNtv3on00hq_p6BNxLDP13UisY-Oif47Z7TmT3j77"

    suspend fun sendNotifyFromCustomer(title: String, des: String,listOfTokens:ArrayList<String>,uid:String,comment:String,time:String,distance:String,priceRange:String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        listOfTokens.forEach { token ->
            val jsonNotif = JSONObject().apply {
                put(TITLE, title)
                put(BODY, des)
                put(CUSTOMERUID, uid)
                put(COMMENT, comment)
                put(TIME, time)
                put(DISTANCE, distance)
                put(PRICERANGE, priceRange)
                put(CLICKACTION, "target_1")
            }

            val jsonData = JSONObject().apply {
                put(TITLE, title)
                put(BODY, des)
                put(CUSTOMERUID, uid)
                put(COMMENT, comment)
                put(TIME, time)
                put(DISTANCE, distance)
                put(PRICERANGE, priceRange)
            }

//            val androidConfig = JSONObject().apply {
//                put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
//            }

            val wholeObj = JSONObject().apply {
                put("to", token)
                put("notification", jsonNotif)
                put("data", jsonData)
                put("priority", "high")
//                put("android", androidConfig)
//                put("collapse_key", "${System.currentTimeMillis()}") // (updated notification) Example collapse key, change "update" to a suitable key for your app

            }

            sendViaHttps(mediaType, wholeObj, client)
        }
    }


    suspend fun sendCancellationNotification(title: String, des: String,driverToken:String,approved:String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val androidConfig = JSONObject().apply {
            put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
        }

        val wholeObj = JSONObject().apply {
            put("to", driverToken)
            put("notification", jsonNotif)
            put("data", jsonData)
            put("priority", "high")
            put("android", androidConfig)
            put("collapse_key", "update") // (updated notification) Example collapse key, change "update" to a suitable key for your app
        }
        sendViaHttps(mediaType, wholeObj, client)


    }




    suspend fun sendApprovedNotification(title: String, des: String,driverToken:String,approved:String) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
            put(CLICKACTION, "liveViewDriver")
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(approvedConst, approved)
        }

        val androidConfig = JSONObject().apply {
            put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
        }

        val wholeObj = JSONObject().apply {
            put("to", driverToken)
            put("notification", jsonNotif)
            put("data", jsonData)
            put("priority", "high")
            put("android", androidConfig)
            put("collapse_key", "update") // (updated notification) Example collapse key, change "update" to a suitable key for your app
        }

        sendViaHttps(mediaType, wholeObj, client)
    }





    suspend fun sendNotificationToAdmin(title: String, des: String,adminFCM:String,) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()

        val jsonNotif = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(MyConstants.SUPPORT, MyConstants.SUPPORT)
            put(CLICKACTION, "message")
        }

        val jsonData = JSONObject().apply {
            put(TITLE, title)
            put(BODY, des)
            put(MyConstants.SUPPORT, MyConstants.SUPPORT)
        }

        val androidConfig = JSONObject().apply {
            put("ttl", "3600s")  // Time-to-live set to 1 hour(expire time)
        }

        val wholeObj = JSONObject().apply {
            put("to", adminFCM)
            put("notification", jsonNotif)
            put("data", jsonData)
            put("priority", "high")
            put("android", androidConfig)
            put("collapse_key", "update") // (updated notification) Example collapse key, change "update" to a suitable key for your app
        }

        sendViaHttps(mediaType, wholeObj, client)
    }


    fun sendViaHttps(mediaType:MediaType?,wholeObj:JSONObject,client: OkHttpClient){
        try {
            val requestBody = RequestBody.create(mediaType, wholeObj.toString())
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .addHeader("Authorization", key)
                .addHeader("Content-type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.i("TAG", "onFailure:${e.message}")
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string()
                        val responseJson = JSONObject(responseBody ?: "{}")
                        val success = responseJson.optInt("success", 0)
                        if (success == 1) {
                            Log.i("TAG", "Notification sent successfully to ")
                        } else {
                            Log.e("TAG", "Failed to send notification. Response: $responseBody")
                        }
                    }catch (e:Exception){
                        Log.i("TAG", "onResponse:${e.message}")
                    }

                }
            })
        }catch (e:Exception){
            Log.i("TAG", "sendCancellationNotification: ${e.message}")
        }
    }


}