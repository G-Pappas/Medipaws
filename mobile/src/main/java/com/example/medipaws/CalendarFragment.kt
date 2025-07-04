package com.example.medipaws

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kizitonwose.calendar.core.*
import com.kizitonwose.calendar.view.CalendarView
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import java.time.ZoneId
import com.example.medipaws.EntryStatus

class CalendarFragment : Fragment() {

    private lateinit var calendarView: CalendarView
    private lateinit var monthTextView: TextView
    private lateinit var selectedDateLabel: TextView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var addEntryButton: Button
    private lateinit var prevMonthButton: ImageButton
    private lateinit var nextMonthButton: ImageButton
    private lateinit var adapter: MedicineEntryAdapter

    private val viewModel: MedicineEntryViewModel by activityViewModels {
        MedicineEntryViewModelFactory(requireActivity().application)
    }

    private var selectedDate: LocalDate? = null
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private var events = mapOf<LocalDate, List<MedicineEntry>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        calendarView = view.findViewById(R.id.calendarView)
        monthTextView = view.findViewById(R.id.monthTextView)
        selectedDateLabel = view.findViewById(R.id.selectedDateLabel)
        entriesRecyclerView = view.findViewById(R.id.entriesRecyclerView)
        addEntryButton = view.findViewById(R.id.addEntryButton)
        prevMonthButton = view.findViewById(R.id.prevMonthButton)
        nextMonthButton = view.findViewById(R.id.nextMonthButton)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        val firstDayOfWeek = firstDayOfWeekFromLocale()
        calendarView.setup(startMonth, endMonth, firstDayOfWeek)
        calendarView.scrollToMonth(currentMonth)

        prevMonthButton.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let {
                calendarView.smoothScrollToMonth(it.yearMonth.minusMonths(1))
            }
        }

        nextMonthButton.setOnClickListener {
            calendarView.findFirstVisibleMonth()?.let {
                calendarView.smoothScrollToMonth(it.yearMonth.plusMonths(1))
            }
        }

        calendarView.monthScrollListener = { month ->
            updateTitle(month.yearMonth)
        }

        calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, data: CalendarDay) {
                container.day = data
                val textView = container.textView
                val dotView = container.dotView

                textView.text = data.date.dayOfMonth.toString()

                if (data.position == DayPosition.MonthDate) {
                    textView.visibility = View.VISIBLE
                    if (data.date == selectedDate) {
                        textView.setBackgroundResource(R.drawable.selection_background)
                    } else {
                        textView.background = null
                    }

                    if (events[data.date].orEmpty().isNotEmpty()) {
                        dotView.visibility = View.VISIBLE
                    } else {
                        dotView.visibility = View.INVISIBLE
                    }
                } else {
                    textView.visibility = View.INVISIBLE
                    dotView.visibility = View.INVISIBLE
                }
            }
        }

        viewModel.entries.observe(viewLifecycleOwner) { allEntries ->
            events = allEntries.groupBy {
                it.dateTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            }
            calendarView.notifyCalendarChanged()
            updateEntryListForDate(selectedDate)
        }

        addEntryButton.setOnClickListener {
            (activity as? MainActivity)?.showEntryDialog(null)
        }

        // Initially select today
        selectDate(LocalDate.now())
    }

    private fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            val oldDate = selectedDate
            selectedDate = date
            oldDate?.let { calendarView.notifyDateChanged(it) }
            calendarView.notifyDateChanged(date)
            updateEntryListForDate(date)
        }
    }

    private fun updateTitle(yearMonth: YearMonth) {
        val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val year = yearMonth.year
        monthTextView.text = "$month $year"
    }

    private fun updateEntryListForDate(date: LocalDate?) {
        selectedDateLabel.text = if (date != null) "Entries for: ${dateFormatter.format(date)}" else "No date selected"
        adapter.updateEntries(events[date].orEmpty())
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

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        lateinit var day: CalendarDay
        val textView = view.findViewById<TextView>(R.id.calendarDayText)
        val dotView = view.findViewById<View>(R.id.calendarDayDot)

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    selectDate(day.date)
                }
            }
            view.setOnLongClickListener {
                if (day.position == DayPosition.MonthDate) {
                    val date = java.util.Date.from(day.date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                    (activity as? MainActivity)?.showEntryDialog(
                        entry = null,
                        preselectedDate = date,
                        preselectNotification = true
                    )
                    true
                } else {
                    false
                }
            }
        }
    }
} 