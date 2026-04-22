package com.example.supershift.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "associates")
data class Associate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val role: String,
    val currentArchetype: String = "Float",
    val pinCode: String? = null,
    val scheduledDays: String = "",
    val defaultStartTime: String = "22:00",
    val defaultEndTime: String = "06:30"
)

@Entity(tableName = "schedule_entries")
data class ScheduleEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val associateName: String,
    val startTime: String,
    val endTime: String
)

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val buildTo: String,
    val category: String,
    val amountHave: Int? = null,
    val amountNeeded: Int? = null,
    val isPulled: Boolean = false
)

@Entity(tableName = "table_items")
data class TableItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val station: String,
    val isInitialed: Boolean = true,
    val wasteAmount: String? = null
)

@Entity(tableName = "tasks")
data class ShiftTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskName: String,
    val archetype: String,
    val basePoints: Int = 10,
    val isCompleted: Boolean = false,
    val isPullTask: Boolean = false,
    val pullCategory: String? = null,
    val isSticky: Boolean = false,
    val priority: String = "Normal",
    val taskDescription: String? = null,
    val assignedTo: String? = null,
    val completedBy: String? = null,
    val isTruckTask: Boolean = false
)

@Entity(tableName = "incidents")
data class IncidentLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val associateId: Int,
    val description: String,
    val category: String,
    val timestampMs: Long,
    val isStatementGenerated: Boolean = false
)

@Entity(tableName = "shift_state")
data class ShiftState(
    @PrimaryKey val id: Int = 1,
    val startTimeMs: Long,
    val endTimeMs: Long = 0L,
    val shiftName: String,
    val isOpen: Boolean = false,
    val isTruckNight: Boolean = false
)

@Dao
interface SuperShiftDao {
    @Query("SELECT * FROM associates ORDER BY name ASC")
    fun getAllAssociates(): Flow<List<Associate>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssociate(associate: Associate)
    @Update
    suspend fun updateAssociate(associate: Associate)
    @Delete
    suspend fun deleteAssociate(associate: Associate)

    @Query("SELECT * FROM schedule_entries ORDER BY startTime ASC")
    fun getSchedule(): Flow<List<ScheduleEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(entry: ScheduleEntry)
    @Delete
    suspend fun deleteScheduleEntry(entry: ScheduleEntry)
    @Query("DELETE FROM schedule_entries")
    suspend fun clearSchedule()

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, archetype ASC")
    fun getAllTasks(): Flow<List<ShiftTask>>
    @Query("SELECT * FROM tasks WHERE pullCategory = :category LIMIT 1")
    suspend fun getTaskByPullCategory(category: String): ShiftTask?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: ShiftTask)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<ShiftTask>)
    @Update
    suspend fun updateTask(task: ShiftTask)

    @Query("DELETE FROM tasks WHERE isSticky = 0 AND isPullTask = 0 AND isTruckTask = 0")
    suspend fun deleteJitTasks()
    @Query("UPDATE tasks SET isCompleted = 0, completedBy = null WHERE isSticky = 1 OR isPullTask = 1 OR isTruckTask = 1")
    suspend fun resetStickyAndPullTasks()
    @Query("UPDATE tasks SET archetype = 'MOD', assignedTo = 'MOD' WHERE archetype = :fromZone AND priority = 'High' AND isCompleted = 0")
    suspend fun pullHighPriorityToMod(fromZone: String)

    @Query("SELECT * FROM inventory_items WHERE category = :category")
    fun getInventoryByCategory(category: String): Flow<List<InventoryItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(items: List<InventoryItem>)
    @Update
    suspend fun updateInventoryItem(item: InventoryItem)
    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)
    @Query("DELETE FROM inventory_items WHERE category = :category")
    suspend fun clearInventoryByCategory(category: String)
    @Query("UPDATE inventory_items SET amountHave = null, amountNeeded = null, isPulled = 0")
    suspend fun resetInventoryCounts()

    @Query("SELECT * FROM table_items WHERE station = :station")
    fun getTableItemsByStation(station: String): Flow<List<TableItem>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTableItems(items: List<TableItem>)
    @Update
    suspend fun updateTableItem(item: TableItem)
    @Delete
    suspend fun deleteTableItem(item: TableItem)
    @Query("DELETE FROM table_items WHERE station = :station")
    suspend fun clearTableItemsByStation(station: String)
    @Query("UPDATE table_items SET isInitialed = 1, wasteAmount = null WHERE station = :station")
    suspend fun resetTableItemsByStation(station: String)

    @Query("SELECT * FROM incidents ORDER BY timestampMs DESC")
    fun getAllIncidents(): Flow<List<IncidentLog>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: IncidentLog)

    @Query("SELECT * FROM shift_state WHERE id = 1")
    fun getActiveShift(): Flow<ShiftState?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateShiftState(state: ShiftState)
    @Query("DELETE FROM shift_state WHERE id = 1")
    suspend fun clearShiftState()
}

@Database(
    entities = [
        Associate::class, ScheduleEntry::class, ShiftTask::class,
        InventoryItem::class, IncidentLog::class, ShiftState::class, TableItem::class
    ],
    version = 10,
    exportSchema = false
)
abstract class SuperShiftDatabase : RoomDatabase() {
    abstract fun superShiftDao(): SuperShiftDao
    companion object {
        @Volatile private var INSTANCE: SuperShiftDatabase? = null
        fun getDatabase(context: Context): SuperShiftDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, SuperShiftDatabase::class.java, "supershift_database")
                    .fallbackToDestructiveMigration().build()
                INSTANCE = instance; instance
            }
        }
    }
}
