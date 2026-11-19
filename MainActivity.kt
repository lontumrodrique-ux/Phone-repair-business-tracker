package com.lontum.repairtracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: JobAdapter
    private lateinit var search: EditText
    private lateinit var filter: Spinner
    private lateinit var empty: TextView
    private var allJobs: List<Job> = emptyList()

    private val filterOptions: List<String> = listOf("All jobs", "Open jobs") + Status.labels

    private val backupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                try {
                    Files.backupTo(this, uri)
                    Toast.makeText(this, "Backup saved", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                reload()
            }
        }

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    Files.restoreFrom(this, uri)
                    Toast.makeText(this, "Jobs restored", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
                reload()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.shop_name)

        empty = findViewById(R.id.empty)
        search = findViewById(R.id.search)
        filter = findViewById(R.id.filter)

        adapter = JobAdapter { job ->
            startActivity(Intent(this, JobEditActivity::class.java).putExtra(JobEditActivity.EXTRA_ID, job.id))
        }
        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        filter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filterOptions)
        filter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = applyFilter()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = applyFilter()
        })

        findViewById<FloatingActionButton>(R.id.add).setOnClickListener {
            startActivity(Intent(this, JobEditActivity::class.java))
        }
        findViewById<View>(R.id.summaryBar).setOnClickListener {
            startActivity(Intent(this, SummaryActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        allJobs = Db.get(this).all()
        updateSummary()
        applyFilter()
    }

    private fun updateSummary() {
        val monthStart = Fmt.startOfMonth(0)
        findViewById<TextView>(R.id.statOpen).text = allJobs.count { it.isOpen }.toString()
        findViewById<TextView>(R.id.statReady).text =
            allJobs.count { it.status == Status.READY.label }.toString()
        findViewById<TextView>(R.id.statOwed).text =
            Fmt.money(allJobs.sumOf { maxOf(it.balance ?: 0L, 0L) }).replace(" FCFA", "")
        findViewById<TextView>(R.id.statProfit).text =
            Fmt.money(allJobs.filter { it.dateIn >= monthStart }.sumOf { it.profit ?: 0L }).replace(" FCFA", "")
    }

    private fun applyFilter() {
        val q = search.text.toString().trim().lowercase(Locale.ROOT)
        val choice = filterOptions.getOrElse(filter.selectedItemPosition) { "All jobs" }
        val shown = allJobs.filter { j ->
            val statusOk = when (choice) {
                "All jobs" -> true
                "Open jobs" -> j.isOpen
                else -> j.status == choice
            }
            val textOk = q.isEmpty() ||
                j.customer.lowercase(Locale.ROOT).contains(q) ||
                j.phone.contains(q) ||
                j.device.lowercase(Locale.ROOT).contains(q) ||
                j.fault.lowercase(Locale.ROOT).contains(q) ||
                j.jobNo.toString() == q
            statusOk && textOk
        }
        adapter.submit(shown)
        empty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        empty.text = if (allJobs.isEmpty()) "No jobs yet.\nTap + to record the first phone."
        else "No jobs match your search."
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_summary -> startActivity(Intent(this, SummaryActivity::class.java))
            R.id.menu_export -> {
                if (allJobs.isEmpty()) Toast.makeText(this, "No jobs to export", Toast.LENGTH_SHORT).show()
                else Files.shareCsv(this)
            }
            R.id.menu_backup -> {
                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                backupLauncher.launch("repair-tracker-backup-$stamp.db")
            }
            R.id.menu_restore -> {
                AlertDialog.Builder(this)
                    .setTitle("Restore from backup?")
                    .setMessage("All jobs now in the app will be replaced by the jobs in the backup file.")
                    .setPositiveButton("Choose file") { _, _ -> restoreLauncher.launch(arrayOf("*/*")) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
