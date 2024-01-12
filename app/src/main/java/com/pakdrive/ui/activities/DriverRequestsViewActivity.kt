package com.pakdrive.ui.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.adapters.DriversRequestsAdapter
import com.pakdrive.databinding.ActivityDriverRequestsViewBinding
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
@AndroidEntryPoint
class DriverRequestsViewActivity : AppCompatActivity() {
   lateinit var binding:ActivityDriverRequestsViewBinding
   lateinit var adapter:DriversRequestsAdapter
   var list:ArrayList<DriverModel> = ArrayList()
   val customerModel:CustomerViewModel by viewModels()
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_driver_requests_view)
        Utils.statusBarColor(this@DriverRequestsViewActivity)

        lifecycleScope.launch {
            var dialog=Utils.showProgressDialog(this@DriverRequestsViewActivity,"Loading...")
            customerModel.receivedOffers().collect { offers ->
                list.clear()
                offers.forEach {
                    var driverModel=async { customerModel.readingDriver(it.driverUid) }.await()
                    if (driverModel!=null){
                        list.add(driverModel)
                    }
                }
                adapter = DriversRequestsAdapter(list,this@DriverRequestsViewActivity)
                binding.recyclerView.apply {
                    layoutManager = LinearLayoutManager(this@DriverRequestsViewActivity)
                    adapter = this@DriverRequestsViewActivity.adapter
                }
                adapter.notifyDataSetChanged()
                Utils.dismissProgressDialog(dialog)
                if (list.size==0){
                    binding.blankTv.visibility= View.VISIBLE
                }
            }
        }
    }
}

