package com.pakdrive.ui.viewmodels

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directions.route.RoutingListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.data.customer.CustomerRepo
import com.pakdrive.models.AcceptModel
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.OfferModel
import com.pakdrive.models.RequestModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@HiltViewModel
class CustomerViewModel @Inject constructor(val customerRepo: CustomerRepo):ViewModel() {
    private var userLocationMarker: Marker? = null

    private var _driverName:MutableLiveData<String> = MutableLiveData("")
    val driverName:LiveData<String> = _driverName

    private var _rating:MutableLiveData<String> = MutableLiveData("")
    val rating:LiveData<String> = _rating

    private var _driverNumber:MutableLiveData<String> = MutableLiveData("")
    val driverNumber:LiveData<String> = _driverNumber

    private var _driverProfileUrl:MutableLiveData<String> = MutableLiveData("")
    val driverProfileUrl:LiveData<String> = _driverProfileUrl

    private var _carDetails:MutableLiveData<String> = MutableLiveData("")
    val carDetails:LiveData<String> = _carDetails

    suspend fun uploadImageToStorage(bitmap: Bitmap):MyResult{
        return try {
           customerRepo.uploadImageToFirebaseStorage(bitmap)
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    suspend fun uploadImageToStorage(uri: String):MyResult{
        return try {
            customerRepo.uploadImageToFirebaseStorage(uri)
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    suspend fun deleteImageFromStorage(url:String):MyResult{
        return try {
            customerRepo.deleteImageToFirebaseStorage(url)
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }


    suspend fun uploadUserOnDatabase(customerModel: CustomerModel): MyResult {
        return try {
            suspendCoroutine { continuation ->
                viewModelScope.launch(Dispatchers.IO) {
                    val result = customerRepo.uploadUserOnDatabase(customerModel)
                    continuation.resume(result)
                }
            }
        } catch (e: Exception) {
            MyResult.Error(e.message.toString())
        }
    }

    // maps

    fun findingRoute(Start: LatLng?, End: LatLng?, context: Activity, routingListener: RoutingListener, travelMode: TravelMode = TravelMode.DRIVING){
        customerRepo.findRoutes(Start,End, context, routingListener, travelMode)
    }

    suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): String?{
        return customerRepo.calculateEstimatedTimeForRoute(start, end, apiKey, travelMode)
    }

    suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double?{
        return customerRepo.calculateDistanceForRoute(start, end, apiKey, travelMode)
    }


    // read
    suspend fun getUser(role:String,uid:String):CustomerModel?{
        return customerRepo.readUser(role, uid)
    }

    suspend fun  getDriversInRadius(startLatLang:LatLng,radius:Double):ArrayList<DriverModel>{
        return customerRepo.driversInRadius(startLatLang,radius)
    }

    suspend fun uploadRequestModel(requestModel: RequestModel,driverUid:String){
        customerRepo.uploadRequestModel(requestModel,driverUid)
    }

    fun updateCustomerDetails(startLatLang:String,endLatLang:String,pickPointName: String,destinationName:String){
        viewModelScope.launch(Dispatchers.IO) {
            customerRepo.updateCustomerDetails(startLatLang, endLatLang,pickPointName,destinationName)
        }
    }

    suspend fun receivedOffers():Flow<ArrayList<OfferModel>>{
        return customerRepo.receiveOffers()
    }

    suspend fun readingDriver(uid: String):DriverModel?{
        return customerRepo.readingDriver(uid)
    }

    suspend fun deleteOffer(driverUid: String):MyResult{
        return customerRepo.deleteOffer(driverUid)
    }
    suspend fun deleteRequest(driverUid: String):MyResult{
        return customerRepo.deleteRequest(driverUid)
    }

    suspend fun uploadAcceptModel(acceptModel: AcceptModel):MyResult{
        return customerRepo.uploadAcceptModel(acceptModel)
    }

    suspend fun deleteAcceptModel(driverUid: String):MyResult{
        return withContext(Dispatchers.IO){customerRepo.deleteAcceptModel(driverUid)}
    }

    fun gettingDriverLatLang(driverUid: String): Flow<DriverModel?>{
        val driverModelFlow=customerRepo.gettingDriverLatLang(driverUid)
        viewModelScope.launch {
            driverModelFlow.collect{
                _driverName.value=it?.userName
                _driverNumber.value=it?.phoneNumber
                _rating.value= (it?.totalRating.toString()+"("+it?.totalPersonRatings.toString()+ ")")
                _driverProfileUrl.value= it?.profileImageUrl
                _carDetails.value= it?.carDetails
            }
        }
        return driverModelFlow
    }



    fun getScaledCarIcon(zoomLevel: Float, context: Activity, drawable: Int): BitmapDescriptor {
        val scaleFactor = 1.0f + (zoomLevel - 18f) / 10.0f
        if (scaleFactor == 1.0f) {
            return BitmapDescriptorFactory.fromResource(drawable)
        }
        val originalIcon = BitmapFactory.decodeResource(context.resources, drawable)
        val scaledWidth = (originalIcon.width * scaleFactor).toInt()
        val scaledHeight = (originalIcon.height * scaleFactor).toInt()
        if (scaledWidth > 0 && scaledHeight > 0) {
            val scaledBitmap = Bitmap.createScaledBitmap(originalIcon, scaledWidth, scaledHeight, true)
            return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
        }
        return BitmapDescriptorFactory.fromResource(drawable)
    }

    fun setUserLocationMarker(location: Location, mMap: GoogleMap, context: Activity, drawable: Int,bearing:Float) {
        val latLng = LatLng(location.latitude, location.longitude)
        var lastZoomLevel = mMap.cameraPosition.zoom
        var lastScaledIcon: BitmapDescriptor? = null

        val updateMarkerIcon = {
            val currentZoomLevel = mMap.cameraPosition.zoom
            if (lastZoomLevel != currentZoomLevel || lastScaledIcon == null) {
                lastScaledIcon = getScaledCarIcon(currentZoomLevel, context, drawable)
                lastZoomLevel = currentZoomLevel
            }
            userLocationMarker?.setIcon(lastScaledIcon)
        }

        if (userLocationMarker == null) {
            val markerOptions = MarkerOptions().position(latLng).icon(getScaledCarIcon(mMap.cameraPosition.zoom, context, drawable)).rotation(location.bearing).anchor(0.5f, 0.5f)
            userLocationMarker = mMap.addMarker(markerOptions)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        } else {
            userLocationMarker?.position = latLng
            userLocationMarker?.rotation = bearing
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
        }

        mMap.setOnCameraIdleListener {
            updateMarkerIcon()
        }
    }

}