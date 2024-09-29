package com.subhajitrajak.nfcdemo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.subhajitrajak.nfcdemo.databinding.ItemPendingOrdersBinding

class PendingOrdersAdapter(
    private val pendingOrdersList: ArrayList<Order>
    ) : RecyclerView.Adapter<PendingOrdersAdapter.PendingOrdersViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingOrdersViewHolder {
        val binding = ItemPendingOrdersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingOrdersViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingOrdersViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pendingOrdersList.size

    inner class PendingOrdersViewHolder(private val binding: ItemPendingOrdersBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply {
                itemName.text = "Order: "+ pendingOrdersList[position].foodName
                itemPrice.text = "Payment To Receive: "+ pendingOrdersList[position].foodPrice!!.substring(7)
            }
        }

    }

}