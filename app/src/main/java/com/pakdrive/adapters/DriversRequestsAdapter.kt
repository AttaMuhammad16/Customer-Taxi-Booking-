package com.pakdrive.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.pakdrive.R
import com.pakdrive.models.CustomerModel
import com.pakdrive.models.DriverModel
import de.hdodenhof.circleimageview.CircleImageView

class DriversRequestsAdapter(private val requestList: ArrayList<DriverModel>, var context: Activity) : RecyclerView.Adapter<DriversRequestsAdapter.RequestViewHolder>() {
    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var profileImage: CircleImageView = view.findViewById(R.id.profileImage)
        var driverName: TextView = view.findViewById(R.id.driverName)
        var vehicleName: TextView = view.findViewById(R.id.vehicleName)
        var priceTv: TextView = view.findViewById(R.id.priceTv)
        var totalRatingTv: TextView = view.findViewById(R.id.totalRatingTv)
        var totalPersonRatings: TextView = view.findViewById(R.id.totalPersonRatings)
        var timeTakenTv: TextView = view.findViewById(R.id.timeTakenTv)
        var distanceTv: TextView = view.findViewById(R.id.distanceTv)
        var declineBtn: Button = view.findViewById(R.id.declineBtn)
        var acceptBtn: Button = view.findViewById(R.id.acceptBtn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.driver_requests_sample_row, parent, false)
        return RequestViewHolder(view)
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val data = requestList[position]
        Glide.with(context).load(data.profileImageUrl).placeholder(R.drawable.person_with_out_circle).into(holder.profileImage)
        holder.driverName.text=data.userName
        holder.vehicleName.text=data.carDetails
        holder.priceTv.text=data.far // update when request place
        holder.totalRatingTv.text=data.totalRating.toString()
        holder.totalPersonRatings.text=data.totalPersonRatings.toString()
        holder.timeTakenTv.text=data.timeTravelToCustomer // update when request place
        holder.distanceTv.text=data.distanceTravelToCustomer // update when request place

        holder.declineBtn.setOnClickListener {

        }

        holder.acceptBtn.setOnClickListener {

        }

    }
    override fun getItemCount() = requestList.size
}