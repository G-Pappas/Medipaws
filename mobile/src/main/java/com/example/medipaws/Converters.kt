package com.example.medipaws

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromEntryStatus(value: EntryStatus?): String? = value?.name

    @TypeConverter
    fun toEntryStatus(value: String?): EntryStatus? = value?.let { EntryStatus.valueOf(it) }
} 