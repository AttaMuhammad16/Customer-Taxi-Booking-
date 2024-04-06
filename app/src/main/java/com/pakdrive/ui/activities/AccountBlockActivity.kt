package com.pakdrive.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.pakdrive.MyConstants
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.setUpNavigationColor
import com.pakdrive.databinding.ActivityAccountBlockBinding
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.MessageModel
import com.pakdrive.service.SendNotification
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class AccountBlockActivity : AppCompatActivity() {
    lateinit var binding:ActivityAccountBlockBinding
    val customerViewMode:CustomerViewModel by viewModels()
    @Inject
    lateinit var auth:FirebaseAuth
    var customer:CustomerModel?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@AccountBlockActivity,R.layout.activity_account_block)
        Utils.statusBarColor(this@AccountBlockActivity)
        setUpNavigationColor()

        lifecycleScope.launch {
            val user=auth.currentUser
            if (user!=null){
                customer=customerViewMode.getUser(MyConstants.CUSTOMER,user.uid)
                if (customer!=null){
                    customer?.apply {
                        binding.nameEdt.setText(userName)
                        binding.emailEdt.setText(email)
                        binding.phoneNumberEdt.setText(phoneNumber)
                    }
                }else{
                    Toast.makeText(this@AccountBlockActivity, "User exit.", Toast.LENGTH_SHORT).show()
                }
            }else{
                Toast.makeText(this@AccountBlockActivity, "User not found.", Toast.LENGTH_SHORT).show()
            }
        }


        binding.submitBtn.setOnClickListener {

            if (binding.nameEdt.text!!.isEmpty()){
                binding.nameEdt.error="Enter Name."
                Toast.makeText(this@AccountBlockActivity, "Enter Name.", Toast.LENGTH_SHORT).show()
            }else if (binding.emailEdt.text!!.isEmpty()){
                binding.emailEdt.error="Enter Email."
                Toast.makeText(this@AccountBlockActivity, "Enter Email.", Toast.LENGTH_SHORT).show()
            }else if (binding.phoneNumberEdt.text!!.isEmpty()){
                binding.phoneNumberEdt.error="Enter PhoneNumber."
                Toast.makeText(this@AccountBlockActivity, "Enter PhoneNumber.", Toast.LENGTH_SHORT).show()
            }else if (binding.messageEdt.text!!.isEmpty()){
                binding.messageEdt.error="Enter Message."
                Toast.makeText(this@AccountBlockActivity, "Enter Message.", Toast.LENGTH_SHORT).show()
            }else{
                val dialog=Utils.showProgressDialog(this@AccountBlockActivity,"Sending...")
                lifecycleScope.launch {
                    val name=binding.nameEdt.text.toString()
                    val email=binding.emailEdt.text.toString()
                    val phoneNumber=binding.phoneNumberEdt.text.toString()
                    val message=binding.messageEdt.text.toString()
                    val currentTime=System.currentTimeMillis().toString()
                    val messageModel=MessageModel("",name, email, phoneNumber, message,currentTime,"",customer!!.profileImage)
                    val result=customerViewMode.uploadMessage(messageModel)
                    val adminToken=async { customerViewMode.getAdminFCM() }.await()
                    SendNotification.sendNotificationToAdmin("Pak Drive customer support","new message",adminToken)
                    Utils.resultChecker(result,this@AccountBlockActivity)
                    dialog.dismiss()
                    binding.messageEdt.setText("")
                }
            }
        }

    }
}