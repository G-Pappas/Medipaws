package com.example.medipaws

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import androidx.room.TypeConverters

enum class EntryStatus { PENDING, DONE, LOST }

@Entity(tableName = "medicine_entries")
@TypeConverters(Converters::class)
data class MedicineEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val dose: String,
    val dateTime: Date,
    val notes: String? = null,
    val type: String = "Medicine",
    val petId: Long? = null,
    val status: EntryStatus = EntryStatus.PENDING,
    val notificationEnabled: Boolean = false,
    val intervalValue: Int = 0,
    val intervalUnit: String = "hours",
    val repeatUntil: Date? = null,
    val seriesId: String? = null
) 