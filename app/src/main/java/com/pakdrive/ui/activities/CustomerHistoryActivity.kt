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
import com.pakdrive.adapters.RideHistoryAdapter
import com.pakdrive.databinding.ActivityCustomerHistoryBinding
import com.pakdrive.models.RideHistoryModel
import com.pakdrive.ui.viewmodels.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomerHistoryActivity : AppCompatActivity() {
    lateinit var binding:ActivityCustomerHistoryBinding
    var list:ArrayList<RideHistoryModel> = ArrayList()
    val customerViewModel:CustomerViewModel by viewModels()
    lateinit var adapter:RideHistoryAdapter
    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=DataBindingUtil.setContentView(this@CustomerHistoryActivity,R.layout.activity_customer_history)
        Utils.statusBarColor(this@CustomerHistoryActivity)
        var dialog=Utils.showProgressDialog(this@CustomerHistoryActivity,"Loading...")
        lifecycleScope.launch{
            val data=customerViewModel.getRideHistory()
            if (data!=null){
                list=data
            }
            if (list.isEmpty()){
                binding.dummyTv.visibility=View.VISIBLE
            }
            list.reverse()
            binding.recyclerView.layoutManager=LinearLayoutManager(this@CustomerHistoryActivity)
            adapter= RideHistoryAdapter(list,this@CustomerHistoryActivity)
            binding.recyclerView.adapter=adapter
            dialog.dismiss()
        }
    }
}