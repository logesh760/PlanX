package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ------------------ ENTITIES ------------------

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val folder: String, // "Notes", "Photos", "Voice", "Documents" etc.
    val timestamp: Long = System.currentTimeMillis(),
    val isVoice: Boolean = false,
    val localFilePath: String? = null
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val deadline: Long,
    val priority: String, // "HIGH", "MEDIUM", "LOW"
    val colorHex: String = "#2196F3",
    val points: Int = 10,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false
)

@Entity(tableName = "finance")
data class FinanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String,
    val type: String, // "income" or "expense"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "fitness")
data class FitnessEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "Steps", "Push-ups", "Pull-ups", "Running", "Water", "Calories", "Weight", "Sleep"
    val value: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String, // "Java Study", "DSA Practice", "Reading", "Exercise", "Meditation", "Water Intake"
    val type: String, // "TIME" (seconds), "COUNT", "PERCENT", "NUMERIC"
    val targetValue: Double,
    val currentValue: Double = 0.0,
    val streak: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val colorHex: String = "#4CAF50"
)

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val id: String,
    val type: String, // "study_tracker", "habit_tracker", "fitness_tracker", "expense_tracker", "todo_list", "notes", "calendar", "pomodoro", "file_manager", "voice_recorder", "photo_gallery", "ai_assistant"
    val title: String,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val sizeClass: String = "medium", // "compact", "medium", "expanded"
    val colorHex: String = "#1E1E1E",
    val opacity: Float = 0.9f,
    val isVisible: Boolean = true
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "voice_mappings")
data class VoiceMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nativePhrases: String, // Comma separated, e.g., "delete panniru, delete, clear"
    val actionName: String, // "DELETE_FILE", "OPEN_SETTINGS", "OPEN_CAMERA", "TAKE_PICTURE", "CREATE_FOLDER", "OPEN_NOTES", "START_STUDY", "STOP_STUDY", "DARK_MODE", "FITNESS_REPORT"
    val confirmationRequired: Boolean = false
)

// ------------------ DAO ------------------

@Dao
interface PlanXDao {
    // Notes
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE folder = :folder ORDER BY timestamp DESC")
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Tasks
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean)

    // Finance
    @Query("SELECT * FROM finance ORDER BY timestamp DESC")
    fun getAllFinances(): Flow<List<FinanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinance(finance: FinanceEntity): Long

    @Delete
    suspend fun deleteFinance(finance: FinanceEntity)

    // Fitness
    @Query("SELECT * FROM fitness ORDER BY timestamp DESC")
    fun getAllFitnessLogs(): Flow<List<FitnessEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFitnessLog(fitness: FitnessEntity): Long

    @Delete
    suspend fun deleteFitnessLog(fitness: FitnessEntity)

    // Habits / Trackers
    @Query("SELECT * FROM habits ORDER BY label ASC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Query("UPDATE habits SET currentValue = :value, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateHabitValue(id: Int, value: Double, timestamp: Long)

    @Query("UPDATE habits SET streak = :newStreak WHERE id = :id")
    suspend fun updateHabitStreak(id: Int, newStreak: Int)

    // Widgets
    @Query("SELECT * FROM widgets")
    fun getAllWidgets(): Flow<List<WidgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidgets(widgets: List<WidgetEntity>)

    @Query("UPDATE widgets SET posX = :posX, posY = :posY, sizeClass = :sizeClass, colorHex = :colorHex, opacity = :opacity, isVisible = :isVisible WHERE id = :id")
    suspend fun updateWidgetLayout(id: String, posX: Float, posY: Float, sizeClass: String, colorHex: String, opacity: Float, isVisible: Boolean)

    @Delete
    suspend fun deleteWidget(widget: WidgetEntity)

    // Settings
    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Query("SELECT * FROM settings")
    fun getAllSettingsFlow(): Flow<List<SettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    // Voice Mappings
    @Query("SELECT * FROM voice_mappings")
    fun getAllVoiceMappings(): Flow<List<VoiceMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceMapping(mapping: VoiceMappingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceMappings(mappings: List<VoiceMappingEntity>)
}

// ------------------ DATABASE ------------------

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        FinanceEntity::class,
        FitnessEntity::class,
        HabitEntity::class,
        WidgetEntity::class,
        SettingEntity::class,
        VoiceMappingEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun planXDao(): PlanXDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "planx_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
