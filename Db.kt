package com.lontum.repairtracker

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class Db private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    companion object {
        const val NAME = "repairtracker.db"
        private const val VERSION = 1
        private const val T = "jobs"

        @Volatile private var instance: Db? = null

        fun get(context: Context): Db =
            instance ?: synchronized(this) {
                instance ?: Db(context).also { instance = it }
            }

        /** Closes the database so its file can be replaced by a backup. */
        fun closeAll() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $T (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                job_no INTEGER NOT NULL,
                date_in INTEGER NOT NULL,
                customer TEXT NOT NULL,
                phone TEXT NOT NULL,
                device TEXT NOT NULL,
                fault TEXT NOT NULL,
                status TEXT NOT NULL,
                quote INTEGER,
                paid INTEGER,
                parts INTEGER,
                date_out INTEGER NOT NULL DEFAULT 0,
                warranty_days INTEGER NOT NULL DEFAULT 30,
                notes TEXT NOT NULL DEFAULT ''
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future versions add columns here without deleting jobs.
    }

    private fun values(j: Job) = ContentValues().apply {
        put("job_no", j.jobNo)
        put("date_in", j.dateIn)
        put("customer", j.customer)
        put("phone", j.phone)
        put("device", j.device)
        put("fault", j.fault)
        put("status", j.status)
        if (j.quote == null) putNull("quote") else put("quote", j.quote)
        if (j.paid == null) putNull("paid") else put("paid", j.paid)
        if (j.partsCost == null) putNull("parts") else put("parts", j.partsCost)
        put("date_out", j.dateOut)
        put("warranty_days", j.warrantyDays)
        put("notes", j.notes)
    }

    private fun Cursor.longOrNull(col: String): Long? {
        val i = getColumnIndexOrThrow(col)
        return if (isNull(i)) null else getLong(i)
    }

    private fun Cursor.toJob() = Job(
        id = getLong(getColumnIndexOrThrow("id")),
        jobNo = getInt(getColumnIndexOrThrow("job_no")),
        dateIn = getLong(getColumnIndexOrThrow("date_in")),
        customer = getString(getColumnIndexOrThrow("customer")),
        phone = getString(getColumnIndexOrThrow("phone")),
        device = getString(getColumnIndexOrThrow("device")),
        fault = getString(getColumnIndexOrThrow("fault")),
        status = getString(getColumnIndexOrThrow("status")),
        quote = longOrNull("quote"),
        paid = longOrNull("paid"),
        partsCost = longOrNull("parts"),
        dateOut = getLong(getColumnIndexOrThrow("date_out")),
        warrantyDays = getInt(getColumnIndexOrThrow("warranty_days")),
        notes = getString(getColumnIndexOrThrow("notes"))
    )

    fun all(): List<Job> {
        val list = ArrayList<Job>()
        readableDatabase.query(T, null, null, null, null, null, "job_no DESC").use { c ->
            while (c.moveToNext()) list.add(c.toJob())
        }
        return list
    }

    fun byId(id: Long): Job? =
        readableDatabase.query(T, null, "id=?", arrayOf(id.toString()), null, null, null).use { c ->
            if (c.moveToFirst()) c.toJob() else null
        }

    fun nextJobNo(): Int =
        readableDatabase.rawQuery("SELECT COALESCE(MAX(job_no), 0) + 1 FROM $T", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 1
        }

    fun save(j: Job): Long {
        return if (j.id == 0L) {
            val id = writableDatabase.insert(T, null, values(j))
            j.id = id
            id
        } else {
            writableDatabase.update(T, values(j), "id=?", arrayOf(j.id.toString()))
            j.id
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete(T, "id=?", arrayOf(id.toString()))
    }
}
