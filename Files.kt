package com.lontum.repairtracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** CSV export (opens in Excel / WPS) plus backup and restore of the whole database. */
object Files {

    private fun csv(s: String): String {
        val clean = s.replace("\"", "\"\"")
        return "\"$clean\""
    }

    fun shareCsv(ctx: Context) {
        val jobs = Db.get(ctx).all().sortedBy { it.jobNo }
        val sb = StringBuilder()
        sb.append("Job No,Date In,Customer,Phone,Device,Fault,Status,Quote (FCFA),Paid (FCFA),")
        sb.append("Balance (FCFA),Parts Cost (FCFA),Profit (FCFA),Date Out,Warranty Until,Notes\n")
        for (j in jobs) {
            sb.append(j.jobNo).append(',')
                .append(csv(Fmt.date(j.dateIn))).append(',')
                .append(csv(j.customer)).append(',')
                .append(csv(j.phone)).append(',')
                .append(csv(j.device)).append(',')
                .append(csv(j.fault)).append(',')
                .append(csv(j.status)).append(',')
                .append(j.quote?.toString() ?: "").append(',')
                .append(j.paid?.toString() ?: "").append(',')
                .append(j.balance?.toString() ?: "").append(',')
                .append(j.partsCost?.toString() ?: "").append(',')
                .append(j.profit?.toString() ?: "").append(',')
                .append(csv(Fmt.date(j.dateOut))).append(',')
                .append(csv(Fmt.date(j.warrantyUntil))).append(',')
                .append(csv(j.notes)).append('\n')
        }
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val f = File(dir, "repair_jobs.csv")
        f.writeText(sb.toString())
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Repair jobs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Send jobs list"))
    }

    /** Copies the database to a file the user chose (Google Drive, Downloads, WhatsApp...). */
    fun backupTo(ctx: Context, target: Uri) {
        Db.closeAll()
        val dbFile = ctx.getDatabasePath(Db.NAME)
        ctx.contentResolver.openOutputStream(target)?.use { out ->
            dbFile.inputStream().use { it.copyTo(out) }
        } ?: throw IllegalStateException("Could not write the backup file")
    }

    /** Replaces all jobs with the ones in a backup file. */
    fun restoreFrom(ctx: Context, source: Uri) {
        val tmp = File(ctx.cacheDir, "restore.db")
        ctx.contentResolver.openInputStream(source)?.use { input ->
            tmp.outputStream().use { input.copyTo(it) }
        } ?: throw IllegalStateException("Could not read the backup file")

        val header = tmp.inputStream().use { s ->
            val b = ByteArray(16)
            val n = s.read(b)
            if (n < 15) "" else String(b, 0, 15, Charsets.US_ASCII)
        }
        if (header != "SQLite format 3") {
            tmp.delete()
            throw IllegalArgumentException("This is not a Repair Tracker backup file")
        }

        Db.closeAll()
        val dbFile = ctx.getDatabasePath(Db.NAME)
        File(dbFile.path + "-wal").delete()
        File(dbFile.path + "-shm").delete()
        File(dbFile.path + "-journal").delete()
        tmp.copyTo(dbFile, overwrite = true)
        tmp.delete()
    }
}
