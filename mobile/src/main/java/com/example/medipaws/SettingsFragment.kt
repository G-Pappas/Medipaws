package com.example.medipaws

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
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

        val snoozeSpinner = view.findViewById<Spinner>(R.id.spinnerSnoozeInterval)
        val snoozeOptions = resources.getStringArray(R.array.snooze_intervals)
        val snoozeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, snoozeOptions)
        snoozeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        snoozeSpinner.adapter = snoozeAdapter

        val savedSnoozeIndex = prefs.getInt("snooze_interval_index", 0)
        snoozeSpinner.setSelection(savedSnoozeIndex)

        snoozeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("snooze_interval_index", position).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        return view
    }
} 