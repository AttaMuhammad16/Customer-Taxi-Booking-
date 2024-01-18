package com.pakdrive.service

import android.util.Log
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object SendNotification {

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

            val requestBody = RequestBody.create(mediaType, wholeObj.toString())
            val request = Request.Builder()
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .addHeader("Authorization", "key=AAAAx5Jyo0U:APA91bEB1Z9IYIqrN7Tt6avCLOTcto6sLJurSg_JrFCEteF8LS4QKqrB_wMsuh1ZFDiUAlw2rnAS94QHonUtw9j_s5ayfsjFgCmv1xU4I7toSlzB82_mquaMT8M-Fdh20jnw2r0HANO3")
                .addHeader("Content-type", "application/json")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    val responseJson = JSONObject(responseBody ?: "{}")
                    val success = responseJson.optInt("success", 0)
                    if (success == 1) {
                        Log.i("Notify", "Notification sent successfully to $token")
                    } else {
                        Log.i("Notify", "Failed to send notification. Response: $responseBody")
                    }
                }
            })
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

        val requestBody = RequestBody.create(mediaType, wholeObj.toString())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(requestBody)
            .addHeader("Authorization", "key=AAAAx5Jyo0U:APA91bEB1Z9IYIqrN7Tt6avCLOTcto6sLJurSg_JrFCEteF8LS4QKqrB_wMsuh1ZFDiUAlw2rnAS94QHonUtw9j_s5ayfsjFgCmv1xU4I7toSlzB82_mquaMT8M-Fdh20jnw2r0HANO3")
            .addHeader("Content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody ?: "{}")
                val success = responseJson.optInt("success", 0)
                if (success == 1) {
                    Log.i("TAG", "Notification sent successfully to $driverToken")
                } else {
                    Log.e("TAG", "Failed to send notification. Response: $responseBody")
                }
            }
        })
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

        val requestBody = RequestBody.create(mediaType, wholeObj.toString())
        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(requestBody)
            .addHeader("Authorization", "key=AAAAx5Jyo0U:APA91bEB1Z9IYIqrN7Tt6avCLOTcto6sLJurSg_JrFCEteF8LS4QKqrB_wMsuh1ZFDiUAlw2rnAS94QHonUtw9j_s5ayfsjFgCmv1xU4I7toSlzB82_mquaMT8M-Fdh20jnw2r0HANO3")
            .addHeader("Content-type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                TODO("Not yet implemented")
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val responseJson = JSONObject(responseBody ?: "{}")
                val success = responseJson.optInt("success", 0)
                if (success == 1) {
                    Log.i("TAG", "Notification sent successfully to $driverToken")
                } else {
                    Log.e("TAG", "Failed to send notification. Response: $responseBody")
                }
            }
        })
    }

}