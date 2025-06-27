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

        petRegistrationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // After registration, reload or refresh UI as needed
            checkAndShowRegistration()
        }
        checkAndShowRegistration()

        val entriesRecyclerView = findViewById<RecyclerView>(R.id.entriesRecyclerView)
        val addEntryFab = findViewById<FloatingActionButton>(R.id.addEntryFab)
        val bottomNavigationView = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigationView)

        adapter = MedicineEntryAdapter(
            emptyList(),
            onLongPress = { entry ->
                // Show edit dialog
                showEntryDialog(entry)
            }
        )
        entriesRecyclerView.layoutManager = LinearLayoutManager(this)
        entriesRecyclerView.adapter = adapter

        bottomNavigationView.setOnItemSelectedListener { item ->
            currentFilter = when (item.itemId) {
                R.id.nav_medicine -> "Medicine"
                R.id.nav_treatment -> "Treatment"
                else -> "Medicine"
            }
            filterAndShowEntries(currentFilter)
            true
        }
        // Set default selected tab
        bottomNavigationView.selectedItemId = R.id.nav_medicine

        viewModel.entries.observe(this, Observer { entries ->
            filterAndShowEntries(currentFilter, entries)
        })

        addEntryFab.setOnClickListener {
            showEntryDialog(null)
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

    private fun showEntryDialog(entry: MedicineEntry? = null) {
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
        }
        radioTypeGroup.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        switchNotification.setOnCheckedChangeListener { _, _ -> updateVisibility() }
        updateVisibility()

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
            // Duration cannot be inferred from a single entry, so leave as default
        } else {
            // Set default start date to today
            inputStartDate.setText(dateFormat.format(java.util.Date()))
            // Set default type based on current filter
            if (currentFilter == "Treatment") {
                radioTreatment.isChecked = true
            } else {
                radioMedicine.isChecked = true
            }
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
                            for (dt in dateTimeList) {
                                val newEntry = MedicineEntry(
                                    name = name,
                                    dose = dose,
                                    dateTime = dt,
                                    notes = notes,
                                    type = type
                                )
                                viewModel.insert(newEntry)
                                scheduleNotification(newEntry)
                            }
                        } else {
                            val newEntry = MedicineEntry(
                                name = name,
                                dose = dose,
                                dateTime = java.util.Date(),
                                notes = notes,
                                type = type
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
                            type = type
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
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("name", entry.name)
            putExtra("dose", entry.dose)
            putExtra("notes", entry.notes ?: "")
            putExtra("id", entry.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.id.toInt(),
            intent,
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