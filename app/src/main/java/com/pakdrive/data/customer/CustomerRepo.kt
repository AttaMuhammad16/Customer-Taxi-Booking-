package com.pakdrive.data.customer

import android.app.Activity
import android.graphics.Bitmap
import com.directions.route.RoutingListener
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.RequestModel

interface CustomerRepo {
    suspend fun uploadImageToFirebaseStorage(uri: String):MyResult
    suspend fun uploadImageToFirebaseStorage(bitmap: Bitmap):MyResult
    suspend fun deleteImageToFirebaseStorage(url: String):MyResult
    suspend fun uploadUserOnDatabase(customerModel: CustomerModel):MyResult

    fun findRoutes(Start: LatLng?, End: LatLng?, context: Activity, routingListener: RoutingListener, travelMode: TravelMode = TravelMode.DRIVING)
    suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode = TravelMode.DRIVING): String?
    suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double?
    suspend fun readCustomer(): CustomerModel?
    suspend fun driversInRadius(startLatLang: LatLng, radius: Double): ArrayList<DriverModel>
    suspend fun uploadRequestModel(requestModel:RequestModel)
    suspend fun updateCustomerStartEndLatLang(startLatLang:String,endLatLang:String)



}