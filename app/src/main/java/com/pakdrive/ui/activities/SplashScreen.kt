package com.pakdrive.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.pakdrive.InternetChecker
import com.pakdrive.MyConstants
import com.pakdrive.PermissionHandler
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.ui.activities.auth.CustomerLoginActivity
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashScreen : AppCompatActivity() {
    @Inject
    lateinit var auth: FirebaseAuth
    val customerViewMode:CustomerViewModel by viewModels()
    lateinit var  locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        Utils.statusBarColor(this@SplashScreen)

        locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager

        Handler().postDelayed({
            if (auth.currentUser!=null){
                lifecycleScope.launch {
                    val internetChecker=InternetChecker().isInternetConnectedWithPackage(this@SplashScreen)
                    if (internetChecker&&locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                        val customerDiffered=async { customerViewMode.getUser(MyConstants.CUSTOMER,auth.currentUser!!.uid) }
                        val model= customerDiffered.await()!!
                        if (model.lock){
                            startActivity(Intent(this@SplashScreen,AccountBlockActivity::class.java))
                            finish()
                        }else{
                            val mainIntent = Intent(this@SplashScreen, MainActivity::class.java)
                            startActivity(mainIntent)
                            finish()
                        }
                    }else{
                        PermissionHandler.showEnableGpsDialog(this@SplashScreen)
                        Toast.makeText(this@SplashScreen, "Please check your internet and location is on.", Toast.LENGTH_LONG).show()
                    }
                }
            }else{
                val mainIntent = Intent(this@SplashScreen, CustomerLoginActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        }, 3000)

    }
}
