package com.pakdrive.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.pakdrive.MyConstants.DRIVERUID
import com.pakdrive.R
import com.pakdrive.databinding.ActivityRatingBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class RatingActivity : AppCompatActivity() {
    lateinit var binding:ActivityRatingBinding
    var uidBundle=""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@RatingActivity,R.layout.activity_rating)

        uidBundle=intent.getStringExtra(DRIVERUID)!!
        Log.i("TAG", uidBundle)

    }
}