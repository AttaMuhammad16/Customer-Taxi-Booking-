package com.pakdrive.ui.activities.auth

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.pakdrive.InternetChecker
import com.pakdrive.MyResult
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.invalidInputsMessage
import com.pakdrive.Utils.myToast
import com.pakdrive.databinding.ActivityCustomerLoginBinding
import com.pakdrive.ui.activities.MainActivity
import com.pakdrive.ui.viewmodels.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomerLoginActivity : AppCompatActivity() {
    val authViewModel:AuthViewModel by viewModels()
    lateinit var binding:ActivityCustomerLoginBinding
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_customer_login)
        Utils.statusBarColor(this@CustomerLoginActivity)
        val user=auth.currentUser

        if (user!=null){
            startActivity(Intent(this@CustomerLoginActivity,MainActivity::class.java))
            finish()
        }
        binding.signUpTv.setOnClickListener {
            startActivity(Intent(this@CustomerLoginActivity,CustomerSignUpActivity::class.java))
        }
        binding.loginBtn.setOnClickListener {

            val dialog= Utils.showProgressDialog(this,"Loading")
            val email = binding.emailEdt.text.toString().trim()
            val password = binding.passwordEdt.text.toString().trim()

            if (email.isEmpty()) {
                invalidInputsMessage(this,binding.emailEdt,"Enter E-mail",dialog)

            } else if (!Utils.isValidEmail(email)) {
                invalidInputsMessage(this,binding.emailEdt,"Enter correct Email.",dialog)

            } else if (password.isEmpty()) {
                invalidInputsMessage(this,binding.passwordEdt,"Enter correct password.",dialog)

            } else if (password.length<=6){
                invalidInputsMessage(this,binding.passwordEdt,"Password length must be at least 6 characters.",dialog)

            } else {

                lifecycleScope.launch{
                    var isInternetAvailable=async { InternetChecker().isInternetConnectedWithPackage(this@CustomerLoginActivity) }
                    if (isInternetAvailable.await()){
                        authViewModel.loginUser(email,password) { result ->
                            if (result is MyResult.Error) {
                                myToast(this@CustomerLoginActivity, result.error)
                                dismissProgressDialog(dialog)
                            } else if (result is MyResult.Success) {
                                myToast(this@CustomerLoginActivity, result.success)
                                dismissProgressDialog(dialog)
                                startActivity(Intent(this@CustomerLoginActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    }else{
                        myToast(this@CustomerLoginActivity, "check your internet connection.")
                        dismissProgressDialog(dialog)
                    }
                }

            }
        }
    }
}