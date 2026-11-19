package com.lontum.repairtracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class JobEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "job_id"
    }

    private lateinit var job: Job
    private lateinit var jobNo: TextInputEditText
    private lateinit var dateIn: TextInputEditText
    private lateinit var customer: TextInputEditText
    private lateinit var phone: TextInputEditText
    private lateinit var device: TextInputEditText
    private lateinit var fault: TextInputEditText
    private lateinit var status: Spinner
    private lateinit var quote: TextInputEditText
    private lateinit var paid: TextInputEditText
    private lateinit var parts: TextInputEditText
    private lateinit var dateOut: TextInputEditText
    private lateinit var warranty: TextInputEditText
    private lateinit var notes: TextInputEditText
    private lateinit var calc: TextView

    private var dateInMs: Long = 0
    private var dateOutMs: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val db = Db.get(this)
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        job = (if (id != 0L) db.byId(id) else null) ?: Job(jobNo = db.nextJobNo())
        supportActionBar?.title = if (job.id == 0L) "New job" else "Job #${job.jobNo}"

        jobNo = findViewById(R.id.jobNo)
        dateIn = findViewById(R.id.dateIn)
        customer = findViewById(R.id.customer)
        phone = findViewById(R.id.phone)
        device = findViewById(R.id.device)
        fault = findViewById(R.id.fault)
        status = findViewById(R.id.status)
        quote = findViewById(R.id.quote)
        paid = findViewById(R.id.paid)
        parts = findViewById(R.id.parts)
        dateOut = findViewById(R.id.dateOut)
        warranty = findViewById(R.id.warranty)
        notes = findViewById(R.id.notes)
        calc = findViewById(R.id.calc)

        status.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Status.labels)

        // Fill the form
        jobNo.setText(job.jobNo.toString())
        dateInMs = job.dateIn
        dateOutMs = job.dateOut
        dateIn.setText(Fmt.date(dateInMs))
        dateOut.setText(Fmt.date(dateOutMs))
        customer.setText(job.customer)
        phone.setText(job.phone)
        device.setText(job.device)
        fault.setText(job.fault)
        status.setSelection(Status.labels.indexOf(job.status).coerceAtLeast(0))
        quote.setText(job.quote?.toString() ?: "")
        paid.setText(job.paid?.toString() ?: "")
        parts.setText(job.partsCost?.toString() ?: "")
        warranty.setText(job.warrantyDays.toString())
        notes.setText(job.notes)

        dateIn.setOnClickListener {
            pickDate(dateInMs) { ms ->
                dateInMs = ms
                dateIn.setText(Fmt.date(ms))
            }
        }
        dateOut.setOnClickListener {
            pickDate(if (dateOutMs == 0L) Fmt.today() else dateOutMs) { ms ->
                dateOutMs = ms
                dateOut.setText(Fmt.date(ms))
                updateCalc()
            }
        }
        dateOut.setOnLongClickListener {
            dateOutMs = 0
            dateOut.setText("")
            updateCalc()
            Toast.makeText(this, "Date out cleared", Toast.LENGTH_SHORT).show()
            true
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updateCalc()
        }
        quote.addTextChangedListener(watcher)
        paid.addTextChangedListener(watcher)
        parts.addTextChangedListener(watcher)
        warranty.addTextChangedListener(watcher)

        status.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                // When the phone is collected, fill in today's date as date out
                if (Status.labels[pos] == Status.COLLECTED.label && dateOutMs == 0L) {
                    dateOutMs = Fmt.today()
                    dateOut.setText(Fmt.date(dateOutMs))
                }
                updateCalc()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        findViewById<MaterialButton>(R.id.save).setOnClickListener { save() }
        updateCalc()
    }

    private fun pickDate(start: Long, done: (Long) -> Unit) {
        val c = Calendar.getInstance()
        c.timeInMillis = if (start == 0L) Fmt.today() else start
        DatePickerDialog(this, { _, y, m, d ->
            val r = Calendar.getInstance()
            r.clear()
            r.set(y, m, d)
            done(Fmt.startOfDay(r.timeInMillis))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    /** Reads the form into the job (without saving). */
    private fun readForm() {
        job.jobNo = jobNo.text.toString().trim().toIntOrNull() ?: job.jobNo
        job.dateIn = if (dateInMs == 0L) Fmt.today() else dateInMs
        job.customer = customer.text.toString().trim()
        job.phone = phone.text.toString().trim()
        job.device = device.text.toString().trim()
        job.fault = fault.text.toString().trim()
        job.status = Status.labels.getOrElse(status.selectedItemPosition) { Status.RECEIVED.label }
        job.quote = Fmt.parseMoney(quote.text.toString())
        job.paid = Fmt.parseMoney(paid.text.toString())
        job.partsCost = Fmt.parseMoney(parts.text.toString())
        job.dateOut = dateOutMs
        job.warrantyDays = warranty.text.toString().trim().toIntOrNull() ?: 30
        job.notes = notes.text.toString().trim()
    }

    private fun updateCalc() {
        readForm()
        val lines = ArrayList<String>()
        lines.add("Balance to pay: " + Fmt.money(job.balance))
        lines.add("Profit on this job: " + Fmt.money(job.profit))
        lines.add(
            "Warranty until: " +
                if (job.warrantyUntil == 0L) "(set date out)" else Fmt.date(job.warrantyUntil)
        )
        calc.text = lines.joinToString("\n")
    }

    private fun save() {
        readForm()
        if (job.customer.isBlank() && job.device.isBlank()) {
            Toast.makeText(this, "Enter at least the customer name or the device", Toast.LENGTH_LONG).show()
            return
        }
        Db.get(this).save(job)
        Toast.makeText(this, "Job #${job.jobNo} saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_menu, menu)
        menu.findItem(R.id.menu_delete)?.isVisible = job.id != 0L
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.menu_whatsapp -> {
                readForm()
                Contact.whatsApp(this, job)
            }
            R.id.menu_delete -> {
                AlertDialog.Builder(this)
                    .setTitle("Delete job #${job.jobNo}?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete") { _, _ ->
                        Db.get(this).delete(job.id)
                        finish()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
