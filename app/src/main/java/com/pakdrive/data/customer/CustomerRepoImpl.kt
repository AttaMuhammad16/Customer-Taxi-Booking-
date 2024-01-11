package com.pakdrive.data.customer

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import com.directions.route.AbstractRouting
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import com.pakdrive.MapUtils.calculateDistance
import com.pakdrive.MapUtils.mapTravelModeToAbstractRouting
import com.pakdrive.MyResult
import com.pakdrive.Utils
import com.pakdrive.Utils.CUSTOMER
import com.pakdrive.Utils.apiKey
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.RequestModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class CustomerRepoImpl @Inject constructor(val auth:FirebaseAuth,val storageReference: StorageReference,val databaseReference: DatabaseReference):CustomerRepo {

     override suspend fun uploadImageToFirebaseStorage(uri: String): MyResult {
        return try {
            val imageRef = storageReference.child("images/${System.currentTimeMillis()}.jpg")
            val uploadTask = imageRef.putFile(Uri.parse(uri))
            val result: UploadTask.TaskSnapshot = uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            MyResult.Success(downloadUrl.toString())
        } catch (e: Exception) {
            MyResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun uploadImageToFirebaseStorage(bitmap: Bitmap): MyResult {
        return try {
            val byteArray = Utils.bitmapToByteArray(bitmap)
            val imageRef = storageReference.child("images/${System.currentTimeMillis()}.jpg")
            val uploadTask = imageRef.putBytes(byteArray)
            val result= uploadTask.await()
            val downloadUrl = result.storage.downloadUrl.await()
            MyResult.Success(downloadUrl.toString())
        } catch (e: Exception) {
            MyResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun deleteImageToFirebaseStorage(url: String): MyResult {
        return try {
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.getReferenceFromUrl(url)
            val deleteTask: Task<Void> = storageRef.delete()
            Tasks.await(deleteTask)
            MyResult.Success("Image deleted successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            MyResult.Error("Failed to delete image: ${e.message}")
        }
    }

    override suspend fun uploadUserOnDatabase(customerModel: CustomerModel): MyResult {
        var uid = auth.currentUser?.uid ?: "${System.currentTimeMillis()}"
        customerModel.uid = uid
        return try {
            databaseReference.child(Utils.CUSTOMER).child(uid).setValue(customerModel).await()
            MyResult.Success("Successfully Registered")
        } catch (e: Exception) {
            MyResult.Error("Failed: ${e.message}")
        }
    }






    // maps

    override fun findRoutes(Start: LatLng?, End: LatLng?, context: Activity, routingListener: RoutingListener, travelMode: TravelMode) {
        if (Start == null || End == null) {
            Toast.makeText(context, "Unable to get location", Toast.LENGTH_LONG).show()
        } else {
            val routing: Routing = Routing.Builder().travelMode(mapTravelModeToAbstractRouting(travelMode)).withListener(routingListener).waypoints(Start, End).key(apiKey).alternativeRoutes(true).build()
            routing.execute()
        }
    }


    override suspend fun calculateEstimatedTimeForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): String? {
        val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

        return try {
            val directionsResult = DirectionsApi.newRequest(geoApiContext).mode(travelMode).origin(com.google.maps.model.LatLng(start.latitude, start.longitude)).destination(com.google.maps.model.LatLng(end.latitude, end.longitude)).await()
            val route = directionsResult.routes[0]
            val leg = route.legs[0]
            val duration = leg.duration
            duration.humanReadable
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override suspend fun calculateDistanceForRoute(start: LatLng, end: LatLng, apiKey: String, travelMode: TravelMode): Double? {
        val geoApiContext = GeoApiContext.Builder().apiKey(apiKey).build()

        return try {
            val directionsResult = DirectionsApi.newRequest(geoApiContext)
                .mode(travelMode)
                .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
                .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))
                .await()

            val route = directionsResult.routes[0]
            val leg = route.legs[0]
            val distanceInMeters = leg.distance.inMeters
            distanceInMeters / 1000.0  // Convert meters to kilometers
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    override suspend fun readCustomer(): CustomerModel? {
        val uid = auth.currentUser?.uid
        return try {
            val dataSnapshot = databaseReference.child(CUSTOMER).child(uid!!).get().await()
            if (dataSnapshot.exists()) {
                val customerModel = dataSnapshot.getValue(CustomerModel::class.java)
                customerModel
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun driversInRadius(startLatLang: LatLng, radius: Double): ArrayList<DriverModel> {
        return suspendCoroutine { continuation ->
            databaseReference.child(Utils.DRIVER).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val driverModels = ArrayList<DriverModel>()
                    dataSnapshot.children.forEach { childSnapshot ->
                        val driverModel = childSnapshot.getValue(DriverModel::class.java)
                        if (driverModel != null && !driverModel.availabe) {
                            val driverLatLng = LatLng(driverModel.lat?:0.0, driverModel.lang?:0.0)
                            val distance = calculateDistance(startLatLang, driverLatLng)
                            if (distance <= radius) {
                                driverModels.add(driverModel)
                            }
                        }
                    }
                    continuation.resume(driverModels)
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    continuation.resume(ArrayList())
                }
            })
        }
    }

    override suspend fun uploadRequestModel(requestModel: RequestModel) {
        var currentUser=auth.currentUser
        if (currentUser!=null){
            databaseReference.child(Utils.REQUESTSNODE).child(currentUser.uid).setValue(requestModel)
        }
    }

    override suspend fun updateCustomerStartEndLatLang(startLatLang: String, endLatLang: String) {
        var currentUser=auth.currentUser
        if (currentUser!=null){
            var map=HashMap<String,Any>()
            map[Utils.CUSTOMERSTARTLATLANG]=startLatLang
            map[Utils.CUSTOMERENDLATLANG]=endLatLang
            databaseReference.child(CUSTOMER).child(currentUser.uid).updateChildren(map)
        }
    }


}