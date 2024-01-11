package com.pakdrive.models

data class RequestModel(
    val customerUid: String,
    val far:String,
    val pickUpLatLang:String,
    val destinationLatLang:String
)