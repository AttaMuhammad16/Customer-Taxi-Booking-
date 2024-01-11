package com.pakdrive.ui.fragments

import com.google.android.gms.maps.model.LatLng

interface UserInputDetails {
    fun userInputDetails(start:LatLng, end:LatLng,stText:String,dt:String,priceRange:Int,comment:String)
}