package com.lontum.repairtracker

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class JobAdapter(
    private val onOpen: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.VH>() {

    private var items: List<Job> = emptyList()

    fun submit(list: List<Job>) {
        items = list
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.title)
        val status: TextView = v.findViewById(R.id.status)
        val device: TextView = v.findViewById(R.id.device)
        val money: TextView = v.findViewById(R.id.money)
        val date: TextView = v.findViewById(R.id.date)
        val call: MaterialButton = v.findViewById(R.id.call)
        val whatsapp: MaterialButton = v.findViewById(R.id.whatsapp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_job, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val j = items[position]
        val ctx = h.itemView.context

        h.title.text = "#${j.jobNo}  ${j.customer.ifBlank { "(no name)" }}"
        h.device.text = listOf(j.device, j.fault).filter { it.isNotBlank() }.joinToString(" — ")

        h.status.text = j.status
        h.status.background = GradientDrawable().apply {
            cornerRadius = 40f
            setColor(Status.colorOf(j.status))
        }

        val bal = j.balance
        when {
            bal == null -> {
                h.money.text = "No quote yet"
                h.money.setTextColor(ContextCompat.getColor(ctx, R.color.text_soft))
            }
            bal > 0 -> {
                h.money.text = "Owes ${Fmt.money(bal)}"
                h.money.setTextColor(ContextCompat.getColor(ctx, R.color.owed))
            }
            else -> {
                h.money.text = "Paid in full · ${Fmt.money(j.quote)}"
                h.money.setTextColor(ContextCompat.getColor(ctx, R.color.profit))
            }
        }

        h.date.text = if (j.dateOut != 0L) "Out ${Fmt.date(j.dateOut)}" else "In ${Fmt.date(j.dateIn)}"

        h.itemView.setOnClickListener { onOpen(j) }
        h.call.setOnClickListener { Contact.call(ctx, j.phone) }
        h.whatsapp.setOnClickListener { Contact.whatsApp(ctx, j) }
    }
}
