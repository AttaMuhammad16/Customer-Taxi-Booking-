package com.pakdrive.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.databinding.ActivityDriverRequestsViewBinding

class DriverRequestsViewActivity : AppCompatActivity() {
   lateinit var binding:ActivityDriverRequestsViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_driver_requests_view)
        Utils.statusBarColor(this@DriverRequestsViewActivity)

    }
}