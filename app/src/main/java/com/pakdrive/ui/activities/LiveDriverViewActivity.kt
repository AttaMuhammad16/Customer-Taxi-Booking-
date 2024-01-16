package com.pakdrive.ui.activities

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Util
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.RoutingListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.maps.model.TravelMode
import com.pakdrive.InternetChecker
import com.pakdrive.MapUtils.routingSuccess
import com.pakdrive.MyConstants
import com.pakdrive.MyConstants.apiKey
import com.pakdrive.PreferencesManager
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.isLocationPermissionGranted
import com.pakdrive.Utils.requestLocationPermission
import com.pakdrive.databinding.ActivityLiveDriverViewBinding
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@LiveDriverViewActivity,R.layout.activity_live_driver_view)
        Utils.statusBarColor(this@LiveDriverViewActivity)
        val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        myFragment.getMapAsync(this)
        list= ArrayList()
        dialog= Utils.showProgressDialog(this@LiveDriverViewActivity,"Loading...")

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
                if (it.isNotEmpty()){
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
                    binding.carDetailsTv.text=it
                }
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        onGoogleMap=googleMap
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))

        onGoogleMap.uiSettings.apply {
            isMapToolbarEnabled=false
            isMyLocationButtonEnabled = true
//            isRotateGesturesEnabled=false
            isCompassEnabled=false
            isZoomControlsEnabled=true
        }
        onGoogleMap.isTrafficEnabled=true

        val uid=PreferencesManager(this@LiveDriverViewActivity).getValue(MyConstants.DRIVERUID,"")

        lifecycleScope.launch{

            customerViewModel.gettingDriverLatLang(uid).collect{
                if (it!=null){
                    var internetChecker=InternetChecker().isInternetConnectedWithPackage(this@LiveDriverViewActivity)
                    if (internetChecker){

                        var driverLocation=Location("").apply {
                            latitude=it.lat!!
                            longitude=it.lang!!
                        }
                        customerViewModel.setUserLocationMarker(driverLocation,googleMap,this@LiveDriverViewActivity,R.drawable.car,it.bearing)
                        dismissProgressDialog(dialog)
                    }

                }else{
                    binding.blankTv.visibility= View.VISIBLE
                    binding.mapFragment.visibility= View.GONE
                }
            }
        }
    }

}