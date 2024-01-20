package com.pakdrive

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.pakdrive.ui.viewmodels.CustomerViewModel
import javax.inject.Inject

class RateUsDialogue(var context: Activity,var imageUrl:String,var driverUid:String,var customerViewModel: CustomerViewModel,var rating:Float,var totalPersons:Long) : Dialog(context) {
    var userRate = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.rating_dialog)

        val rateNowBtn: AppCompatButton = findViewById(R.id.rateNow)
        val laterBtn: AppCompatButton = findViewById(R.id.laterBtn)
        val ratingBar: RatingBar = findViewById(R.id.ratingBar)
        val ratingImage: ImageView = findViewById(R.id.ratingImage)
        Glide.with(context).load(imageUrl).placeholder(R.drawable.person).into(ratingImage)

        rateNowBtn.setOnClickListener {
            if (userRate!=0f){
                var dialog=Utils.showProgressDialog(context,"Loading...")
                customerViewModel.ratingToTheDriver(driverUid,rating,userRate,totalPersons){
                    Utils.resultChecker(it,context)
                    if (it is MyResult.Success){
                        Utils.dismissProgressDialog(dialog)
                        dismiss()
                        context.finish()
                    }
                }
            }else{
                Toast.makeText(context, "Give rating", Toast.LENGTH_SHORT).show()
            }
        }

        laterBtn.setOnClickListener {
            dismiss()
            context.finish()
        }

        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            userRate = rating
        }

    }
}