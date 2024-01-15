package com.pakdrive.ui.activities

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
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
import com.pakdrive.MapUtils.routingSuccess
import com.pakdrive.MyConstants.apiKey
import com.pakdrive.R
import com.pakdrive.Utils.isLocationPermissionGranted
import com.pakdrive.Utils.requestLocationPermission
import com.pakdrive.databinding.ActivityLiveDriverViewBinding
import com.pakdrive.ui.viewmodels.CustomerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class LiveDriverViewActivity : AppCompatActivity(), OnMapReadyCallback {
    lateinit var binding:ActivityLiveDriverViewBinding
    private lateinit var onGoogleMap: GoogleMap
    private lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var list:ArrayList<LatLng>
    val customerViewModel:CustomerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@LiveDriverViewActivity,R.layout.activity_live_driver_view)
        val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        myFragment.getMapAsync(this)
        list= ArrayList()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(this, getString(R.string.api));
        placesClient = Places.createClient(this)

        locationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(2000L).setFastestInterval(2000L).setMaxWaitTime(2000L)

        if (!isLocationPermissionGranted(this@LiveDriverViewActivity)) {
            requestLocationPermission(this@LiveDriverViewActivity)
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
    }

}