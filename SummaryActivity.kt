package com.lontum.repairtracker

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/** Shop totals, same as the Summary tab of the spreadsheet, plus profit for the last 6 months. */
class SummaryActivity : AppCompatActivity() {

    private lateinit var box: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Shop summary"

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(ContextCompat.getColor(this, R.color.card))
        box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }
        scroll.addView(box)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun heading(t: String) {
        box.addView(TextView(this).apply {
            text = t
            setTextColor(ContextCompat.getColor(this@SummaryActivity, R.color.primary))
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(20), 0, dp(6))
        })
    }

    private fun row(label: String, value: String, colorRes: Int = R.color.text_main) {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        line.addView(TextView(this).apply {
            text = label
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@SummaryActivity, R.color.text_main))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        line.addView(TextView(this).apply {
            text = value
            textSize = 15f
            gravity = Gravity.END
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@SummaryActivity, colorRes))
        })
        box.addView(line)
    }

    private fun build() {
        box.removeAllViews()
        val jobs = Db.get(this).all()

        heading("JOBS")
        row("Total jobs", jobs.size.toString())
        row("Jobs still open", jobs.count { it.isOpen }.toString())
        row("Ready, not collected", jobs.count { it.status == Status.READY.label }.toString())
        row("Waiting for parts", jobs.count { it.status == Status.WAITING_PARTS.label }.toString())
        row("Not repairable", jobs.count { it.status == Status.NOT_REPAIRABLE.label }.toString())

        heading("MONEY (ALL JOBS)")
        row("Total quoted", Fmt.money(jobs.sumOf { it.quote ?: 0L }))
        row("Money received", Fmt.money(jobs.sumOf { it.paid ?: 0L }), R.color.profit)
        row("Balance owed by customers", Fmt.money(jobs.sumOf { maxOf(it.balance ?: 0L, 0L) }), R.color.owed)
        row("Parts cost", Fmt.money(jobs.sumOf { it.partsCost ?: 0L }))
        row("Profit", Fmt.money(jobs.sumOf { it.profit ?: 0L }), R.color.profit)

        heading("PROFIT BY MONTH (BY DATE IN)")
        for (i in 0 until 6) {
            val start = Fmt.startOfMonth(i)
            val end = if (i == 0) Long.MAX_VALUE else Fmt.startOfMonth(i - 1)
            val inMonth = jobs.filter { it.dateIn in start until end }
            row(
                "${Fmt.monthName(start)}  (${inMonth.size} jobs)",
                Fmt.money(inMonth.sumOf { it.profit ?: 0L }),
                R.color.profit
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
