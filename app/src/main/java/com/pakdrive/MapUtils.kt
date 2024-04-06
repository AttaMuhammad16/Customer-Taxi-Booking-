package com.pakdrive


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.directions.route.AbstractRouting
import com.directions.route.AbstractRouting.TravelMode
import com.directions.route.Route
import com.directions.route.Routing
import com.directions.route.RoutingListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polygon
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.maps.android.SphericalUtil
import com.pakdrive.Utils.dismissProgressDialog
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.Policy
import java.util.Arrays
import java.util.Locale

object MapUtils {

    var polylines: ArrayList<Polyline> = ArrayList()
    var markers: ArrayList<Marker> = ArrayList()
    private val handler = Handler(Looper.getMainLooper())

    private val curvePolygons = mutableListOf<Polygon>()


    fun showPlaces(map:GoogleMap,places:ArrayList<LatLng>){
        places.forEach {
            map.addMarker(MarkerOptions().position(it))
        }
    }

    fun routingSuccess(route: ArrayList<Route>, shortestRouteIndex: Int, context: Activity, onGoogleMap: GoogleMap, st: String, dt: String, color: Int,startMarkerBol:Boolean=true,startLatLang:LatLng,endLatLang:LatLng) {

        clearMapObjects()

        val polyOptions = PolylineOptions()
        var polylineStartLatLng: LatLng? = null
        var polylineEndLatLng: LatLng? = null

        for (i in route.indices) {
            if (i == shortestRouteIndex) {
                polyOptions.color(context.resources.getColor(color))
                polyOptions.width(7f)
                polyOptions.addAll(route[shortestRouteIndex].points)

                val polyline = onGoogleMap.addPolyline(polyOptions)
                polylineStartLatLng = polyline.points[0]
                val k = polyline.points.size
                polylineEndLatLng = polyline.points[k - 1]
                polylines.add(polyline)
            }
        }
        if (startMarkerBol){
            val startMarker = MarkerOptions()
            startMarker.position(polylineStartLatLng!!)
            startMarker.title(st)
            val startMarkerObject = onGoogleMap.addMarker(startMarker)
            markers.add(startMarkerObject!!)
        }

        val endMarker = MarkerOptions()
        endMarker.position(polylineEndLatLng!!)
        endMarker.title(dt)
        val endMarkerObject = onGoogleMap.addMarker(endMarker)
        markers.add(endMarkerObject!!)

    }


    fun clearMapObjects() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines.clear()

        for (marker in markers) {
            marker.remove()
        }
        markers.clear()
    }

    fun removePreviousMarkers(markersList: MutableList<Marker>) {
        if (markersList.isEmpty()){
            return
        }
        for (marker in markersList) {
            marker.remove()
        }
        markersList.clear()
        clearCurves()
    }


    @SuppressLint("MissingPermission")
    fun updateLocationUI(map: GoogleMap,context: Activity,fusedLocationClient: FusedLocationProviderClient,dialog:Dialog) {
        try {
            map.apply {
                isMyLocationEnabled = true
                uiSettings.isMyLocationButtonEnabled = true

                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(context) { task ->
                    if (task.result!=null){

                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses= geocoder.getFromLocation(
                            task.result.latitude,
                            task.result.longitude,
                            1
                        )

                        if (addresses!!.isNotEmpty()) {
                            val address: Address = addresses[0]
                            val addressName = address.getAddressLine(0)
                            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(task.result.latitude,task.result.longitude),17f))
                            dismissProgressDialog(dialog)
                            val markerOption=MarkerOptions()
                            markerOption.title(addressName)
                            markerOption.position(LatLng(task.result.latitude,task.result.longitude))
                            map.addMarker(markerOption)
                        }else{
                            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(task.result.latitude,task.result.longitude),17f))
                            dismissProgressDialog(dialog)
                            val markerOption=MarkerOptions()
                            markerOption.title("Current Location")
                            markerOption.position(LatLng(task.result.latitude,task.result.longitude))
                            map.addMarker(markerOption)
                        }

                    }else{
                        Log.i("task", "updateLocationUI:${task.result}")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.i("Exception: %s", e.message, e)
        }
    }


    fun mapTravelModeToAbstractRouting(googleMapsTravelMode: com.google.maps.model.TravelMode): AbstractRouting.TravelMode {
        return when (googleMapsTravelMode) {
            com.google.maps.model.TravelMode.DRIVING -> AbstractRouting.TravelMode.DRIVING
            com.google.maps.model.TravelMode.WALKING -> AbstractRouting.TravelMode.WALKING
            com.google.maps.model.TravelMode.BICYCLING -> AbstractRouting.TravelMode.BIKING
            com.google.maps.model.TravelMode.TRANSIT -> AbstractRouting.TravelMode.TRANSIT
            else -> throw IllegalArgumentException("Unsupported TravelMode: $googleMapsTravelMode")
        }
    }


    fun calculateDistance(point1: LatLng, point2: LatLng): Float {
        val location1 = Location("point1")
        location1.latitude = point1.latitude
        location1.longitude = point1.longitude

        val location2 = Location("point2")
        location2.latitude = point2.latitude
        location2.longitude = point2.longitude
        return location1.distanceTo(location2)
    }


    fun drawCurveOnMap(context: Activity,googleMap: GoogleMap, latLng1: LatLng, latLng2: LatLng) {
        val k = 0.5 //curve radius
        var h = SphericalUtil.computeHeading(latLng1, latLng2)
        var d = 0.0
        val p: LatLng?

        if (h < 0) {
            d = SphericalUtil.computeDistanceBetween(latLng2, latLng1)
            h = SphericalUtil.computeHeading(latLng2, latLng1)
            //Midpoint position
            p = SphericalUtil.computeOffset(latLng2, d * 0.5, h)
        } else {
            d = SphericalUtil.computeDistanceBetween(latLng1, latLng2)

            //Midpoint position
            p = SphericalUtil.computeOffset(latLng1, d * 0.5, h)
        }

        //Apply some mathematics to calculate position of the circle center
        val x = (1 - k * k) * d * 0.5 / (2 * k)
        val r = (1 + k * k) * d * 0.5 / (2 * k)

        val c = SphericalUtil.computeOffset(p, x, h + 90.0)

        //Calculate heading between circle center and two points
        val h1 = SphericalUtil.computeHeading(c, latLng1)
        val h2 = SphericalUtil.computeHeading(c, latLng2)

        //Calculate positions of points on circle border and add them to polyline options
        val numberOfPoints = 1000 //more numberOfPoints more smooth curve you will get
        val step = (h2 - h1) / numberOfPoints

        //Create PolygonOptions object to draw on map
        val polygon = PolygonOptions()

        //Create a temporary list of LatLng to store the points that's being drawn on map for curve
        val temp = arrayListOf<LatLng>()

        //iterate the numberOfPoints and add the LatLng to PolygonOptions to draw curve
        //and save in temp list to add again reversely in PolygonOptions
        for (i in 0 until numberOfPoints) {
            val latlng = SphericalUtil.computeOffset(c, r, h1 + i * step)
            polygon.add(latlng) //Adding in PolygonOptions
            temp.add(latlng)    //Storing in temp list to add again in reverse order
        }

        //iterate the temp list in reverse order and add in PolygonOptions
        for (i in (temp.size - 1) downTo 1) {
            polygon.add(temp[i])
        }

        polygon.strokeColor(context.getColor(R.color.yellow))
        polygon.strokeWidth(8f)
        val drewPolygon=googleMap.addPolygon(polygon)
        curvePolygons.add(drewPolygon)
        temp.clear()
    }

    fun clearCurves() {
        for (polygon in curvePolygons) {
            polygon.remove()
        }
        curvePolygons.clear()
    }


}