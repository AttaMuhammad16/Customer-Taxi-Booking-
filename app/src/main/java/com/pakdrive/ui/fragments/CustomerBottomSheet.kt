package com.pakdrive.ui.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.maps.model.TravelMode
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.apiKey
import com.pakdrive.Utils.calculatePrice
import com.pakdrive.adapters.AutoCompleteAdapter
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomerBottomSheet : BottomSheetDialogFragment() {
    private lateinit var adapter: AutoCompleteAdapter
    lateinit var predictionsList: List<AutocompletePrediction>
    private lateinit var etSearch: AutoCompleteTextView
    private lateinit var destinationSearch: AutoCompleteTextView
    private lateinit var priceRageEdt: AutoCompleteTextView
    private lateinit var commentEdt: AutoCompleteTextView
    lateinit var crossIcon: ImageView
    lateinit var crossForDes: ImageView
    lateinit var crossPrice: ImageView
    lateinit var commentCross: ImageView
    lateinit var pd: ProgressBar

    lateinit var placesClient: PlacesClient
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    protected var start: LatLng? = null
    protected var end: LatLng? = null
    protected lateinit var findBtn: Button

    private var callback: UserInputDetails? = null
    val customerViewModel:CustomerViewModel by viewModels()
    var pricePerKm=0.0

    @Inject
    lateinit var databaseReference: DatabaseReference
    @Inject
    lateinit var auth:FirebaseAuth

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is UserInputDetails) {
            callback = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var view= inflater.inflate(R.layout.fragment_customer_bottom_sheet, container, false)


        etSearch = view.findViewById(R.id.etSearch)
        destinationSearch = view.findViewById(R.id.destinationSearch)
        crossIcon = view.findViewById(R.id.crossIcon)
        crossForDes = view.findViewById(R.id.crossForDes)
        findBtn = view.findViewById(R.id.findBtn)
        crossPrice = view.findViewById(R.id.crossPrice)
        priceRageEdt = view.findViewById(R.id.priceRageEdt)
        commentCross = view.findViewById(R.id.commentCross)
        commentEdt = view.findViewById(R.id.commentEdt)
        pd = view.findViewById(R.id.pd)

        if (auth.currentUser!=null){
            databaseReference.child("pricePerKM").addValueEventListener(object:ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    pricePerKm= snapshot.getValue(Double::class.java)?:0.0
                }
                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        Places.initialize(requireContext(), getString(R.string.api));
        placesClient = Places.createClient(requireContext());


        if (!Utils.isLocationPermissionGranted(requireActivity())) {
            Utils.requestLocationPermission(requireActivity())
        }

        crossIcon.setOnClickListener {
            etSearch.setText("")
        }

        crossForDes.setOnClickListener {
            destinationSearch.setText("")
        }

        crossPrice.setOnClickListener {
            priceRageEdt.setText("")
        }

        commentCross.setOnClickListener {
            commentEdt.setText("")
        }

        findBtn.setOnClickListener {
            var st=etSearch.text.toString().trim()
            var dt=destinationSearch.text.toString().trim()
            var price=priceRageEdt.text.toString().trim()
            var comment=commentEdt.text.toString().trim()
            if (start==null){
                etSearch.requestFocus()
                Toast.makeText(requireContext(), "Enter Pick up point.", Toast.LENGTH_SHORT).show()
            }else if (end==null){
                Toast.makeText(requireContext(), "Enter Destination", Toast.LENGTH_SHORT).show()
                destinationSearch.requestFocus()
            }else if (price.isEmpty()){
                Toast.makeText(requireContext(), "Enter Price Range.", Toast.LENGTH_SHORT).show()
                priceRageEdt.requestFocus()
            }else{
                getUserInputDetails(start!!,end!!,st,dt,price.toInt(),comment)
                this@CustomerBottomSheet.dismiss()
            }
        }

        adapter = AutoCompleteAdapter(requireActivity(), placesClient)
        etSearch.setAdapter(adapter)
        destinationSearch.setAdapter(adapter)


        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (etSearch.text.isEmpty()){
                    crossIcon.visibility = View.GONE
                }else{
                    crossIcon.visibility = View.VISIBLE
                }
            }

            override fun afterTextChanged(et: Editable?) {
                lifecycleScope.launch {
                    val query = et?.toString() ?: ""
                    predictionsList = adapter.getAutocompletePredictions(query)
                    adapter.clear()
                    adapter.addAll(predictionsList.map {it.getFullText(null).toString()})
                }
            }
        })

        destinationSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (destinationSearch.text.isEmpty()){
                    crossForDes.visibility = View.GONE
                }else{
                    crossForDes.visibility = View.VISIBLE
                }
            }
            override fun afterTextChanged(et: Editable?) {
                lifecycleScope.launch {

                    val query = et?.toString() ?: ""
                    predictionsList = adapter.getAutocompletePredictions(query)
                    adapter.clear()
                    adapter.addAll(predictionsList.map { it.getFullText(null).toString() })

                }
            }
        })

        etSearch.setOnItemClickListener { _, _, position, _ ->
            val placeId = predictionsList[position].placeId
            val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)

            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                var latlang: LatLng = place.latLng
                start = LatLng(latlang.latitude,latlang.longitude)
            }
        }


        destinationSearch.setOnItemClickListener { _, _, position, _ ->
            val placeId = predictionsList[position].placeId
            val placeFields = listOf(Place.Field.NAME, Place.Field.LAT_LNG)
            val request = FetchPlaceRequest.newInstance(placeId, placeFields)

            placesClient.fetchPlace(request).addOnSuccessListener { response ->
                val place = response.place
                var latlang: LatLng = place.latLng
                end = LatLng(latlang.latitude,latlang.longitude)
                if (start==null){
                    Toast.makeText(requireContext(), "Enter Your Start Destination", Toast.LENGTH_SHORT).show()
                }else{
                    lifecycleScope.launch {
                        pd.visibility=View.VISIBLE
                        var dialog=Utils.showProgressDialog(requireContext(),"wait...")
                        var distanceDiffered= async { customerViewModel.calculateDistanceForRoute(start!!,end!!, apiKey,TravelMode.DRIVING)?:0.0 }
                        var distanceInKM=distanceDiffered.await()
                        var price=calculatePrice(distanceInKM,pricePerKm).toInt()
                        priceRageEdt.setText("$price")
                        Utils.dismissProgressDialog(dialog)
                        pd.visibility=View.GONE
                    }
                }
            }
        }
        return view
    }

    private fun getUserInputDetails(startLatLng:LatLng, endLatLng:LatLng,st:String,dt:String,priceRange:Int,comment:String) {
        callback?.userInputDetails(startLatLng,endLatLng,st,dt,priceRange,comment)
    }

}