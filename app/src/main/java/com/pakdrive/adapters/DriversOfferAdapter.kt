package com.pakdrive.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pakdrive.DialogeInterface
import com.pakdrive.MyConstants.DRIVERUID
import com.pakdrive.MyResult
import com.pakdrive.PreferencesManager
import com.pakdrive.R
import com.pakdrive.Utils
import com.pakdrive.Utils.dismissProgressDialog
import com.pakdrive.Utils.resultChecker
import com.pakdrive.Utils.showProgressDialog
import com.pakdrive.models.AcceptModel
import com.pakdrive.models.DriverModel
import com.pakdrive.service.SendNotification.sendApprovedNotification
import com.pakdrive.service.SendNotification.sendCancellationNotification
import com.pakdrive.ui.activities.LiveDriverViewActivity
import com.pakdrive.ui.viewmodels.CustomerViewModel
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DriversOfferAdapter(private val requestList: ArrayList<DriverModel>, var context: Activity,val customerViewModel: CustomerViewModel) : RecyclerView.Adapter<DriversOfferAdapter.RequestViewHolder>() {
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.driver_offers_sample_row, parent, false)
        return RequestViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val data = requestList[position]
        Picasso.get().load(data.profileImageUrl).placeholder(R.drawable.person_with_out_circle).error(R.drawable.person_with_out_circle).into(holder.profileImage);

        holder.driverName.text=data.userName
        holder.vehicleName.text=data.carDetails
        holder.priceTv.text="${data.far} PKR"
        holder.totalRatingTv.text=data.totalRating.toString()
        holder.totalPersonRatings.text=data.totalPersonRatings.toString()
        holder.timeTakenTv.text=data.timeTravelToCustomer // update when request place

        val kilometers = data.distanceTravelToCustomer.toDouble()
        val meters = (kilometers * 1000).toInt()
        if (meters<1000){
            holder.distanceTv.text = "$meters meters"
        }else{
            holder.distanceTv.text = "${data.distanceTravelToCustomer} KM"
        }

        holder.declineBtn.setOnClickListener {
            Utils.showAlertDialog(context,object: DialogeInterface {
                override fun clickedBol(bol: Boolean) {
                    if (bol){
                        var dialog= showProgressDialog(context,"Cancelling...")
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val deleteOfferResult = customerViewModel.deleteOffer(data.uid!!)
                                if (deleteOfferResult is MyResult.Success) {
                                    val requestDeleteResult = customerViewModel.deleteRequest(data.uid!!)
                                    resultChecker(requestDeleteResult, context)
                                    sendCancellationNotification("Pak Drive","Ride Cancellation Notification.User does not accept your Offer",data.driverFCMToken,"false")
                                } else {
                                    resultChecker(deleteOfferResult, context)
                                }
                            } catch (e: Exception) {
                                resultChecker(MyResult.Error(e.message ?: "Unknown error"), context)
                            } finally {
                                dismissProgressDialog(dialog)
                            }
                        }

                    }
                }

            },"Do you want to cancel this request?")
        }

        holder.acceptBtn.setOnClickListener {
            Utils.showAlertDialog(context,object: DialogeInterface {
                override fun clickedBol(bol: Boolean) {
                    if (bol){
                        var dialog= showProgressDialog(context,"Sending...")
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                customerViewModel.deleteAllOffers()
                                customerViewModel.updateDriverAvailableNode(true,data.uid!!)
                                customerViewModel.deleteRideRequestFromDriver(data.uid!!) // delete ride requests from driver

                                val result=customerViewModel.uploadAcceptModel(AcceptModel(driverUid = data.uid!!, start = false, customerUid = ""))
                                PreferencesManager(context).putValue(DRIVERUID,data.uid!!)

                                sendApprovedNotification("Pak Drive request accepted","Your request has been accepted by the customer. Please proceed to the pickup point. Click on it for more details.",data.driverFCMToken,"true")
                                resultChecker(result,context)
                                context.startActivity(Intent(context,LiveDriverViewActivity::class.java))
                                context.finish()

                            } catch (e: Exception) {
                                resultChecker(MyResult.Error(e.message ?: "Unknown error"), context)
                            } finally {
                                dismissProgressDialog(dialog)
                            }
                        }
                    }
                }
            },"Do you want to accept this ride?")

        }

    }
    override fun getItemCount() = requestList.size
}