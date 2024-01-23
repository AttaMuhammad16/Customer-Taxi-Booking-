package com.pakdrive.models

data class RideHistoryModel(
    var date:String="",
    var pickUpPoint:String="",
    var destinationPoint:String="",
    var rideStatus:Boolean=false,
    var payment:String="",
    var uid:String="",
    var dataBaseKey:String="",
)