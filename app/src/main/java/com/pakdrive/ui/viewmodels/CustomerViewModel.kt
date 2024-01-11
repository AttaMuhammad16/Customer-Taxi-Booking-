package com.pakdrive.ui.viewmodels

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.directions.route.RoutingListener
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.TravelMode
import com.pakdrive.MyResult
import com.pakdrive.data.customer.CustomerRepo
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.RequestModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@HiltViewModel
class CustomerViewModel @Inject constructor(val customerRepo: CustomerRepo):ViewModel() {

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
    suspend fun getCustomer():CustomerModel?{
        return customerRepo.readCustomer()
    }

    suspend fun  getDriversInRadius(startLatLang:LatLng,radius:Double):ArrayList<DriverModel>{
        return customerRepo.driversInRadius(startLatLang,radius)
    }

    suspend fun uploadRequestModel(requestModel: RequestModel){
        customerRepo.uploadRequestModel(requestModel)
        Log.i("TAG", "uploadRequestModel:called")
    }

    fun updateCustomerStartEndLatLang(startLatLang:String,endLatLang:String){
        viewModelScope.launch(Dispatchers.IO) {
            customerRepo.updateCustomerStartEndLatLang(startLatLang, endLatLang)
        }
    }

}