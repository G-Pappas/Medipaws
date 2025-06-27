package com.example.medipaws

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "medicine_entries")
data class MedicineEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val dose: String,
    val dateTime: Date,
    val notes: String? = null,
    val type: String = "Medicine",
    val petId: Long? = null
) 