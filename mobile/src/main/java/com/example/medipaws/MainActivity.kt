package com.example.medipaws

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import java.util.UUID
import com.example.medipaws.EntryStatus

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: MedicineEntryAdapter
    private val viewModel: MedicineEntryViewModel by viewModels {
        MedicineEntryViewModelFactory(application)
    }
    private var currentFilter: String = "Medicine"
    private lateinit var petRegistrationLauncher: androidx.activity.result.ActivityResultLauncher<Intent>
    private var checkedPetsOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf("android.permission.POST_NOTIFICATIONS"), 1001)
            }
        }

        petRegistrationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkAndShowRegistration()
        }
        checkAndShowRegistration()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setupWithNavController(navController)

        val addEntryFab = findViewById<FloatingActionButton>(R.id.addEntryFab)
        addEntryFab.setOnClickListener {
            // Show the add entry dialog for the current fragment if possible
            val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
            if (currentFragment is EntriesFragment) {
                (currentFragment.activity as? MainActivity)?.showEntryDialog(null)
            } else if (currentFragment is CalendarFragment) {
                (currentFragment.activity as? MainActivity)?.showEntryDialog(null)
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_medicine -> {
                    currentFilter = "Medicine"
                    addEntryFab.show()
                }
                R.id.nav_treatment -> {
                    currentFilter = "Treatment"
                    addEntryFab.show()
                }
                else -> addEntryFab.hide()
            }
        }
    }

    private fun checkAndShowRegistration() {
        CoroutineScope(Dispatchers.IO).launch {
            val petDao = AppDatabase.getDatabase(applicationContext).petDao()
            val pets = petDao.getAllPets().first()
            if (pets.isEmpty()) {
                runOnUiThread {
                    val intent = Intent(this@MainActivity, PetRegistrationActivity::class.java)
                    petRegistrationLauncher.launch(intent)
                }
            }
        }
    }

    fun showEntryDialog(
        entry: MedicineEntry? = null,
        preselectedDate: java.util.Date? = null,
        preselectNotification: Boolean = false
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_entry, null)
        val radioTypeGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioTypeGroup)
        val radioMedicine = dialogView.findViewById<android.widget.RadioButton>(R.id.radioMedicine)
        val radioTreatment = dialogView.findViewById<android.widget.RadioButton>(R.id.radioTreatment)
        val inputDose = dialogView.findViewById<EditText>(R.id.inputDose)
        val spinnerDoseUnit = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerDoseUnit)
        val switchNotification = dialogView.findViewById<android.widget.Switch>(R.id.switchNotification)
        val layoutDuration = dialogView.findViewById<View>(R.id.layoutDuration)
        val buttonDecrementDuration = dialogView.findViewById<android.widget.Button>(R.id.buttonDecrementDuration)
        val buttonIncrementDuration = dialogView.findViewById<android.widget.Button>(R.id.buttonIncrementDuration)
        val textDurationValue = dialogView.findViewById<android.widget.TextView>(R.id.textDurationValue)
        val inputStartDate = dialogView.findViewById<EditText>(R.id.inputStartDate)
        val inputAlertTime = dialogView.findViewById<EditText>(R.id.inputAlertTime)
        val inputNotes = dialogView.findViewById<EditText>(R.id.inputNotes)
        val inputName = dialogView.findViewById<EditText>(R.id.inputName)
        val layoutDose = dialogView.findViewById<View>(R.id.layoutDose)
        val layoutRepeatSwitch = dialogView.findViewById<View>(R.id.layoutRepeatSwitch)
        val switchRepeat = dialogView.findViewById<android.widget.Switch>(R.id.switchRepeat)
        val layoutRepeat = dialogView.findViewById<View>(R.id.layoutRepeat)
        val spinnerRepeatUnit = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerRepeatUnit)
        val buttonDecrementRepeat = dialogView.findViewById<android.widget.Button>(R.id.buttonDecrementRepeat)
        val buttonIncrementRepeat = dialogView.findViewById<android.widget.Button>(R.id.buttonIncrementRepeat)
        val textRepeatValue = dialogView.findViewById<android.widget.TextView>(R.id.textRepeatValue)
        val inputRepeatUntil = dialogView.findViewById<EditText>(R.id.inputRepeatUntil)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        var durationValue = 1
        textDurationValue.text = durationValue.toString()
        buttonDecrementDuration.setOnClickListener {
            if (durationValue > 1) {
                durationValue--
                textDurationValue.text = durationValue.toString()
            }
        }
        buttonIncrementDuration.setOnClickListener {
            durationValue++
            textDurationValue.text = durationValue.toString()
        }
        // Show/hide fields based on type and notification
        fun updateVisibility() {
            val isMedicine = radioMedicine.isChecked
            layoutDose.visibility = if (isMedicine) View.VISIBLE else View.GONE
            switchNotification.visibility = View.VISIBLE
            val notify = switchNotification.isChecked
            layoutDuration.visibility = if (notify) View.VISIBLE else View.GONE
            inputStartDate.visibility = if (notify) View.VISIBLE else View.GONE
            inputAlertTime.visibility = if (notify) View.VISIBLE else View.GONE
            layoutRepeatSwitch.visibility = if (notify) View.VISIBLE else View.GONE
            layoutRepeat.visibility = if (notify && switchRepeat.isChecked) View.VISIBLE else View.GONE
            inputRepeatUntil.visibility = if (notify && switchRepeat.isChecked) View.VISIBLE else View.GONE
        }
        radioTypeGroup.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        switchNotification.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        switchRepeat.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        updateVisibility()

        var repeatValue = 1
        textRepeatValue.text = repeatValue.toString()
        buttonDecrementRepeat.setOnClickListener {
            if (repeatValue > 1) {
                repeatValue--
                textRepeatValue.text = repeatValue.toString()
        }
        }
        buttonIncrementRepeat.setOnClickListener {
            repeatValue++
            textRepeatValue.text = repeatValue.toString()
        }
        // If editing, pre-fill fields
        if (entry != null) {
            // For now, assume all entries are Medicine (for backward compatibility)
            radioMedicine.isChecked = entry.type == "Medicine"
            radioTreatment.isChecked = entry.type == "Treatment"
            inputName.setText(entry.name)
            // Split dose into value and unit if possible
            val doseParts = entry.dose.split(" ", limit = 2)
            inputDose.setText(doseParts.getOrNull(0) ?: "")
            val unit = doseParts.getOrNull(1) ?: ""
            val unitAdapter = spinnerDoseUnit.adapter
            if (unitAdapter != null) {
                for (i in 0 until unitAdapter.count) {
                    if (unitAdapter.getItem(i).toString().equals(unit, ignoreCase = true)) {
                        spinnerDoseUnit.setSelection(i)
                        break
                    }
                }
            }
            inputNotes.setText(entry.notes ?: "")
            // Pre-fill notification fields if entry has a future date
            inputStartDate.setText(dateFormat.format(entry.dateTime))
            inputAlertTime.setText(timeFormat.format(entry.dateTime))
            // Pre-fill notification switch based on entry
            switchNotification.isChecked = entry.notificationEnabled
            // Duration cannot be inferred from a single entry, so leave as default
            // Pre-fill interval fields
            switchRepeat.isChecked = entry.intervalValue > 0
            repeatValue = entry.intervalValue.takeIf { it > 0 } ?: 1
            textRepeatValue.text = repeatValue.toString()
            val repeatUnitAdapter = spinnerRepeatUnit.adapter
            if (repeatUnitAdapter != null) {
                for (i in 0 until repeatUnitAdapter.count) {
                    if (repeatUnitAdapter.getItem(i).toString().equals(entry.intervalUnit, ignoreCase = true)) {
                        spinnerRepeatUnit.setSelection(i)
                        break
                    }
                }
            }
            // Pre-fill repeat fields
            inputRepeatUntil.setText(entry.repeatUntil?.let { dateFormat.format(it) } ?: "")
        } else {
            // Set default start date to today or preselectedDate
            inputStartDate.setText(dateFormat.format(preselectedDate ?: java.util.Date()))
            // Set default type based on current filter
            if (currentFilter == "Treatment") {
                radioTreatment.isChecked = true
            } else {
                radioMedicine.isChecked = true
            }
            // Preselect notification if requested
            switchNotification.isChecked = preselectNotification
            spinnerRepeatUnit.setSelection(0)
            switchRepeat.isChecked = false
            repeatValue = 1
            textRepeatValue.text = repeatValue.toString()
            inputRepeatUntil.setText("")
        }

        inputStartDate.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                inputStartDate.setText(dateFormat.format(calendar.time))
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }
        inputAlertTime.setOnClickListener {
            val now = Calendar.getInstance()
            TimePickerDialog(this, { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                inputAlertTime.setText(String.format("%02d:%02d", hourOfDay, minute))
            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
        }
        inputRepeatUntil.setOnClickListener {
            val now = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, dayOfMonth ->
                now.set(Calendar.YEAR, year)
                now.set(Calendar.MONTH, month)
                now.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                inputRepeatUntil.setText(dateFormat.format(now.time))
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle(if (entry == null) "Add Entry" else "Edit Entry")
            .setView(dialogView)
            .setPositiveButton(if (entry == null) "Add" else "Save", null)
            .setNegativeButton("Cancel", null)

        if (entry != null) {
            dialogBuilder.setNeutralButton("Delete") { _, _ ->
                viewModel.delete(entry)
                Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = dialogBuilder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val isMedicine = radioMedicine.isChecked
                val type = if (isMedicine) "Medicine" else "Treatment"
                val name = inputName.text.toString().trim()
                val doseValue = inputDose.text.toString().trim()
                val doseUnit = spinnerDoseUnit.selectedItem?.toString()?.trim() ?: ""
                val dose = if (isMedicine && doseValue.isNotEmpty()) "$doseValue $doseUnit" else ""
                val notes = inputNotes.text.toString().trim().ifEmpty { null }
                val notify = switchNotification.isChecked
                val startDateStr = inputStartDate.text.toString().trim()
                val alertTimeStr = inputAlertTime.text.toString().trim()
                val intervalValue = textRepeatValue.text.toString().toIntOrNull() ?: 0
                val intervalUnit = spinnerRepeatUnit.selectedItem?.toString() ?: "hours"
                val repeatEnabled = switchRepeat.isChecked
                val repeatValueFinal = if (repeatEnabled) textRepeatValue.text.toString().toIntOrNull() ?: 1 else 0
                val repeatUnit = spinnerRepeatUnit.selectedItem?.toString() ?: "hours"
                val repeatUntilDate = inputRepeatUntil.text.toString().takeIf { it.isNotBlank() }?.let {
                    try { dateFormat.parse(it) } catch (e: Exception) { null }
                }

                // Validation
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter the name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (isMedicine && doseValue.isEmpty()) {
                    Toast.makeText(this, "Please enter the dose", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (notify) {
                    if (durationValue < 1 || startDateStr.isEmpty() || alertTimeStr.isEmpty()) {
                        Toast.makeText(this, "Please fill all notification fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                try {
                    val dateTimeList = mutableListOf<java.util.Date>()
                    if (notify) {
                        val days = try { durationValue } catch (e: Exception) { Log.e("AddEntryDialogError", "Invalid duration: $durationValue", e); throw e }
                        val startDate = try { dateFormat.parse(startDateStr) } catch (e: Exception) { Log.e("AddEntryDialogError", "Invalid start date: $startDateStr", e); throw e }
                        val alertTime = try { timeFormat.parse(alertTimeStr) } catch (e: Exception) { Log.e("AddEntryDialogError", "Invalid alert time: $alertTimeStr", e); throw e }
                        if (startDate == null || alertTime == null) throw Exception("Null date/time")
                        val cal = Calendar.getInstance()
                        cal.time = startDate
                        val alertCal = Calendar.getInstance()
                        alertCal.time = alertTime
                        for (i in 0 until days) {
                            val notifCal = cal.clone() as Calendar
                            notifCal.set(Calendar.HOUR_OF_DAY, alertCal.get(Calendar.HOUR_OF_DAY))
                            notifCal.set(Calendar.MINUTE, alertCal.get(Calendar.MINUTE))
                            notifCal.set(Calendar.SECOND, 0)
                            notifCal.set(Calendar.MILLISECOND, 0)
                            dateTimeList.add(notifCal.time)
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                    if (entry == null) {
                        if (notify && dateTimeList.isNotEmpty()) {
                            // If repeat is enabled and repeatUntil is set, generate all repeat dates
                            if (repeatEnabled && repeatUntilDate != null && repeatValueFinal > 0) {
                                val repeatDates = mutableListOf<java.util.Date>()
                                val cal = Calendar.getInstance()
                                cal.time = dateFormat.parse(startDateStr) ?: java.util.Date()
                                val alertCal = Calendar.getInstance()
                                alertCal.time = timeFormat.parse(alertTimeStr) ?: java.util.Date()
                                val newSeriesId = UUID.randomUUID().toString()
                                while (!cal.time.after(repeatUntilDate)) {
                                    val notifCal = cal.clone() as Calendar
                                    notifCal.set(Calendar.HOUR_OF_DAY, alertCal.get(Calendar.HOUR_OF_DAY))
                                    notifCal.set(Calendar.MINUTE, alertCal.get(Calendar.MINUTE))
                                    notifCal.set(Calendar.SECOND, 0)
                                    notifCal.set(Calendar.MILLISECOND, 0)
                                    repeatDates.add(notifCal.time)
                                    when (repeatUnit) {
                                        "days" -> cal.add(Calendar.DAY_OF_YEAR, repeatValueFinal)
                                        "hours" -> cal.add(Calendar.HOUR_OF_DAY, repeatValueFinal)
                                        "years" -> cal.add(Calendar.YEAR, repeatValueFinal)
                                        else -> cal.add(Calendar.DAY_OF_YEAR, repeatValueFinal)
                                    }
                                }
                                for (dt in repeatDates) {
                                    val newEntry = MedicineEntry(
                                        name = name,
                                        dose = dose,
                                        dateTime = dt,
                                        notes = notes,
                                        type = type,
                                        status = EntryStatus.PENDING,
                                        notificationEnabled = true,
                                        intervalValue = repeatValueFinal,
                                        intervalUnit = repeatUnit,
                                        repeatUntil = repeatUntilDate,
                                        seriesId = newSeriesId
                                    )
                                    viewModel.insert(newEntry)
                                    scheduleNotification(newEntry)
                                }
                            } else {
                            for (dt in dateTimeList) {
                                val newEntry = MedicineEntry(
                                    name = name,
                                    dose = dose,
                                    dateTime = dt,
                                    notes = notes,
                                        type = type,
                                        status = EntryStatus.PENDING,
                                        notificationEnabled = true,
                                        intervalValue = repeatValueFinal,
                                        intervalUnit = repeatUnit,
                                        repeatUntil = repeatUntilDate,
                                        seriesId = null
                                )
                                viewModel.insert(newEntry)
                                scheduleNotification(newEntry)
                                }
                            }
                        } else {
                            val newEntry = MedicineEntry(
                                name = name,
                                dose = dose,
                                dateTime = java.util.Date(),
                                notes = notes,
                                type = type,
                                status = EntryStatus.PENDING,
                                notificationEnabled = false,
                                intervalValue = repeatValueFinal,
                                intervalUnit = repeatUnit,
                                repeatUntil = repeatUntilDate,
                                seriesId = null
                            )
                            viewModel.insert(newEntry)
                        }
                        dialog.dismiss()
                        Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show()
                    } else {
                        val updatedEntry = entry.copy(
                            name = name,
                            dose = dose,
                            notes = notes,
                            type = type,
                            notificationEnabled = notify,
                            intervalValue = repeatValueFinal,
                            intervalUnit = repeatUnit,
                            repeatUntil = repeatUntilDate,
                            status = EntryStatus.PENDING // Preserve status
                        )
                        viewModel.update(updatedEntry)
                        if (notify && dateTimeList.isNotEmpty()) {
                            scheduleNotification(updatedEntry.copy(dateTime = dateTimeList[0]))
                        }
                        dialog.dismiss()
                        Toast.makeText(this, "Entry updated", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("AddEntryDialogError", "Exception in add/edit entry", e)
                    Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }

    private fun scheduleNotification(entry: MedicineEntry) {
        val context = this
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Toast.makeText(this, "Please allow exact alarms for reminders to work.", Toast.LENGTH_LONG).show()
                return
            }
        }
        val baseIntent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("name", entry.name)
            putExtra("dose", entry.dose)
            putExtra("notes", entry.notes ?: "")
            putExtra("id", entry.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.id.toInt(),
            baseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            entry.dateTime.time,
            pendingIntent
        )
    }

    private fun filterAndShowEntries(filter: String, entries: List<MedicineEntry>? = null) {
        val allEntries = entries ?: viewModel.entries.value ?: emptyList()
        val filtered = when (filter) {
            "Medicine" -> allEntries.filter { it.type == "Medicine" }
            "Treatment" -> allEntries.filter { it.type == "Treatment" }
            else -> allEntries
        }
        adapter.updateEntries(filtered)
    }
}