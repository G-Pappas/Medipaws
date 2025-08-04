package com.example.medipaws

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.content.Intent
import androidx.core.content.FileProvider
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
        
        setupReminderTimeSpinner(view)
        setupSnoozeIntervalSpinner(view)
        setupExportButton(view)
        
        return view
    }
    
    private fun setupReminderTimeSpinner(view: View) {
        val spinner = view.findViewById<Spinner>(R.id.spinnerReminderTime)
        val reminderOptions = resources.getStringArray(R.array.reminder_times)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, reminderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedIndex = prefs.getInt("reminder_time_index", 0)
        spinner.setSelection(savedIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("reminder_time_index", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun setupSnoozeIntervalSpinner(view: View) {
        val snoozeSpinner = view.findViewById<Spinner>(R.id.spinnerSnoozeInterval)
        val snoozeOptions = resources.getStringArray(R.array.snooze_intervals)
        val snoozeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, snoozeOptions)
        snoozeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        snoozeSpinner.adapter = snoozeAdapter

        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedSnoozeIndex = prefs.getInt("snooze_interval_index", 0)
        snoozeSpinner.setSelection(savedSnoozeIndex)

        snoozeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("snooze_interval_index", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    
    private fun setupExportButton(view: View) {
        val exportButton = view.findViewById<Button>(R.id.exportButton)
        exportButton.setOnClickListener {
            showExportDialog()
        }
    }
    
    private fun showExportDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_export_options, null)
        
        val includeCompletedCheckBox = dialogView.findViewById<CheckBox>(R.id.includeCompletedCheckBox)
        val dateRangeCheckBox = dialogView.findViewById<CheckBox>(R.id.dateRangeCheckBox)
        val startDatePicker = dialogView.findViewById<DatePicker>(R.id.startDatePicker)
        val endDatePicker = dialogView.findViewById<DatePicker>(R.id.endDatePicker)
        
        // Set default dates (last 30 days)
        val calendar = Calendar.getInstance()
        endDatePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null)
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        startDatePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), null)
        
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Export Options")
            .setView(dialogView)
            .setPositiveButton("Export") { _, _ ->
                performExport(
                    includeCompleted = includeCompletedCheckBox.isChecked,
                    useDateRange = dateRangeCheckBox.isChecked,
                    startDate = if (dateRangeCheckBox.isChecked) {
                        val cal = Calendar.getInstance()
                        cal.set(startDatePicker.year, startDatePicker.month, startDatePicker.dayOfMonth)
                        cal.time
                    } else null,
                    endDate = if (dateRangeCheckBox.isChecked) {
                        val cal = Calendar.getInstance()
                        cal.set(endDatePicker.year, endDatePicker.month, endDatePicker.dayOfMonth, 23, 59, 59)
                        cal.time
                    } else null
                )
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    private fun performExport(
        includeCompleted: Boolean,
        useDateRange: Boolean,
        startDate: Date?,
        endDate: Date?
    ) {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(requireContext())
                val pets = db.petDao().getAllPets().first()
                val entries = db.medicineEntryDao().getAllEntries().first()
                
                val pdfService = PdfExportService(requireContext())
                val pdfFile = pdfService.exportMedicineReport(
                    pets = pets,
                    entries = entries,
                    startDate = if (useDateRange) startDate else null,
                    endDate = if (useDateRange) endDate else null,
                    includeCompleted = includeCompleted
                )
                
                sharePdfFile(pdfFile)
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun sharePdfFile(pdfFile: java.io.File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            pdfFile
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "MediPaws Medicine Report")
            putExtra(Intent.EXTRA_TEXT, "Please find attached the medicine report from MediPaws app.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        startActivity(Intent.createChooser(shareIntent, "Share PDF Report"))
        Toast.makeText(requireContext(), "PDF exported successfully!", Toast.LENGTH_SHORT).show()
    }
} 