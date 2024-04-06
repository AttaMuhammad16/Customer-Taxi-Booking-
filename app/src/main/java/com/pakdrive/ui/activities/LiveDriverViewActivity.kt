package com.pakdrive.ui.activities

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.model.TravelMode
import com.pakdrive.DialogeInterface
import com.pakdrive.InternetChecker
import com.pakdrive.MyConstants
import com.pakdrive.MyConstants.CUSTOMER
import com.pakdrive.MyConstants.DRIVERUID
import com.pakdrive.MyConstants.apiKey
import com.pakdrive.PreferencesManager
import com.pakdrive.R
import com.pakdrive.RateUsDialogue
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.getCurrentFormattedDate
import com.pakdrive.Utils.isLocationPermissionGranted
import com.pakdrive.Utils.requestLocationPermission
import com.pakdrive.Utils.setUpNavigationColor
import com.pakdrive.Utils.showAlertDialog
import com.pakdrive.Utils.showProgressDialog
import com.pakdrive.Utils.stringToLatLng
import com.pakdrive.databinding.ActivityLiveDriverViewBinding
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.RideHistoryModel
import com.pakdrive.service.SendNotification
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.ArrayList
import javax.inject.Inject

@AndroidEntryPoint
class LiveDriverViewActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var binding:ActivityLiveDriverViewBinding
    private lateinit var onGoogleMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var list:ArrayList<LatLng>
    val customerViewModel:CustomerViewModel by viewModels()
    lateinit var dialog:Dialog
    var carInfo=""
    @Inject
    lateinit var auth:FirebaseAuth
    var driverToken=""
    var startLatLang:LatLng?=null
    var endLatLang:LatLng?=null
    private var hasElseBlockExecuted = false
    private lateinit var customerModel:CustomerModel

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@LiveDriverViewActivity,R.layout.activity_live_driver_view)
        Utils.statusBarColor(this@LiveDriverViewActivity)
        setUpNavigationColor()
        val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        myFragment.getMapAsync(this)
        list= ArrayList()
        dialog= showProgressDialog(this@LiveDriverViewActivity,"Loading...")

        lifecycleScope.launch {
            customerModel=customerViewModel.getUser(CUSTOMER,auth.uid!!)!!
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(this, getString(R.string.api));
        placesClient = Places.createClient(this)

        locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000L).setFastestInterval(2000L).setMaxWaitTime(2000L)

        if (!isLocationPermissionGranted(this@LiveDriverViewActivity)) {
            requestLocationPermission(this@LiveDriverViewActivity)
        }


        customerViewModel.apply {
            driverNumber.observe(this@LiveDriverViewActivity){

                if (it.isNotEmpty()){
                    binding.dialImg.setOnClickListener {
                        if (ContextCompat.checkSelfPermission(this@LiveDriverViewActivity, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@LiveDriverViewActivity, arrayOf(android.Manifest.permission.CALL_PHONE), 123)
                        }else{
                            val intent = Intent(Intent.ACTION_CALL);
                            intent.data = Uri.parse("tel:${it}")
                            startActivity(intent)
                        }
                    }
                }
            }

            driverName.observe(this@LiveDriverViewActivity){
                if (it.isNotEmpty() ){
                    binding.driverNameTv.text=it
                }
            }

            rating.observe(this@LiveDriverViewActivity){
                if (it.isNotEmpty()){
                    binding.ratingTv.text=it
                }
            }

            driverProfileUrl.observe(this@LiveDriverViewActivity){
                if (it.isNotEmpty()){
                    Glide.with(this@LiveDriverViewActivity).load(it).placeholder(R.drawable.person_with_out_circle).into(binding.driverImage)
                }
            }

            carDetails.observe(this@LiveDriverViewActivity){
                if (it.isNotEmpty()){
                    carInfo=it
                }
            }

            far.observe(this@LiveDriverViewActivity){
                if (it.isNotEmpty()){
                    binding.farTv.text="Far: $it PKR"
                }
            }

            lifecycleScope.launch {
                if (auth.currentUser!=null){
                    val customer=getUser(MyConstants.CUSTOMER,auth.currentUser!!.uid)
                    if (customer?.startLatLang!="" && customer?.endLatLang!=""){
                        startLatLang= stringToLatLng(customer!!.startLatLang)
                        endLatLang= stringToLatLng(customer.endLatLang)
                        val distance=calculateDistanceForRoute(startLatLang!!,endLatLang!!, apiKey,TravelMode.DRIVING)
                        binding.distanceTv.text="($distance km)"
                    }
                }else{
                    binding.distanceTv.text="(0 km)"
                }
            }
        }

        binding.cancelBtn.setOnClickListener { // cancel the ride.
            showAlertDialog(this@LiveDriverViewActivity,object:DialogeInterface{
                override fun clickedBol(bol: Boolean) {
                    if (bol&&driverToken.isNotEmpty()){
                        val dialog=showProgressDialog(this@LiveDriverViewActivity,"Cancelling...")
                        lifecycleScope.launch{
                            var far=""
                            customerViewModel.far.observe(this@LiveDriverViewActivity){far=it}
                            val date=getCurrentFormattedDate()
                            async { customerViewModel.rideHistory(RideHistoryModel(date,customerModel.pickUpPointName,customerModel.destinationName,false,far,"","")) }.await()

                            val result=customerViewModel.updateCustomerLatLang()
                            Utils.resultChecker(result,this@LiveDriverViewActivity)

                            val driverUid=PreferencesManager(this@LiveDriverViewActivity).getValue(DRIVERUID,"empty")
                            customerViewModel.updateDriverAvailableNode(false,driverUid) // update driver available node
                            async { customerViewModel.deleteAcceptModel(driverUid) }.await() // delete accept model

                            SendNotification.sendCancellationNotification("Pak Drive", "Ride cancellation notification.Ride has been canceled by the customer.", driverToken, "false")
                            PreferencesManager(this@LiveDriverViewActivity).deleteValue(DRIVERUID)
                            dialog.dismiss()

                        }
                    }
                }
            },"Do you want to cancel this ride?")
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        onGoogleMap=googleMap

        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        onGoogleMap.uiSettings.apply {
            isMapToolbarEnabled=false
            isMyLocationButtonEnabled = true
            isCompassEnabled=false
            isZoomControlsEnabled=true
        }

        onGoogleMap.isTrafficEnabled=true
        var uid=PreferencesManager(this@LiveDriverViewActivity).getValue(DRIVERUID,"empty")

        lifecycleScope.launch{
            if (uid!="empty"){
                customerViewModel.gettingDriverLatLang(uid).collect{
                    if (it != null) {
                        if (it.availabe){
                            driverToken=it.driverFCMToken
                            val internetChecker=InternetChecker().isInternetConnectedWithPackage(this@LiveDriverViewActivity)
                            if (internetChecker){
                                val driverLocation=Location("").apply {
                                    latitude=it.lat!!
                                    longitude=it.lang!!
                                }
                                customerViewModel.setUserLocationMarker(driverLocation,googleMap,this@LiveDriverViewActivity,R.drawable.car,it.bearing,it.carDetails,it.userName)
                                dismissProgressDialog(dialog)
                            }
                        }else{
                            if (!hasElseBlockExecuted){
                                binding.blankTv.visibility= View.VISIBLE
                                binding.mapFragment.visibility= View.GONE
                                binding.constraintLayout.visibility= View.GONE
                                binding.cardView.visibility= View.GONE
                                PreferencesManager(this@LiveDriverViewActivity).deleteValue(DRIVERUID)
                                uid="empty"
                                dismissProgressDialog(dialog)
                                hasElseBlockExecuted=true
                                RateUsDialogue(this@LiveDriverViewActivity,it.profileImageUrl,it.uid!!,customerViewModel,it.totalRating,it.totalPersonRatings).show()
                            }
                        }
                    }else{
                        binding.blankTv.visibility= View.VISIBLE
                        binding.mapFragment.visibility= View.GONE
                        binding.constraintLayout.visibility= View.GONE
                        binding.cardView.visibility= View.GONE
                        PreferencesManager(this@LiveDriverViewActivity).deleteValue(DRIVERUID)
                        uid="empty"
                        dismissProgressDialog(dialog)
                    }
                }

            }else{
                binding.blankTv.visibility= View.VISIBLE
                binding.mapFragment.visibility= View.GONE
                binding.constraintLayout.visibility= View.GONE
                binding.cardView.visibility= View.GONE
                dismissProgressDialog(dialog)
            }
        }
    }

}