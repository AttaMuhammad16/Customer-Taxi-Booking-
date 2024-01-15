package com.pakdrive.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.adapters.DriversOfferAdapter
import com.pakdrive.databinding.ActivityDriverOffersBinding
import com.pakdrive.models.DriverModel
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@AndroidEntryPoint
class DriversOfferActivity : AppCompatActivity() {
   lateinit var binding:ActivityDriverOffersBinding
   lateinit var adapter:DriversOfferAdapter
   var list:ArrayList<DriverModel> = ArrayList()
   val customerModel:CustomerViewModel by viewModels()
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_driver_offers)
        Utils.statusBarColor(this@DriversOfferActivity)

        lifecycleScope.launch {
            var dialog=Utils.showProgressDialog(this@DriversOfferActivity,"Loading...")
            customerModel.receivedOffers().collect { offers ->
                list.clear()
                if (offers.size==0){
                    binding.blankTv.visibility= View.VISIBLE
                }else{
                    binding.blankTv.visibility=View.GONE
                }

                offers.forEach {
                    delay(500)
                    Log.i("TAG", "offers far value:${it.far}")
                    var driverModel=customerModel.readingDriver(it.driverUid)
                    if (driverModel!=null){
                        list.add(driverModel)
                        Log.i("TAG", "drivers far value:${driverModel.far}")
                    }
                }

                adapter = DriversOfferAdapter(list,this@DriversOfferActivity,customerModel)
                binding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(this@DriversOfferActivity)
                    adapter = this@DriversOfferActivity.adapter
                }

                adapter.notifyDataSetChanged()
                Utils.dismissProgressDialog(dialog)

            }
        }
    }
}

