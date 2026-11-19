package com.lontum.repairtracker

import android.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** One phone repair job. Dates are stored as milliseconds (0 = not set). Money is in FCFA. */
data class Job(
    var id: Long = 0,
    var jobNo: Int = 0,
    var dateIn: Long = Fmt.today(),
    var customer: String = "",
    var phone: String = "",
    var device: String = "",
    var fault: String = "",
    var status: String = Status.RECEIVED.label,
    var quote: Long? = null,
    var paid: Long? = null,
    var partsCost: Long? = null,
    var dateOut: Long = 0,
    var warrantyDays: Int = 30,
    var notes: String = ""
) {
    /** What the customer still owes. Null when there is no quote yet. */
    val balance: Long? get() = quote?.let { it - (paid ?: 0) }

    /** Quote minus parts cost. Null when there is no quote yet. */
    val profit: Long? get() = quote?.let { it - (partsCost ?: 0) }

    /** Last day of warranty, or 0 when the phone has not gone out yet. */
    val warrantyUntil: Long
        get() = if (dateOut == 0L) 0L else Fmt.addDays(dateOut, warrantyDays)

    val isOpen: Boolean
        get() = status != Status.COLLECTED.label && status != Status.NOT_REPAIRABLE.label
}

enum class Status(val label: String, val color: Int) {
    RECEIVED("Received", Color.parseColor("#9E9E9E")),
    DIAGNOSING("Diagnosing", Color.parseColor("#1E88E5")),
    WAITING_PARTS("Waiting for parts", Color.parseColor("#FB8C00")),
    IN_REPAIR("In repair", Color.parseColor("#8E24AA")),
    READY("Ready", Color.parseColor("#43A047")),
    COLLECTED("Collected", Color.parseColor("#00897B")),
    NOT_REPAIRABLE("Not repairable", Color.parseColor("#E53935"));

    companion object {
        val labels: List<String> = values().map { it.label }
        fun colorOf(label: String): Int =
            values().firstOrNull { it.label == label }?.color ?: Color.GRAY
    }
}

object Fmt {
    private val money = NumberFormat.getIntegerInstance(Locale.US)

    fun money(v: Long?): String = if (v == null) "-" else money.format(v) + " FCFA"

    fun date(ms: Long): String =
        if (ms == 0L) "" else SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(ms))

    fun today(): Long = startOfDay(System.currentTimeMillis())

    fun startOfDay(ms: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun addDays(ms: Long, days: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        c.add(Calendar.DAY_OF_MONTH, days)
        return c.timeInMillis
    }

    fun startOfMonth(monthsBack: Int = 0): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = today()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, -monthsBack)
        return c.timeInMillis
    }

    fun monthName(ms: Long): String =
        SimpleDateFormat("MMM yyyy", Locale.US).format(Date(ms))

    /** Reads a number typed by the user, ignoring spaces and commas. */
    fun parseMoney(s: String): Long? {
        val digits = s.replace(",", "").replace(" ", "").replace(".", "").trim()
        return if (digits.isEmpty()) null else digits.toLongOrNull()
    }
}
