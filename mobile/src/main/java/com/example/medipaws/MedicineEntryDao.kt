package com.example.medipaws

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineEntryDao {
    @Query("SELECT * FROM medicine_entries ORDER BY dateTime DESC")
    fun getAllEntries(): Flow<List<MedicineEntry>>

    @Insert
    suspend fun insert(entry: MedicineEntry)

    @Delete
    suspend fun delete(entry: MedicineEntry)

    @Update
    suspend fun update(entry: MedicineEntry)
} 