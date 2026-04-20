package com.example.supershift.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. ENTITIES (The Tables)
// ==========================================

@Entity(tableName = "associates")
data class Associate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val pinCode: String? = null // For fast switching on the floor
)

@Entity(tableName = "tasks")
data class ShiftTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,      // e.g., "Sammich PB Grape"
    val pullAmount: String,    // e.g., "12 BT"
    val category: String,      // e.g., "RTE", "Prep", "Bakery"
    val basePoints: Int,       // Gamification: Point value for completing
    val isCompleted: Boolean = false,
    val completedById: Int? = null, // Links to Associate.id
    val timestampMs: Long? = null
)

@Entity(tableName = "incidents")
data class IncidentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val associateId: Int,      // Links to Associate.id
    val description: String,   // What happened
    val category: String,      // e.g., "Insubordination", "Task Failure"
    val timestampMs: Long,
    val isStatementGenerated: Boolean = false // Flags if DocuPro has processed it
)

// ==========================================
// 2. DATA ACCESS OBJECT (The Queries)
// ==========================================

@Dao
interface SuperShiftDao {

    // --- Associates ---
    @Query("SELECT * FROM associates ORDER BY name ASC")
    fun getAllAssociates(): Flow<List<Associate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssociate(associate: Associate)

    // --- Tasks (The Mission Engine) ---
    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY category ASC")
    fun getPendingTasks(): Flow<List<ShiftTask>>

    @Query("SELECT * FROM tasks WHERE category = :category AND isCompleted = 0")
    fun getTasksByCategory(category: String): Flow<List<ShiftTask>>

    @Query("DELETE FROM tasks WHERE category = :category AND isCompleted = 0")
    suspend fun deletePendingTasksByCategory(category: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ShiftTask>) // For the CSV Importer!

    @Update
    suspend fun updateTask(task: ShiftTask)

    //Shift State

    @Entity(tableName = "shift_state")
    data class ShiftState(
        @PrimaryKey val id: Int = 1, // Only one active shift at a time
        val startTimeMs: Long,
        val shiftName: String, // e.g., "Overnight", "Morning", "Afternoon"
        val isOpen: Boolean = false
    )

    @Query("SELECT * FROM shift_state WHERE id = 1")
    fun getActiveShift(): Flow<ShiftState?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateShiftState(state: ShiftState)

    @Query("DELETE FROM shift_state WHERE id = 1")
    suspend fun clearShiftState()
}

    // --- Incidents (The Black Box) ---
    @Query("SELECT * FROM incidents ORDER BY timestampMs DESC")
    fun getAllIncidents(): Flow<List<IncidentLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentLog)
}

// ==========================================
// 3. DATABASE BUILDER (The Engine)
// ==========================================

@Database(entities = [Associate::class, ShiftTask::class, IncidentLog::class], version = 1, exportSchema = false)
abstract class SuperShiftDatabase : RoomDatabase() {

    abstract fun superShiftDao(): SuperShiftDao

    companion object {
        @Volatile
        private var INSTANCE: SuperShiftDatabase? = null

        fun getDatabase(context: Context): SuperShiftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuperShiftDatabase::class.java,
                    "supershift_database"
                )
                    // .fallbackToDestructiveMigration() // Use this during dev if schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}