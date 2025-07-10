package com.example.medipaws

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

@Database(entities = [MedicineEntry::class, Pet::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineEntryDao(): MedicineEntryDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // Always use applicationContext to avoid leaking Activity/Service
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medipaws_db"
                )
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
                INSTANCE = instance
                instance
            }
        }

        // For testing or app shutdown
        fun destroyInstance() {
            INSTANCE = null
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medicine_entries ADD COLUMN notificationEnabled INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medicine_entries ADD COLUMN intervalValue INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE medicine_entries ADD COLUMN intervalUnit TEXT NOT NULL DEFAULT 'hours'")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medicine_entries ADD COLUMN repeatUntil INTEGER")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medicine_entries ADD COLUMN seriesId TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE medicine_entries ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
            }
        }
    }
} 