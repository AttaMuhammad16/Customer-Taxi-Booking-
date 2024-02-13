package com.pakdrive.data.customer

import android.app.Activity
import android.graphics.Bitmap
import com.directions.route.RoutingListener
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.models.AcceptModel
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.MessageModel
import com.pakdrive.models.OfferModel
import com.pakdrive.models.RequestModel
import com.pakdrive.models.RideHistoryModel
import kotlinx.coroutines.flow.Flow

interface CustomerRepo {

    suspend fun uploadImageToFirebaseStorage(uri: String):MyResult
    suspend fun uploadImageToFirebaseStorage(bitmap: Bitmap):MyResult
    suspend fun deleteImageToFirebaseStorage(url: String):MyResult
    suspend fun uploadUserOnDatabase(customerModel: CustomerModel):MyResult


    fun findRoutes(Start: LatLng?, End: LatLng?, context: Activity, routingListener: RoutingListener, travelMode: TravelMode = TravelMode.DRIVING)
    suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode = TravelMode.DRIVING): String?
    suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double?
    suspend fun readUser(role:String,uid:String): CustomerModel?
    suspend fun driversInRadius(startLatLang: LatLng, radius: Double): ArrayList<DriverModel>
    suspend fun uploadRequestModel(requestModel:RequestModel,driverUid:String)
    suspend fun updateCustomerDetails(startLatLang:String,endLatLang:String,pickUpPointName:String,destinationName:String)
    fun receiveOffers():Flow<ArrayList<OfferModel>>
    suspend fun readingDriver(uid: String):DriverModel?

    suspend fun deleteOffer(driverUid:String):MyResult
    suspend fun deleteRequest(driverUid:String):MyResult

    suspend fun uploadAcceptModel(acceptModel: AcceptModel):MyResult

    suspend fun deleteAcceptModel(driverUid: String):MyResult

    fun gettingDriverLatLang(driverUid: String):Flow<DriverModel?>
    suspend fun deleteAllOffers()
    suspend fun updateDriverAvailableNode(available:Boolean,driverUid: String)

    suspend fun updateCustomerLatLang():MyResult

    suspend fun deleteRideRequestFromDriver(driverUid: String)
    suspend fun ratingToTheDriver(driverUid: String,rating:Float,currentRating:Float,totalPersonRating:Long,callBack:(MyResult)->Unit)
    suspend fun rideHistory(rideHistoryModel: RideHistoryModel)
    suspend fun getRideHistory():ArrayList<RideHistoryModel>?

    suspend fun updateCustomerDetails(name:String?,number:String?,address:String?)
    suspend fun uploadMessage(messageModel: MessageModel):MyResult
    suspend fun getAdminFCM():String

}