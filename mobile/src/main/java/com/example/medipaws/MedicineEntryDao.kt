package com.example.medipaws

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineEntryDao {
    @Query("SELECT * FROM medicine_entries ORDER BY dateTime DESC")
    fun getAllEntries(): Flow<List<MedicineEntry>>

    @Insert
    suspend fun insert(entry: MedicineEntry): Long

    @Delete
    suspend fun delete(entry: MedicineEntry)

    @Update
    suspend fun update(entry: MedicineEntry)

    @Query("SELECT * FROM medicine_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): MedicineEntry?
} 