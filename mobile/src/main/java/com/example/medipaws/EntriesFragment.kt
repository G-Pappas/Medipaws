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

class EntriesFragment : Fragment() {

    private lateinit var adapter: MedicineEntryAdapter
    private val viewModel: MedicineEntryViewModel by activityViewModels {
        MedicineEntryViewModelFactory(requireActivity().application)
    }
    private val args: EntriesFragmentArgs by navArgs()
    private lateinit var entriesRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_entries, container, false)
        entriesRecyclerView = view.findViewById(R.id.entriesRecyclerView)
        setupRecyclerView()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                viewModel.update(entry.copy(taken = !entry.taken))
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