package com.pakdrive.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pakdrive.R
import com.pakdrive.models.RideHistoryModel

class RideHistoryAdapter(private val rideHistory: ArrayList<RideHistoryModel>, var context: Activity) : RecyclerView.Adapter<RideHistoryAdapter.RequestViewHolder>() {
    class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var dateTv: TextView = view.findViewById(R.id.dateTv)
        var pickUpPointTv: TextView = view.findViewById(R.id.pickUpPointTv)
        var destinationTv: TextView = view.findViewById(R.id.destinationTv)
        var paymentTv: TextView = view.findViewById(R.id.paymentTv)
        var statusTv: TextView = view.findViewById(R.id.statusTv)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.ride_history_item_view, parent, false)
        return RequestViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "ResourceAsColor")
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val data = rideHistory[position]
        holder.dateTv.text=data.date
        holder.pickUpPointTv.text=data.pickUpPoint
        holder.destinationTv.text=data.destinationPoint
        holder.paymentTv.text="${data.payment} PKR"
        if (data.rideStatus){
            holder.statusTv.text="Completed"
            holder.statusTv.setTextColor(Color.parseColor("#1BDF21"))
        }else{
            holder.statusTv.text="Cancelled"
            holder.statusTv.setTextColor(Color.parseColor("#F61D37"))
        }
    }
    override fun getItemCount() = rideHistory.size
}