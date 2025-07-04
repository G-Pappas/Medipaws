package com.example.medipaws

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medipaws.MedicineEntryViewModel
import com.example.medipaws.MedicineEntryViewModelFactory
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.app.AppCompatActivity
import com.example.medipaws.EntryStatus

class EntriesFragment : Fragment() {

    private lateinit var adapter: MedicineEntryAdapter
    private val viewModel: MedicineEntryViewModel by activityViewModels {
        MedicineEntryViewModelFactory(requireActivity().application)
    }
    private val args: EntriesFragmentArgs by navArgs()
    private lateinit var entriesRecyclerView: RecyclerView
    private var multiSelectMode = false
    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val view = inflater.inflate(R.layout.fragment_entries, container, false)
        entriesRecyclerView = view.findViewById(R.id.entriesRecyclerView)
        setupRecyclerView()
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.entries_menu, menu)
        this.menu = menu
        updateMenuVisibility()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        updateMenuVisibility()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_select -> {
                enterMultiSelectMode()
                return true
            }
            R.id.action_delete -> {
                confirmDeleteSelected()
                return true
            }
            R.id.action_cancel -> {
                exitMultiSelectMode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun enterMultiSelectMode() {
        multiSelectMode = true
        adapter.setMultiSelectMode(true)
        updateMenuVisibility()
    }

    private fun exitMultiSelectMode() {
        multiSelectMode = false
        adapter.setMultiSelectMode(false)
        updateMenuVisibility()
    }

    private fun updateMenuVisibility() {
        menu?.findItem(R.id.action_select)?.isVisible = !multiSelectMode
        menu?.findItem(R.id.action_delete)?.isVisible = multiSelectMode
        menu?.findItem(R.id.action_cancel)?.isVisible = multiSelectMode
    }

    private fun confirmDeleteSelected() {
        val selected = adapter.getSelectedEntries()
        if (selected.isEmpty()) return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete entries?")
            .setMessage("Are you sure you want to delete ${selected.size} entries?")
            .setPositiveButton("Delete") { _, _ ->
                selected.forEach { viewModel.delete(it) }
                exitMultiSelectMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.entriesToolbar)
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        viewModel.entries.observe(viewLifecycleOwner, Observer { entries ->
            filterAndShowEntries(args.filterType, entries)
        })
    }

    private fun setupRecyclerView() {
        adapter = MedicineEntryAdapter(
            emptyList(),
            onLongPress = { entry ->
                (activity as? MainActivity)?.showEntryDialog(entry)
            },
            onToggleTaken = { entry ->
                viewModel.update(entry)
            }
        )
        entriesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        entriesRecyclerView.adapter = adapter
    }

    private fun filterAndShowEntries(filter: String, entries: List<MedicineEntry>) {
        val filtered = when (filter) {
            "Medicine" -> entries.filter { it.type == "Medicine" }
            "Treatment" -> entries.filter { it.type == "Treatment" }
            else -> entries
        }
        adapter.updateEntries(filtered)
    }
} 