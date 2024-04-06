package com.pakdrive.ui.activities.auth

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.pakdrive.MyConstants.USER_IMAGE_REQUEST_CODE
import com.pakdrive.MyResult
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.convertUriToBitmap
import com.pakdrive.Utils.isValidEmail
import com.pakdrive.Utils.isValidPakistaniPhoneNumber
import com.pakdrive.Utils.myToast
import com.pakdrive.Utils.resultChecker
import com.pakdrive.Utils.setUpNavigationColor
import com.pakdrive.databinding.ActivityCustomerSignUpBinding
import com.pakdrive.models.CustomerModel
import com.pakdrive.ui.viewmodels.AuthViewModel
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomerSignUpActivity : AppCompatActivity() {
    lateinit var binding:ActivityCustomerSignUpBinding

    val customerViewModel:CustomerViewModel by viewModels()
    val authViewModel:AuthViewModel by viewModels()

    private lateinit var bitmap: Bitmap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_customer_sign_up)
        Utils.statusBarColor(this,R.color.tool_color)
        setUpNavigationColor()

        binding.loginTv.setOnClickListener {
            startActivity(Intent(this@CustomerSignUpActivity,CustomerLoginActivity::class.java))
        }
        binding.selectUserImage.setOnClickListener {
            Utils.pickImage(USER_IMAGE_REQUEST_CODE,this)
        }

        binding.signUpBtn.setOnClickListener {
            val dialog=Utils.showProgressDialog(this,"Registering...")
            val userName=binding.nameEdt.text.toString().trim()
            val email=binding.emailEdt.text.toString().trim()
            val password=binding.passwordEdt.text.toString().trim()
            val phoneNumber=binding.phoneNumberEdt.text.toString().trim()
            val address=binding.addressEdt.text.toString().trim()

            if (userName.isEmpty()){
                Utils.invalidInputsMessage(this, binding.nameEdt, "Enter Name", dialog)
            }else if (email.isEmpty()){
                Utils.invalidInputsMessage(this, binding.emailEdt, "Enter E-mail", dialog)
            }else if (password.isEmpty()){
                Utils.invalidInputsMessage(this, binding.passwordEdt, "Enter Password", dialog)
            }else if (phoneNumber.isEmpty()){
                Utils.invalidInputsMessage(this, binding.phoneNumberEdt, "Enter PhoneNumber", dialog)
            }else if (!isValidPakistaniPhoneNumber(phoneNumber)){
                Utils.invalidInputsMessage(this, binding.phoneNumberEdt, "Enter correct phone number.", dialog)
            } else if (address.isEmpty()){
                Utils.invalidInputsMessage(this, binding.addressEdt, "Enter Address", dialog)
            }else if (address.length<5){
                Utils.invalidInputsMessage(this, binding.addressEdt, "address length must be at least 6", dialog)
            } else if (!isValidEmail(email)){
                Utils.invalidInputsMessage(this, binding.emailEdt, "Enter correct E-mail", dialog)
            }else if (!::bitmap.isInitialized){
                myToast(this,"Select User Image", Toast.LENGTH_LONG)
                Utils.dismissProgressDialog(dialog)
            }else if (password.length<=6){
                Utils.invalidInputsMessage(this, binding.passwordEdt, "Password length must be 6", dialog)
            }else{
                authViewModel.registerUser(email,password){authResult->
                    if (authResult is MyResult.Error){
                        resultChecker(authResult,this)
                    }else if (authResult is MyResult.Success){
                        lifecycleScope.launch {
                            val resultJob= async { customerViewModel.uploadImageToStorage(bitmap) }
                            val imageUrl=resultJob.await()
                            if (imageUrl is MyResult.Success){
                                val model= CustomerModel(null,userName, email, password, phoneNumber, address,imageUrl.success,"","","","","",false)
                                val uploadResult=async { customerViewModel.uploadUserOnDatabase(model) }
                                if (uploadResult.await() is MyResult.Success){
                                    Utils.dismissProgressDialog(dialog)
                                    resultChecker(uploadResult.await(),this@CustomerSignUpActivity)
                                    startActivity(Intent(this@CustomerSignUpActivity,CustomerLoginActivity::class.java))
                                    finish()
                                }else{
                                    resultChecker(uploadResult.await(),this@CustomerSignUpActivity)
                                    Utils.dismissProgressDialog(dialog)
                                }
                            }else{
                                resultChecker(resultJob.await(),this@CustomerSignUpActivity)
                                Utils.dismissProgressDialog(dialog)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== Activity.RESULT_OK&&requestCode==USER_IMAGE_REQUEST_CODE){
            var uri=data?.data?: Uri.parse("")

            var job= CoroutineScope(Dispatchers.IO).async {
                convertUriToBitmap(uri,this@CustomerSignUpActivity)!!
            }
            lifecycleScope.launch {
                bitmap=job.await()
                job.cancel()
                binding.selectedImg.setImageBitmap(bitmap)
            }
        }
    }

}