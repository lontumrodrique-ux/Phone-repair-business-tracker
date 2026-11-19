package com.lontum.repairtracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/** Calling and WhatsApp messages to customers. */
object Contact {

    /** Cameroon numbers: 9 digits get the 237 country code added. */
    fun internationalNumber(phone: String): String {
        var d = phone.filter { it.isDigit() }
        if (d.startsWith("00")) d = d.substring(2)
        if (d.length == 9) d = "237$d"
        return d
    }

    fun call(ctx: Context, phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(ctx, "No phone number for this customer", Toast.LENGTH_SHORT).show()
            return
        }
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim())))
    }

    fun messageFor(ctx: Context, j: Job): String {
        val shop = ctx.getString(R.string.shop_name)
        val name = j.customer.ifBlank { "customer" }
        val phoneName = j.device.ifBlank { "phone" }
        val bal = j.balance
        return when (j.status) {
            Status.READY.label -> {
                val money = if (bal != null && bal > 0)
                    " Balance to pay: ${Fmt.money(bal)}." else ""
                "Hello $name, your $phoneName is repaired and ready for collection at $shop.$money Thank you!"
            }
            Status.WAITING_PARTS.label ->
                "Hello $name, this is $shop. We are waiting for a part for your $phoneName. We will call you as soon as it arrives."
            Status.NOT_REPAIRABLE.label ->
                "Hello $name, this is $shop. Sorry, your $phoneName cannot be repaired. Please come and collect it."
            Status.COLLECTED.label -> {
                val w = if (j.warrantyUntil != 0L)
                    " Your warranty is valid until ${Fmt.date(j.warrantyUntil)}." else ""
                "Thank you $name for choosing $shop.$w"
            }
            else -> {
                val q = if (j.quote != null) " The repair cost is ${Fmt.money(j.quote)}." else ""
                "Hello $name, this is $shop about your $phoneName (job #${j.jobNo}).$q"
            }
        }
    }

    fun whatsApp(ctx: Context, j: Job) {
        val num = internationalNumber(j.phone)
        if (num.isBlank()) {
            Toast.makeText(ctx, "No phone number for this customer", Toast.LENGTH_SHORT).show()
            return
        }
        val url = "https://wa.me/$num?text=" + Uri.encode(messageFor(ctx, j))
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(ctx, "WhatsApp could not be opened", Toast.LENGTH_SHORT).show()
        }
    }
}
