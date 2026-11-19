# Repair Tracker – Lontum's Phone Repair

An Android app for tracking phone repair jobs. It works offline, and the jobs are saved on the phone.

## What it does
- Records each phone that comes in: job number, date in, customer, phone number, device, fault, status, quote, paid, parts cost, date out, warranty and notes.
- Works out the balance to pay, the profit on each job and the warranty end date.
- The coloured status goes from Received to Diagnosing, Waiting for parts, In repair, Ready, Collected or Not repairable. Setting a job to Collected fills in today's date as the date out.
- The top bar shows open jobs, phones that are ready, money owed to you and this month's profit. Tap it to see the full summary and profit for each of the last 6 months.
- Lets you search by name, phone number, device or fault, and filter by status.
- Each job card has Call and WhatsApp buttons. The WhatsApp message is filled in to match the status. For example, when a phone is Ready the message tells the customer it is ready and how much is left to pay. 9-digit Cameroon numbers get +237 added.
- The menu has three options:
  - Export to Excel (CSV) sends the job list to WhatsApp, Gmail or Drive, and it opens in Excel or WPS.
  - Backup jobs saves all jobs to a file. Keep it on Google Drive.
  - Restore from backup loads the jobs back, for example on a new phone.

## Building with GitHub
Push or upload this project to a GitHub repository. The **Build APK** workflow runs on its own and does two things:
- It publishes the APK on the repository's **Releases** page, which is the easiest place to download it on your phone.
- It also saves the APK under **Actions → the latest run → Artifacts**.

Every build is signed with the same key (`app/repairtracker.keystore`). That means a new APK installs as an update and your jobs are kept. Do not delete or change that file.
