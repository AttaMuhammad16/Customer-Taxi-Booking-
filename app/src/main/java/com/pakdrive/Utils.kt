package com.pakdrive

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.net.Uri
import android.text.InputType
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.pakdrive.MyConstants.LOCATION_PERMISSION_REQUEST_CODE
import com.pakdrive.MyConstants.REQUESTCODEFORPERMISSION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Utils {

    fun isLocationPermissionGranted(context:Activity): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun statusBarColor(context: Activity,color:Int=R.color.tool_color){
        context.window.statusBarColor=ContextCompat.getColor(context,color)
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})"
        val pattern = Regex(emailRegex)
        return pattern.matches(email)
    }

    fun pickImage(requestCode: Int,context: Activity){
        var intent= Intent(Intent.ACTION_GET_CONTENT)
        intent.type="image/*"
        context.startActivityForResult(intent,requestCode)
    }

    fun myToast(context: Activity,message:String,length:Int=Toast.LENGTH_SHORT){
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, message,length).show()
        }
    }

    fun resultChecker(result:MyResult, context: Activity){
        when(result){
            is MyResult.Success->{
                myToast(context,result.success)
            }
            is MyResult.Error->{
                myToast(context,result.error)
            }
            else->{
                myToast(context,"unknown error")
            }
        }
    }


    fun isValidPakistaniPhoneNumber(phoneNumber: String): Boolean {
        return if (phoneNumber.startsWith("+923")){
            phoneNumber.length==13
        }else if (phoneNumber.startsWith("923")){
            phoneNumber.length==12
        }else if (phoneNumber.startsWith("03")){
            phoneNumber.length==11
        }else{
            false
        }
    }


    suspend fun convertUriToBitmap(uri: Uri?, context: Activity): Bitmap? {
        return withContext(Dispatchers.IO) {
            if (uri != null) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        BitmapFactory.decodeStream(inputStream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            } else {
                null
            }
        }
    }



    fun showProgressDialog(context: Context, message: String): Dialog {
        val progressDialog = Dialog(context)
        progressDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        progressDialog.setCancelable(false)

        val view = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val messageTextView = view.findViewById<TextView>(R.id.messageTextView)
        messageTextView.text = message
        progressDialog.setContentView(view)
        progressDialog.show()
        return progressDialog
    }

    fun dismissProgressDialog(progressDialog: Dialog) {
        progressDialog.dismiss()
    }

    fun generateFCMToken(role:String,tokenNode: String,auth: FirebaseAuth){
        if (auth.currentUser!=null){
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isSuccessful) {
                    var map= hashMapOf<String,Any>()
                    map[tokenNode]=it.result
                    FirebaseDatabase.getInstance().reference.child(role).child(auth.currentUser!!.uid).updateChildren(map)
                }
            }
        }
    }


    fun updateFCMToken(role:String,tokenNode: String,updatedToken:String,auth: FirebaseAuth){
        if (auth.currentUser!=null){
            var map= hashMapOf<String,Any>()
            map[tokenNode]=updatedToken
            FirebaseDatabase.getInstance().reference.child(role).child(auth.currentUser!!.uid).updateChildren(map)
        }
    }



    suspend fun calculatePrice(kilometers: Double,pricePerKilometer:Double): Double {
        return kilometers * pricePerKilometer
    }


    fun stringToLatLng(latLangString: String?): LatLng? {
        if (latLangString.isNullOrBlank()) {
            return null
        }
        val startIndex = latLangString.indexOf("(") + 1
        val endIndex = latLangString.indexOf(")")

        if (startIndex == -1 || endIndex == -1) {
            return null
        }
        val latLngSubstring = latLangString.substring(startIndex, endIndex)
        val (latitude, longitude) = latLngSubstring.split(",").map { it.trim().toDoubleOrNull() ?: return null }
        return LatLng(latitude, longitude)
    }



    fun showAlertDialog(context: Activity,dialogeInterface: DialogeInterface,title:String){
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setPositiveButton("Yes") { _, _ ->
            dialogeInterface.clickedBol(true)
        }
        builder.setNegativeButton("No") { _, _ ->
            dialogeInterface.clickedBol(false)
        }
        val dialog = builder.create()
        dialog.show()
    }



    fun invalidInputsMessage(context: Activity,editText: EditText,message:String,dialog: Dialog) {
          editText?.error = message
          myToast(context, message)
          dismissProgressDialog(dialog)
    }


    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    fun requestLocationPermission(context: Activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            AlertDialog.Builder(context)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality.")
                .setPositiveButton("OK") { _, _ ->
                    ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }.create().show()
        } else {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }



    fun blinkAnimation(context: Activity):Animation{
        return AnimationUtils.loadAnimation(context, R.anim.blink)
    }

    fun getCurrentFormattedDate(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val currentDate = Date(currentTimeMillis)
        return dateFormat.format(currentDate)
    }


    private fun shareLocation(location: Location,context: Activity) {
        val latitude = location.latitude
        val longitude = location.longitude
        val uriString = "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Pak Drive\nCheck out my location here: $uriString")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Location Using: Pak Drive"))
    }


    fun getCurrentLocation(context: Activity) {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
           ActivityCompat.requestPermissions(context,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUESTCODEFORPERMISSION)
        }
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location : Location? ->
            location?.let {
                shareLocation(it,context)
            }
        }
    }

    fun shareText(text: String,context: Activity) {
        val baseText = "Check out Pak Drive(For Customers)! It's amazing. Install now for an easy and enjoyable ride. 🚗\n\n $text"
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, baseText)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share text via:Pak Drive"))
    }




    fun showInputDialog(context: Activity, title: String,text: String, onSave: (String) -> Unit) {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(text)
        }
        val builder = AlertDialog.Builder(context).apply {
            setTitle(title)
            setView(input)
            setPositiveButton("Save") { dialog, _ ->
                val text = input.text.toString()
                if (text.isNotEmpty()) {
                    onSave(text)
                    dialog.dismiss()
                } else {
                    myToast(context, "Enter your details")
                }
            }
            setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
        }
        builder.show()
    }


    fun Activity.setUpNavigationColor(color: Int=R.color.widgetscolor){
        window.navigationBarColor= ContextCompat.getColor(this,color)
    }


    fun shareAppLink(context: Context,str: String){
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, str)
        context.startActivity(Intent.createChooser(intent, "Share via"))
    }

}


interface DialogeInterface{
    fun clickedBol(bol: Boolean)
}