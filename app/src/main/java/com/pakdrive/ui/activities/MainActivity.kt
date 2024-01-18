package com.pakdrive.ui.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.directions.route.Route
import com.directions.route.RouteException
import com.directions.route.RoutingListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.Polyline
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.maps.model.TravelMode
import com.pakdrive.InternetChecker
import com.pakdrive.MapUtils.clearMapObjects
import com.pakdrive.MapUtils.removePreviousMarkers
import com.pakdrive.MapUtils.routingSuccess
import com.pakdrive.MapUtils.updateLocationUI
import com.pakdrive.MyConstants.CUSTOMER
import com.pakdrive.MyConstants.CUSTOMER_TOKEN_NODE
import com.pakdrive.MyConstants.apiKey
import com.pakdrive.PermissionHandler.Companion.askNotificationPermission
import com.pakdrive.PermissionHandler.Companion.permissionRequestCode
import com.pakdrive.PermissionHandler.Companion.showEnableGpsDialog
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.blinkAnimation
import com.pakdrive.Utils.generateFCMToken
import com.pakdrive.Utils.isLocationPermissionGranted
import com.pakdrive.Utils.myToast
import com.pakdrive.Utils.requestLocationPermission
import com.pakdrive.databinding.ActivityMainBinding
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.models.RequestModel
import com.pakdrive.service.notification.NotificationManager.Companion.sendNotification
import com.pakdrive.ui.fragments.CustomerBottomSheet
import com.pakdrive.ui.fragments.UserInputDetails
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnPolylineClickListener, GoogleMap.OnPolygonClickListener, GoogleApiClient.OnConnectionFailedListener, RoutingListener, UserInputDetails {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this@MainActivity, "Permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this@MainActivity, "permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val customerViewModel: CustomerViewModel by viewModels()

    lateinit var binding: ActivityMainBinding
    lateinit var onGoogleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    protected var start: LatLng? = null
    protected var end: LatLng? = null
    protected var priceRange: Int? = null
    protected var comment: String? = null

    lateinit var bottomSheetFragment: CustomerBottomSheet

    var st = ""
    var dt = ""
    var time = ""
    var distance:Double = 0.0

    private var currentCircle: Circle? = null
    private var markersList: MutableList<Marker> = mutableListOf()
    var listOfTokens=ArrayList<String>()
    lateinit var dialog: Dialog

    @Inject
    lateinit var auth:FirebaseAuth
    lateinit var currentUser:FirebaseUser
    lateinit var  locationManager:LocationManager
    var radius:Double=1000.0
    lateinit var model: CustomerModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        dialog=Utils.showProgressDialog(this,"Finding...")
        currentUser=auth.currentUser!!
        Utils.statusBarColor(this,R.color.tool_color)


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            askNotificationPermission(this,requestPermissionLauncher)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), permissionRequestCode)
            Utils.dismissProgressDialog(dialog)
        }

        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val myFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        myFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        Places.initialize(this, getString(R.string.api));

        lifecycleScope.launch {
            if (InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                generateFCMToken(CUSTOMER,CUSTOMER_TOKEN_NODE,auth)
            }
        }

        binding.sheetShow.setOnClickListener {
            bottomSheetFragment = CustomerBottomSheet()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        lifecycleScope.launch {
            val customerDiffered=async { customerViewModel.getUser(CUSTOMER,currentUser.uid) }
            model= customerDiffered.await()!!
        }


        binding.menuImage.setOnClickListener {
            binding.blinkAnim.visibility=View.GONE
            if (binding.drawer.isDrawerOpen(GravityCompat.START)) {
                binding.drawer.closeDrawer(GravityCompat.START)
            } else {
                binding.drawer.openDrawer(GravityCompat.START)
            }
        }


        lifecycleScope.launch { // drawer item
            customerViewModel.receivedOffers().collect{offers->
                binding.numberOfRequests.text=offers.size.toString()
                if (offers.size!=0){
                    binding.blinkAnim.visibility=View.VISIBLE
                    binding.numberOfRequests.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.offerTextColor))
                    binding.numberOfRequests.startAnimation(blinkAnimation(this@MainActivity))
                }else{
                    binding.blinkAnim.visibility=View.GONE
                    binding.numberOfRequests.clearAnimation()
                }
            }
        }

        binding.rideRequestLinear.setOnClickListener {// drawer item
            binding.numberOfRequests.clearAnimation()
            binding.blinkAnim.visibility=View.GONE
            startActivity(Intent(this@MainActivity,DriversOfferActivity::class.java))
        }

        binding.liveDriveLinear.setOnClickListener{
            startActivity(Intent(this@MainActivity,LiveDriverViewActivity::class.java))
        }

        binding.clearRouteBtn.setOnClickListener {
            if (::onGoogleMap.isInitialized){
                clearMapObjects()
                removePreviousMarkers(markersList)
                currentCircle?.remove()
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        lifecycleScope.launch {
            var internetChecker=async { InternetChecker().isInternetConnectedWithPackage(this@MainActivity) }
            if (internetChecker.await()){
                onGoogleMap = googleMap
//                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@MainActivity, R.raw.map_style))

                onGoogleMap.uiSettings.apply {
                    isMapToolbarEnabled = false
                    isMyLocationButtonEnabled = true
                    isRotateGesturesEnabled = false
                    isCompassEnabled = false
                }

                lifecycleScope.launch {
                    if (isLocationPermissionGranted(this@MainActivity)&&locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)&&InternetChecker().isInternetConnectedWithPackage(this@MainActivity)&&::fusedLocationClient.isInitialized) {
                        updateLocationUI(onGoogleMap,this@MainActivity,fusedLocationClient,dialog)
                    } else {
                        requestLocationPermission(this@MainActivity)
                    }
                }
                onGoogleMap.setOnCameraMoveStartedListener {
                    if (::bottomSheetFragment.isInitialized) {
                        bottomSheetFragment.dismiss()
                    }
                }
            }else{
                myToast(this@MainActivity,"Check your internet connection.")
            }
        }
    }

    fun showDriversOnMap(map: GoogleMap,start: LatLng,priceRange: Int,listOfDriver:ArrayList<DriverModel>) {
        try {
            lifecycleScope.launch {


                removePreviousMarkers(markersList)

                listOfTokens.clear()

                listOfDriver.forEach { it ->
                    val lat=it.lat
                    val lang=it.lang
                    val position = LatLng(lat!!, lang!!)
                    val markerOptions = MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                    val marker=onGoogleMap.addMarker(markerOptions)
                    markersList.add(marker?:Marker(null))
                    listOfTokens.add(it.driverFCMToken)
                    Log.i("drivers", "it.lat -> ${it.lat}, it.lang->${it.lang}, position->$position")
                }
                val km=radius/1000.0
                sendNotification(listOfTokens,model,comment?:"",time,distance.toString(),priceRange.toString())
                Toast.makeText(this@MainActivity, "Rides in $km KM Radius", Toast.LENGTH_LONG).show()
                delay(4000)
                startActivity(Intent(this@MainActivity,DriversOfferActivity::class.java))

            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onPolylineClick(polyline: Polyline) {
        TODO("Not yet implemented")
    }

    override fun onPolygonClick(p0: Polygon) {
        TODO("Not yet implemented")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Toast.makeText(this, "connection Failed", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingStart() {
        Toast.makeText(this, "Finding best route...", Toast.LENGTH_SHORT).show()
    }

    override fun onRoutingSuccess(route: ArrayList<Route>?, shortestRouteIndex: Int) {
        onGoogleMap.isTrafficEnabled = true
        if (start != null && end != null) {
            routingSuccess(route!!, shortestRouteIndex, this, onGoogleMap, st, dt, R.color.yellow)
            Toast.makeText(this, "Your Shortest Route.", Toast.LENGTH_LONG).show()

            onGoogleMap.apply {
                currentCircle?.remove()
                currentCircle = addCircle(CircleOptions().center(start!!).radius(radius).strokeColor(Color.BLACK).strokeWidth(5f))
            }

            onGoogleMap.apply {
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(start!!, 12f)
                animateCamera(cameraUpdate)
            }


        } else {
            Toast.makeText(this, "Enter Starting and Ending Point.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRoutingCancelled() {
        customerViewModel.findingRoute(start, end, this, this, TravelMode.DRIVING)
    }

    override fun onRoutingFailure(p0: RouteException?) {
        customerViewModel.findingRoute(start, end, this, this, TravelMode.DRIVING)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun userInputDetails(start: LatLng, end: LatLng, st: String, dt: String, priceRange:Int, comment: String) {
        this.start = start
        this.end = end
        this.st = st
        this.dt = dt
        this.priceRange = priceRange
        this.comment = comment

        if (this.start != null && this.end != null && this.st.isNotEmpty() && this.dt.isNotEmpty() && this.priceRange!=null) {

            if (::onGoogleMap.isInitialized) {

                customerViewModel.findingRoute(start, end, this@MainActivity, this, TravelMode.DRIVING)

                lifecycleScope.launch {

                    val timeDiffered=async(Dispatchers.IO) {
                        customerViewModel.calculateEstimatedTimeForRoute(start,end, apiKey, TravelMode.DRIVING)?:"none"
                    }

                    val distanceDiffered=async(Dispatchers.IO) {
                        customerViewModel.calculateDistanceForRoute(start,end,apiKey,
                            TravelMode.DRIVING)?:0.0
                    }
                    time=timeDiffered.await()
                    distance=distanceDiffered.await()

                    var listOfDrivers=async {customerViewModel.getDriversInRadius(start,radius)  }.await()

                    if (listOfDrivers.isEmpty()){
                        myToast(this@MainActivity,"Did not find any drivers. Please consider changing your location.",Toast.LENGTH_LONG)
                    }else{
                        showDriversOnMap(onGoogleMap, start, priceRange, listOfDrivers)

                        GlobalScope.launch(Dispatchers.IO) {
                            // request model.
                            var model=RequestModel(customerUid = currentUser!!.uid, far = priceRange.toString(),pickUpLatLang=start.toString(),destinationLatLang=end.toString(),comment=comment, timeTaken = time, distance = distance.toString())
                            listOfDrivers.forEach {
                                customerViewModel.uploadRequestModel(model,it.uid!!)
                            }
                            customerViewModel.updateCustomerDetails(start.toString(),end.toString(),st,dt)
                        }
                    }
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch{
            launch {
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    showEnableGpsDialog(this@MainActivity)
                    Utils.dismissProgressDialog(dialog)
                }else if (!InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                    startActivity(intent)
                    myToast(this@MainActivity, "on your internet connection.", Toast.LENGTH_LONG)
                }else if (!isLocationPermissionGranted(this@MainActivity)){
                    requestLocationPermission(this@MainActivity)
                } else if (isLocationPermissionGranted(this@MainActivity)&&locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)&&InternetChecker().isInternetConnectedWithPackage(this@MainActivity)){
                    askNotificationPermission(this@MainActivity,requestPermissionLauncher)
                    if (::onGoogleMap.isInitialized&&::fusedLocationClient.isInitialized){
//                        updateLocationUI(onGoogleMap,this@MainActivity,fusedLocationClient,dialog)
                        generateFCMToken(CUSTOMER,CUSTOMER_TOKEN_NODE,auth)
                    }
                }
            }
        }
    }


}