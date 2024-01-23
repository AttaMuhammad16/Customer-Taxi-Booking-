package com.pakdrive.data.customer

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import com.pakdrive.MyConstants.ACCEPTNODE
import com.pakdrive.MyConstants.CUSTOMER
import com.pakdrive.MyConstants.CUSTOMERENDLATLANG
import com.pakdrive.MyConstants.CUSTOMERRIDEHISTORY
import com.pakdrive.MyConstants.CUSTOMERSTARTLATLANG
import com.pakdrive.MyConstants.DESTINATIONNAME
import com.pakdrive.MyConstants.DRIVER
import com.pakdrive.MyConstants.DRIVERAVAILABLENODE
import com.pakdrive.MyConstants.OFFER
import com.pakdrive.MyConstants.PICKUPPOINTNAME
import com.pakdrive.MyConstants.RIDEREQUESTS
import com.pakdrive.MyConstants.TAOTALPERSONRATING
import com.pakdrive.MyConstants.TOTALRATING
import com.pakdrive.MyConstants.apiKey
import com.pakdrive.MyResult
import com.pakdrive.Utils
import com.pakdrive.models.AcceptModel
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.OfferModel
import com.pakdrive.models.RequestModel
import com.pakdrive.models.RideHistoryModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class CustomerRepoImpl @Inject constructor(val auth:FirebaseAuth,val storageReference: StorageReference,val databaseReference: DatabaseReference):CustomerRepo {
    val currentUser = auth.currentUser

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
        return if (currentUser!=null){
            customerModel.uid = currentUser.uid
            databaseReference.child(CUSTOMER).child(currentUser.uid).setValue(customerModel).await()
            MyResult.Success("Successfully Registered")
        }else{
            MyResult.Error("User not login")
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


    override suspend fun readUser(role:String,uid:String): CustomerModel? {
        return try {
            val dataSnapshot = databaseReference.child(role).child(uid).get().await()
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
            databaseReference.child(DRIVER).addListenerForSingleValueEvent(object : ValueEventListener {
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

    override suspend fun uploadRequestModel(requestModel: RequestModel,driverUid:String) {
        if (currentUser!=null){
            databaseReference.child(RIDEREQUESTS).child(driverUid).child(currentUser.uid).setValue(requestModel)
        }
    }

    override suspend fun updateCustomerDetails(startLatLang: String, endLatLang: String,pickUpPointName:String,destinationName:String) {
        if (currentUser!=null){
            val map=HashMap<String,Any>()
            map[CUSTOMERSTARTLATLANG]=startLatLang
            map[CUSTOMERENDLATLANG]=endLatLang
            map[PICKUPPOINTNAME]=pickUpPointName
            map[DESTINATIONNAME]=destinationName
            databaseReference.child(CUSTOMER).child(currentUser.uid).updateChildren(map)
        }
    }

    override fun receiveOffers(): Flow<ArrayList<OfferModel>> = callbackFlow {
        val offerReference = databaseReference.child(OFFER).child(currentUser?.uid ?: return@callbackFlow)

        val listener = offerReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listOfOffers = ArrayList<OfferModel>()
                snapshot.children.mapNotNullTo(listOfOffers) { it.getValue(OfferModel::class.java) }
                trySend(listOfOffers)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })
        awaitClose {
            offerReference.removeEventListener(listener)
        }
    }

    override suspend fun readingDriver(uid: String): DriverModel? {
        val snap= databaseReference.child(DRIVER).child(uid).get().await()
        val model=snap.getValue(DriverModel::class.java)
        if (model!=null){
            return model
        }else{
            return null
        }
    }

    override suspend fun deleteOffer(driverUid: String): MyResult {
        return if (currentUser != null) {
            try {
                databaseReference.child(OFFER).child(currentUser.uid).child(driverUid).removeValue().await()
                MyResult.Success("Deleted")
            } catch (e: Exception) {
                MyResult.Error(e.message ?: "An error occurred")
            }
        } else {
            MyResult.Error("User not logged in")
        }
    }

    override suspend fun deleteRequest(driverUid: String):MyResult {
        return if (currentUser!=null){
            databaseReference.child(RIDEREQUESTS).child(driverUid).child(currentUser.uid).removeValue().await()
            MyResult.Success("Deleted")
        }else {
            MyResult.Error("User not logged in")
        }
    }


    override suspend fun uploadAcceptModel(acceptModel: AcceptModel): MyResult {
       return if (currentUser!=null){
           acceptModel.customerUid=currentUser.uid
            databaseReference.child(ACCEPTNODE).child(acceptModel.driverUid).setValue(acceptModel).await()
            MyResult.Success("Your request has been sent to the driver. Please await confirmation from the driver.")
       }else{
           MyResult.Error("Something wrong or check internet connection.")
       }
    }

    override suspend fun deleteAcceptModel(driverUid: String):MyResult {
        databaseReference.child(ACCEPTNODE).child(driverUid).removeValue().await()
        return MyResult.Success("Ride cancelled")
    }

    override fun gettingDriverLatLang(driverUid: String): Flow<DriverModel?> {
        return callbackFlow {
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val driverModel = snapshot.getValue(DriverModel::class.java)
                    if (driverModel!=null){
                        trySend(driverModel).isSuccess
                    }else{
                        trySend(null)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    close(error.toException())
                }
            }
            databaseReference.child(DRIVER).child(driverUid).addValueEventListener(listener)
            awaitClose { databaseReference.child(driverUid).child(driverUid).removeEventListener(listener) }
        }
    }

    override suspend fun deleteAllOffers() {
        if (currentUser!=null){
            databaseReference.child(OFFER).child(currentUser.uid).removeValue().await()
        }
    }

    override suspend fun updateDriverAvailableNode(available: Boolean,driverUid: String) {
        if (currentUser!=null){
            var map:HashMap<String,Any> = hashMapOf()
            map[DRIVERAVAILABLENODE] = available
            databaseReference.child(DRIVER).child(driverUid).updateChildren(map).await()
        }
    }

    override suspend fun updateCustomerLatLang(): MyResult {
        return if (currentUser!=null){
            val map= hashMapOf<String,Any>()
            map[CUSTOMERSTARTLATLANG]= ""
            map[CUSTOMERENDLATLANG]= ""
            map[PICKUPPOINTNAME]= ""
            map[DESTINATIONNAME]= ""
            databaseReference.child(CUSTOMER).child(currentUser.uid).updateChildren(map).await()
            MyResult.Success("Ride Cancelled.")
        }else{
            MyResult.Error("User Not Registered.")
        }
    }

    override suspend fun deleteRideRequestFromDriver(driverUid: String) {
        if (currentUser!=null){
            databaseReference.child(RIDEREQUESTS).child(driverUid).removeValue().await()
        }
    }


    override suspend fun ratingToTheDriver(driverUid: String, rating: Float,currentRating:Float,totalPersonRating:Long,callBack:(MyResult)->Unit) {
        val totalPerson=totalPersonRating+1
        val newOverallRating: Float = if (totalPersonRating == 0L) {
            currentRating
        } else {
            ((currentRating * totalPersonRating) + rating) / (totalPersonRating + 1)
        }
        val map= hashMapOf<String,Any>()
        map[TAOTALPERSONRATING]=totalPerson
        val formattedRating = String.format("%.1f", newOverallRating).toFloat()
        map[TOTALRATING] = formattedRating
        databaseReference.child(DRIVER).child(driverUid).updateChildren(map).addOnSuccessListener {
            callBack(MyResult.Success("Done"))
        }.addOnFailureListener {
            callBack(MyResult.Success("Something wrong ${it.message}"))
        }
    }

    override suspend fun rideHistory(rideHistoryModel: RideHistoryModel) {
        if (auth.currentUser!=null){
            rideHistoryModel.uid=auth.uid!!
            databaseReference.push().key?.let {
                rideHistoryModel.dataBaseKey=it
                databaseReference.child(CUSTOMERRIDEHISTORY).child(auth.uid!!).child(it).setValue(rideHistoryModel).await()
            }
        }
    }

    override suspend fun getRideHistory(): ArrayList<RideHistoryModel>? {
        val list=ArrayList<RideHistoryModel>()
        list.clear()
        return try {
            if (auth.currentUser!=null){
                val snap=databaseReference.child(CUSTOMERRIDEHISTORY).child(auth.uid!!).get().await()
                if (snap.exists()){
                    for (i in snap.children){
                        list.add(i.getValue(RideHistoryModel::class.java)!!)
                    }
                }
            }
            list
        }catch (e:Exception){
            return null
        }

    }

}