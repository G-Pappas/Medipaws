package com.example.medipaws

import android.content.Context
import androidx.room.*
import java.util.Date

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val species: String? = null,
    val breed: String? = null,
    val age: Int? = null,
    val photoUri: String? = null
)

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPets(): kotlinx.coroutines.flow.Flow<List<Pet>>

    @Insert
    suspend fun insert(pet: Pet): Long

    @Update
    suspend fun update(pet: Pet)

    @Delete
    suspend fun delete(pet: Pet)
}

@Database(entities = [MedicineEntry::class, Pet::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineEntryDao(): MedicineEntryDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medipaws_db"
                )
                // SOS: REMOVE .fallbackToDestructiveMigration() BEFORE PRODUCTION! This will delete all user data on schema change.
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
} 