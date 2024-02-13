package com.pakdrive.ui.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.pakdrive.MyConstants
import com.pakdrive.MyConstants.APPURL
import com.pakdrive.MyConstants.REQUESTCODEFORPERMISSION
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.databinding.ActivityProfileBinding
import com.pakdrive.ui.viewmodels.CustomerViewModel
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding
    val customerViewModel:CustomerViewModel by viewModels()
    @Inject
    lateinit var auth:FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@ProfileActivity,R.layout.activity_profile)
        var dialog=Utils.showProgressDialog(this@ProfileActivity,"Loading...")
        Utils.statusBarColor(this@ProfileActivity)
        binding.backImage.setOnClickListener {
            finish()
        }
        lifecycleScope.launch{
            if (auth.currentUser!=null) {

                val customerDetails=customerViewModel.getUser(MyConstants.CUSTOMER, auth.uid!!)?.apply {
                    Picasso.get().load(profileImage).placeholder(R.drawable.user).into(binding.userImage)
                    binding.frontNameTv.text = userName
                    binding.nameTv.text = userName
                    binding.phoneNumberTv.text = phoneNumber
                    binding.addressTv.text = address
                    binding.emailTv.text = email
                }

                Utils.dismissProgressDialog(dialog)

                binding.nameTv.setOnClickListener {
                    Utils.showInputDialog(this@ProfileActivity,"Edit Your Details",customerDetails!!.userName){
                        lifecycleScope.launch {
                            async { customerViewModel.updateCustomerPersonalDetails(it,null,null) }.await()
                            Toast.makeText(this@ProfileActivity, "Name saved", Toast.LENGTH_SHORT).show()
                            binding.frontNameTv.text = it
                            binding.nameTv.text = it
                        }
                    }
                }

                binding.phoneNumberTv.setOnClickListener {
                    Utils.showInputDialog(this@ProfileActivity,"Edit Your Details",customerDetails!!.phoneNumber){
                        lifecycleScope.launch {
                            async { customerViewModel.updateCustomerPersonalDetails(null,it,null) }.await()
                            Toast.makeText(this@ProfileActivity, "phoneNumber saved", Toast.LENGTH_SHORT).show()
                            binding.phoneNumberTv.text = it
                        }
                    }
                }

                binding.addressTv.setOnClickListener {
                    Utils.showInputDialog(this@ProfileActivity,"Edit Your Details",customerDetails!!.address){
                        lifecycleScope.launch {
                            async { customerViewModel.updateCustomerPersonalDetails(null,null,it) }.await()
                            Toast.makeText(this@ProfileActivity, "Address saved", Toast.LENGTH_SHORT).show()
                            binding.addressTv.text = it
                        }
                    }
                }

            }

            binding.rideHistoryTv.setOnClickListener {
                startActivity(Intent(this@ProfileActivity,CustomerHistoryActivity::class.java))
            }
            binding.shareLocationTv.setOnClickListener {
                Utils.getCurrentLocation(this@ProfileActivity)
            }
            binding.inviteFriendsTv.setOnClickListener {
                Utils.shareText(APPURL,this@ProfileActivity)
            }

        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0]==Activity.RESULT_OK && requestCode==REQUESTCODEFORPERMISSION){
            Utils.getCurrentLocation(this@ProfileActivity)
        }
    }

}