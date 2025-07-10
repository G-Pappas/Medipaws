package com.example.medipaws

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MedicineEntryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).medicineEntryDao()
    val entries = dao.getAllEntries().asLiveData()

    fun insert(entry: MedicineEntry, onInserted: (Long) -> Unit) = viewModelScope.launch {
        val newId = dao.insert(entry)
        onInserted(newId)
    }

    fun insert(entry: MedicineEntry) = viewModelScope.launch {
        dao.insert(entry)
    }

    fun delete(entry: MedicineEntry) = viewModelScope.launch {
        dao.delete(entry)
    }

    fun update(entry: MedicineEntry) = viewModelScope.launch {
        dao.update(entry)
    }
}

class MedicineEntryViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicineEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MedicineEntryViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 