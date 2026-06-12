package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AppRepository(private val dao: PlanXDao) {

    val allNotes: Flow<List<NoteEntity>> = dao.getAllNotes()
    fun getNotesByFolder(folder: String): Flow<List<NoteEntity>> = dao.getNotesByFolder(folder)
    suspend fun insertNote(note: NoteEntity): Long = dao.insertNote(note)
    suspend fun deleteNote(note: NoteEntity) = dao.deleteNote(note)
    suspend fun deleteNoteById(id: Int) = dao.deleteNoteById(id)

    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    suspend fun insertTask(task: TaskEntity): Long = dao.insertTask(task)
    suspend fun deleteTask(task: TaskEntity) = dao.deleteTask(task)
    suspend fun updateTaskStatus(id: Int, isCompleted: Boolean) = dao.updateTaskStatus(id, isCompleted)

    val allFinances: Flow<List<FinanceEntity>> = dao.getAllFinances()
    suspend fun insertFinance(finance: FinanceEntity): Long = dao.insertFinance(finance)
    suspend fun deleteFinance(finance: FinanceEntity) = dao.deleteFinance(finance)

    val allFitnessLogs: Flow<List<FitnessEntity>> = dao.getAllFitnessLogs()
    suspend fun insertFitnessLog(fitness: FitnessEntity): Long = dao.insertFitnessLog(fitness)
    suspend fun deleteFitnessLog(fitness: FitnessEntity) = dao.deleteFitnessLog(fitness)

    val allHabits: Flow<List<HabitEntity>> = dao.getAllHabits()
    suspend fun insertHabit(habit: HabitEntity): Long = dao.insertHabit(habit)
    suspend fun deleteHabit(habit: HabitEntity) = dao.deleteHabit(habit)
    suspend fun updateHabitValue(id: Int, value: Double, timestamp: Long) = dao.updateHabitValue(id, value, timestamp)
    suspend fun updateHabitStreak(id: Int, newStreak: Int) = dao.updateHabitStreak(id, newStreak)

    val allWidgets: Flow<List<WidgetEntity>> = dao.getAllWidgets()
    suspend fun insertWidget(widget: WidgetEntity) = dao.insertWidget(widget)
    suspend fun insertWidgets(widgets: List<WidgetEntity>) = dao.insertWidgets(widgets)
    suspend fun updateWidgetLayout(
        id: String,
        posX: Float,
        posY: Float,
        sizeClass: String,
        colorHex: String,
        opacity: Float,
        isVisible: Boolean
    ) = dao.updateWidgetLayout(id, posX, posY, sizeClass, colorHex, opacity, isVisible)
    suspend fun deleteWidget(widget: WidgetEntity) = dao.deleteWidget(widget)

    val allSettingsFlow: Flow<List<SettingEntity>> = dao.getAllSettingsFlow()
    suspend fun getSetting(key: String): String? = dao.getSetting(key)?.value
    suspend fun saveSetting(key: String, value: String) = dao.insertSetting(SettingEntity(key, value))

    val allVoiceMappings: Flow<List<VoiceMappingEntity>> = dao.getAllVoiceMappings()
    suspend fun insertVoiceMapping(mapping: VoiceMappingEntity) = dao.insertVoiceMapping(mapping)

    suspend fun seedDefaults() {
        // Seed default widgets if empty
        val currentWidgets = dao.getAllWidgets().firstOrNull() ?: emptyList()
        if (currentWidgets.isEmpty()) {
            val defaults = listOf(
                WidgetEntity("study_tracker", "study_tracker", "Study Tracking", isVisible = true),
                WidgetEntity("habit_tracker", "habit_tracker", "Habit Loops", isVisible = true),
                WidgetEntity("fitness_tracker", "fitness_tracker", "Fitness Hub", isVisible = true),
                WidgetEntity("expense_tracker", "expense_tracker", "Expense & Income Ledger", isVisible = true),
                WidgetEntity("todo_list", "todo_list", "To-Do Matrix", isVisible = true),
                WidgetEntity("notes", "notes", "Local Memo Scratchpad", isVisible = true),
                WidgetEntity("calendar", "calendar", "Calendar Grid", isVisible = true),
                WidgetEntity("pomodoro", "pomodoro", "Workspace Pomodoro", isVisible = true),
                WidgetEntity("voice_recorder", "voice_recorder", "Secured Voice Recorder", isVisible = false),
                WidgetEntity("photo_gallery", "photo_gallery", "Local Snap Gallery", isVisible = false),
                WidgetEntity("ai_assistant", "ai_assistant", "Hermes local AI Assistant", isVisible = true)
            )
            dao.insertWidgets(defaults)
        }

        // Seed voice mappings
        val currentMappings = dao.getAllVoiceMappings().firstOrNull() ?: emptyList()
        if (currentMappings.isEmpty()) {
            val mappings = listOf(
                VoiceMappingEntity(
                    nativePhrases = "delete panniru, delete file, azhi, clear note, delete",
                    actionName = "DELETE_FILE",
                    confirmationRequired = true
                ),
                VoiceMappingEntity(
                    nativePhrases = "camera open pannu, open camera, camera, launch camera, padamedukkavum",
                    actionName = "OPEN_CAMERA",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "take picture, take photo, snap, snapshot, capture, padam edu",
                    actionName = "TAKE_PICTURE",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "create folder, make directory, folder uruvaku, new folder",
                    actionName = "CREATE_FOLDER",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "notes open pannu, open notes, show notes, notes kaatu, notes",
                    actionName = "OPEN_NOTES",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "study start pannu, start java study timer, dsa study start, track study",
                    actionName = "START_STUDY",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "stop study, stop timer, finish tracker, dsa stop, mudivu pannu",
                    actionName = "STOP_STUDY",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "dark mode, night mode, enable dark mode, dark path, iruttu, light mode",
                    actionName = "DARK_MODE",
                    confirmationRequired = false
                ),
                VoiceMappingEntity(
                    nativePhrases = "fitness report, show report, report summary, fitness analysis, fitness kaatu",
                    actionName = "FITNESS_REPORT",
                    confirmationRequired = false
                )
            )
            dao.insertVoiceMappings(mappings)
        }

        // Seed some initial habits for the user to see how it works instantly
        val currentHabits = dao.getAllHabits().firstOrNull() ?: emptyList()
        if (currentHabits.isEmpty()) {
            dao.insertHabit(HabitEntity(label = "Java Coding Study", type = "TIME", targetValue = 7200.0, currentValue = 1800.0, streak = 5, colorHex = "#FB8C00"))
            dao.insertHabit(HabitEntity(label = "Water Intake", type = "NUMERIC", targetValue = 8.0, currentValue = 3.0, streak = 2, colorHex = "#03A9F4"))
            dao.insertHabit(HabitEntity(label = "Meditation", type = "TIME", targetValue = 900.0, currentValue = 0.0, streak = 0, colorHex = "#9C27B0"))
            dao.insertHabit(HabitEntity(label = "DSA Focus Sessions", type = "COUNT", targetValue = 5.0, currentValue = 2.0, streak = 8, colorHex = "#E91E63"))
        }

        // Seed a welcome note in folders Notes and Documents
        val currentNotes = dao.getAllNotes().firstOrNull() ?: emptyList()
        if (currentNotes.isEmpty()) {
            dao.insertNote(
                NoteEntity(
                    title = "Welcome to PlanX OS",
                    content = "PlanX is your secure, local-first workspace. Speak Tamil, English or customize dynamic widgets! Try saying 'Add income 5000' or 'study start pannu' in the Voice Engine.",
                    folder = "Notes"
                )
            )
            dao.insertNote(
                NoteEntity(
                    title = "System Workspace Structure",
                    content = "Dynamic Local folders initialized:\n- Notes\n- Documents\n- Photo Gallery\n- Voice Logs\n- Finance Receipts\n- Backups",
                    folder = "Documents"
                )
            )
        }

        // Seed generic tasks
        val currentTasks = dao.getAllTasks().firstOrNull() ?: emptyList()
        if (currentTasks.isEmpty()) {
            dao.insertTask(TaskEntity(title = "Review Local Backups", description = "Ensure SQLite database remains updated and optimized.", deadline = System.currentTimeMillis() + 86400000 * 2, priority = "HIGH", points = 20))
            dao.insertTask(TaskEntity(title = "Practice 2 DSA problems", description = "Reverse Linked List and binary search questions.", deadline = System.currentTimeMillis() + 86400000, priority = "MEDIUM", points = 15))
        }

        // Seed some finances
        val currentFinances = dao.getAllFinances().firstOrNull() ?: emptyList()
        if (currentFinances.isEmpty()) {
            dao.insertFinance(FinanceEntity(amount = 25000.0, category = "Freelance", type = "income", notes = "Seeded Starting Income"))
            dao.insertFinance(FinanceEntity(amount = 120.0, category = "food", type = "expense", notes = "Seeded cafeteria lunch"))
        }
    }
}
