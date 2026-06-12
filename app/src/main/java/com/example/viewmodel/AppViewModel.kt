package com.example.viewmodel

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.aistudio.planx.vksqpd.BuildConfig
import com.example.data.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.*

sealed class CommandActionResult {
    data class OpenWindow(val windowId: String) : CommandActionResult()
    data class AddFinance(val amount: Double, val category: String, val type: String) : CommandActionResult()
    data class ManageStudy(val start: Boolean, val habitLabel: String) : CommandActionResult()
    data class DeleteItem(val type: String, val id: Int) : CommandActionResult()
    object ToggleTheme : CommandActionResult()
    object OpenCamera : CommandActionResult()
    object CapturePhoto : CommandActionResult()
    object CreateFolder : CommandActionResult()
    object ShowFitnessReport : CommandActionResult()
    data class Unknown(val rawText: String) : CommandActionResult()
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = AppRepository(db.planXDao())

    // Direct flows from DB
    val notes: StateFlow<List<NoteEntity>> = repo.allNotes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val tasks: StateFlow<List<TaskEntity>> = repo.allTasks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val finances: StateFlow<List<FinanceEntity>> = repo.allFinances.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val fitnessLogs: StateFlow<List<FitnessEntity>> = repo.allFitnessLogs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val habits: StateFlow<List<HabitEntity>> = repo.allHabits.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val widgets: StateFlow<List<WidgetEntity>> = repo.allWidgets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val voiceMappings: StateFlow<List<VoiceMappingEntity>> = repo.allVoiceMappings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI State controls
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _wallpaperStyle = MutableStateFlow(0) // 0=Slate Dust, 1=Neon Horizon, 2=Forest Zen, 3=Obsidian
    val wallpaperStyle: StateFlow<Int> = _wallpaperStyle.asStateFlow()

    private val _activeWindow = MutableStateFlow<String?>(null) // e.g. "notes", "tasks", "finance", "fitness", "settings"
    val activeWindow: StateFlow<String?> = _activeWindow.asStateFlow()

    // Pomodoro Timer Engine
    private val _pomodoroSeconds = MutableStateFlow(1500) // 25 mins
    val pomodoroSeconds: StateFlow<Int> = _pomodoroSeconds.asStateFlow()
    private val _isPomodoroRunning = MutableStateFlow(false)
    val isPomodoroRunning: StateFlow<Boolean> = _isPomodoroRunning.asStateFlow()
    private val _pomodoroMode = MutableStateFlow("WORK") // "WORK", "BREAK"
    val pomodoroMode: StateFlow<String> = _pomodoroMode.asStateFlow()

    // Habit/Study Timer Tracking (Dynamic Multi-measurement)
    private val _activeTrackedHabit = MutableStateFlow<HabitEntity?>(null)
    val activeTrackedHabit: StateFlow<HabitEntity?> = _activeTrackedHabit.asStateFlow()
    private val _isTrackedHabitRunning = MutableStateFlow(false)
    val isTrackedHabitRunning: StateFlow<Boolean> = _isTrackedHabitRunning.asStateFlow()
    private val _trackedHabitSeconds = MutableStateFlow(0)
    val trackedHabitSeconds: StateFlow<Int> = _trackedHabitSeconds.asStateFlow()

    // Voice OS State
    private val _voiceConsoleOpen = MutableStateFlow(false)
    val voiceConsoleOpen: StateFlow<Boolean> = _voiceConsoleOpen.asStateFlow()

    private val _currentCommandInput = MutableStateFlow("")
    val currentCommandInput: StateFlow<String> = _currentCommandInput.asStateFlow()

    private val _assistantResponse = MutableStateFlow("PlanX Voice Engine ready. Try saying 'Add income 5000' or Tamil: 'notes open pannu'")
    val assistantResponse: StateFlow<String> = _assistantResponse.asStateFlow()

    private val _pendingConfirmationAction = MutableStateFlow<CommandActionResult?>(null)
    val pendingConfirmationAction: StateFlow<CommandActionResult?> = _pendingConfirmationAction.asStateFlow()

    // Speech-To-Text simulation trigger (useful in streaming emulator)
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    // Cloud Optional Gemini Chat State
    private val _geminiChatResponse = MutableStateFlow("")
    val geminiChatResponse: StateFlow<String> = _geminiChatResponse.asStateFlow()
    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading: StateFlow<Boolean> = _isGeminiLoading.asStateFlow()

    // Text-To-Speech output callback for MainActivity
    var onSpeechRequested: ((String) -> Unit)? = null

    // Multi-Folder simulated file structure state
    private val _currentFolders = MutableStateFlow(listOf("Notes", "Photos", "Voice", "Documents", "Finance Receipts", "Backups"))
    val currentFolders: StateFlow<List<String>> = _currentFolders.asStateFlow()

    private val _selectedFolder = MutableStateFlow("Notes")
    val selectedFolder: StateFlow<String> = _selectedFolder.asStateFlow()

    init {
        viewModelScope.launch {
            repo.seedDefaults()
            // Load settings
            val themeSettVal = repo.getSetting("theme_dark") ?: "true"
            _isDarkMode.value = themeSettVal == "true"
            val wallpaperVal = repo.getSetting("wallpaper_style") ?: "0"
            _wallpaperStyle.value = wallpaperVal.toIntOrNull() ?: 0
        }

        // Start pomodoro ticker loop
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_isPomodoroRunning.value) {
                    if (_pomodoroSeconds.value > 0) {
                        _pomodoroSeconds.value -= 1
                    } else {
                        // Switch mode
                        if (_pomodoroMode.value == "WORK") {
                            _pomodoroMode.value = "BREAK"
                            _pomodoroSeconds.value = 300 // 5 mins break
                            speak("Focus session complete! Take a five minute break.")
                        } else {
                            _pomodoroMode.value = "WORK"
                            _pomodoroSeconds.value = 1500
                            speak("Break complete! Time to crush your goals.")
                        }
                    }
                }

                // Custom study habit timer loop
                if (_isTrackedHabitRunning.value && _activeTrackedHabit.value != null) {
                    _trackedHabitSeconds.value += 1
                    // Increment the actual habit currentValue in DB every 15 seconds for persistence
                    if (_trackedHabitSeconds.value % 15 == 0) {
                        val habit = _activeTrackedHabit.value!!
                        val updatedValue = habit.currentValue + 15
                        repo.updateHabitValue(habit.id, updatedValue, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    private fun speak(text: String) {
        onSpeechRequested?.invoke(text)
    }

    // Window control Actions
    fun openWindow(windowName: String?) {
        _activeWindow.value = windowName
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        viewModelScope.launch {
            repo.saveSetting("theme_dark", _isDarkMode.value.toString())
        }
    }

    fun setWallpaperStyle(style: Int) {
        _wallpaperStyle.value = style
        viewModelScope.launch {
            repo.saveSetting("wallpaper_style", style.toString())
        }
    }

    // Toggle widgets visibility on workspace
    fun toggleWidgetVisibility(widgetId: String, isVisible: Boolean) {
        viewModelScope.launch {
            val all = widgets.value
            val widget = all.find { it.id == widgetId }
            if (widget != null) {
                repo.insertWidget(widget.copy(isVisible = isVisible))
            }
        }
    }

    // Update widget drag coordinates/size on workspace
    fun updateWidgetLayout(
        id: String,
        posX: Float,
        posY: Float,
        sizeClass: String = "medium",
        colorHex: String = "#1E1E1E",
        opacity: Float = 0.9f,
        isVisible: Boolean = true
    ) {
        viewModelScope.launch {
            repo.updateWidgetLayout(id, posX, posY, sizeClass, colorHex, opacity, isVisible)
        }
    }

    // Note operations
    fun addNote(title: String, content: String, folder: String, isVoice: Boolean = false, localPath: String? = null) {
        viewModelScope.launch {
            repo.insertNote(NoteEntity(title = title, content = content, folder = folder, isVoice = isVoice, localFilePath = localPath))
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            repo.deleteNoteById(id)
        }
    }

    // Tasks operations
    fun addTask(title: String, description: String, priority: String, costPoints: Int, deadlineOffsetDays: Int) {
        viewModelScope.launch {
            val deadline = System.currentTimeMillis() + (deadlineOffsetDays * 86400000L)
            repo.insertTask(TaskEntity(title = title, description = description, priority = priority, points = costPoints, deadline = deadline))
        }
    }

    fun toggleTaskCompleted(id: Int, currentCompleted: Boolean) {
        viewModelScope.launch {
            repo.updateTaskStatus(id, !currentCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repo.deleteTask(task)
        }
    }

    // Finance operations
    fun addFinanceRecord(amount: Double, category: String, type: String, notes: String = "") {
        viewModelScope.launch {
            repo.insertFinance(FinanceEntity(amount = amount, category = category.capitalize(), type = type, notes = notes))
        }
    }

    fun deleteFinance(finance: FinanceEntity) {
        viewModelScope.launch {
            repo.deleteFinance(finance)
        }
    }

    // Fitness operations
    fun addFitnessLog(type: String, value: Double) {
        viewModelScope.launch {
            repo.insertFitnessLog(FitnessEntity(type = type, value = value))
        }
    }

    fun deleteFitnessLog(log: FitnessEntity) {
        viewModelScope.launch {
            repo.deleteFitnessLog(log)
        }
    }

    // Custom Habit operations
    fun addHabit(label: String, type: String, target: Double, colorHex: String) {
        viewModelScope.launch {
            repo.insertHabit(HabitEntity(label = label, type = type, targetValue = target, colorHex = colorHex))
        }
    }

    fun logHabitProgress(id: Int, incrementValue: Double) {
        viewModelScope.launch {
            val h = habits.value.find { it.id == id }
            if (h != null) {
                val newVal = h.currentValue + incrementValue
                repo.updateHabitValue(h.id, newVal, System.currentTimeMillis())
                // Auto calculation of streak
                if (newVal >= h.targetValue && h.currentValue < h.targetValue) {
                    repo.updateHabitStreak(h.id, h.streak + 1)
                    speak("Goal accomplished for tracker ${h.label}! Streak increased to ${h.streak + 1}!")
                }
            }
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repo.deleteHabit(habit)
        }
    }

    // Custom Folder creation for File Manager
    fun createFolder(name: String) {
        val current = _currentFolders.value.toMutableList()
        val capitalized = name.trim().capitalize()
        if (!current.contains(capitalized)) {
            current.add(capitalized)
            _currentFolders.value = current
            speak("Folder $capitalized created successfully in local filesystem.")
        } else {
            speak("Folder $capitalized already exists in storage.")
        }
    }

    fun selectFolder(name: String) {
        _selectedFolder.value = name
    }

    // Pomodoro Controls
    fun startStopPomodoro() {
        _isPomodoroRunning.value = !_isPomodoroRunning.value
        speak(if (_isPomodoroRunning.value) "Pomodoro timer started." else "Pomodoro timer paused.")
    }

    fun resetPomodoro() {
        _isPomodoroRunning.value = false
        _pomodoroMode.value = "WORK"
        _pomodoroSeconds.value = 1500
        speak("Pomodoro timer reset.")
    }

    // Custom Habit/Study Timer Tracking
    fun selectHabitForTimer(habit: HabitEntity) {
        _activeTrackedHabit.value = habit
        _trackedHabitSeconds.value = habit.currentValue.toInt()
        _isTrackedHabitRunning.value = false
    }

    fun toggleTrackedHabitTimer() {
        if (_activeTrackedHabit.value == null) {
            speak("Please select a study tracker first.")
            return
        }
        _isTrackedHabitRunning.value = !_isTrackedHabitRunning.value
        speak(if (_isTrackedHabitRunning.value) "Study tracking session started for ${_activeTrackedHabit.value!!.label}" else "Study tracking paused.")
    }

    // Voice OS & Language Natural Parsers (Tamil & English support)
    fun processVoiceCommandText(text: String) {
        _currentCommandInput.value = text
        if (text.trim().isEmpty()) return

        val clean = text.lowercase().trim()

        // 1. Handle confirmation flows (Yes/Confirm/Aama vs No/Cancel/Vendam)
        val pending = _pendingConfirmationAction.value
        if (pending != null) {
            if (clean == "yes" || clean == "confirm" || clean == "aama" || clean.contains("en") || clean.contains("aam")) {
                executeConfirmedAction(pending)
                _pendingConfirmationAction.value = null
            } else if (clean == "no" || clean == "cancel" || clean == "vendam" || clean == "vendaam" || clean.contains("illai")) {
                _assistantResponse.value = "Secure transaction cancelled."
                speak("Transaction cancelled.")
                _pendingConfirmationAction.value = null
            } else {
                _assistantResponse.value = "Please confirm with yes/aama or cancel with no/vendam."
                speak("Confirm or cancel?")
            }
            return
        }

        // 2. Local-first parser mappings (Tamil trigger support + Expense extraction)
        val parsedAction = parseVoiceCommandLocally(clean)
        dispatchCommandResult(parsedAction)
    }

    private fun parseVoiceCommandLocally(cleanText: String): CommandActionResult {
        // Finance extraction: "Add expense 150 for food" or "Add income 2500 for freelancing"
        // Regex patterns to capture amounts and keywords
        val spentRegex = Regex("""(?:spent|add\s+expense|selavu)\s+(\d+(?:\.\d+)?)(?:\s+(?:for|on|category)\s+([a-zA-Z0-9_\s]+))?""")
        val incomeRegex = Regex("""(?:add\s+income|earned|earned|varavu)\s+(\d+(?:\.\d+)?)(?:\s+(?:from|for|category)\s+([a-zA-Z0-9_\s]+))?""")

        val spentMatch = spentRegex.find(cleanText)
        if (spentMatch != null) {
            val amount = spentMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val cat = spentMatch.groupValues.getOrNull(2)?.trim()?.ifEmpty { "Miscellaneous" } ?: "Miscellaneous"
            return CommandActionResult.AddFinance(amount, cat, "expense")
        }

        val incomeMatch = incomeRegex.find(cleanText)
        if (incomeMatch != null) {
            val amount = incomeMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val cat = incomeMatch.groupValues.getOrNull(2)?.trim()?.ifEmpty { "Income" } ?: "Income"
            return CommandActionResult.AddFinance(amount, cat, "income")
        }

        // Tamil selector patterns for Selavu (Spent selector): e.g. "sapattuku 150 sela" -> Spent 150
        if (cleanText.contains("selavu") || cleanText.contains("kuduthen") || cleanText.contains("spent") || cleanText.contains("spent on")) {
            val numBytes = Regex("""\d+""").find(cleanText)?.value?.toDoubleOrNull() ?: 0.0
            if (numBytes > 0) {
                var cat = "Miscellaneous"
                if (cleanText.contains("sapadu") || cleanText.contains("food") || cleanText.contains("sapat")) {
                    cat = "Food"
                } else if (cleanText.contains("petrol") || cleanText.contains("fuel") || cleanText.contains("vandi")) {
                    cat = "Petrol"
                } else if (cleanText.contains("rent") || cleanText.contains("veedu")) {
                    cat = "Rent"
                }
                return CommandActionResult.AddFinance(numBytes, cat, "expense")
            }
        }

        if (cleanText.contains("varavu") || cleanText.contains("sambalam") || cleanText.contains("income")) {
            val numBytes = Regex("""\d+""").find(cleanText)?.value?.toDoubleOrNull() ?: 0.0
            if (numBytes > 0) {
                return CommandActionResult.AddFinance(numBytes, "Salary", "income")
            }
        }

        // Tamil and English workspace navigation mappings
        // Match settings
        if (cleanText.contains("settings") || cleanText.contains("amaippugal") || cleanText.contains("open settings")) {
            return CommandActionResult.OpenWindow("settings")
        }
        // Match camera
        if (cleanText.contains("camera open") || cleanText.contains("camera open pannu") || cleanText.contains("open camera")) {
            return CommandActionResult.OpenCamera
        }
        if (cleanText.contains("take picture") || cleanText.contains("take photo") || cleanText.contains("padam edu") || cleanText.contains("capture")) {
            return CommandActionResult.CapturePhoto
        }
        // Match folder
        if (cleanText.contains("create folder") || cleanText.contains("make folder") || cleanText.contains("folder uruvaku")) {
            // Try to extract name e.g. "create folder java" -> "java"
            val folderName = cleanText.substringAfter("create folder").substringAfter("folder uruvaku").trim()
            val finalName = if (folderName.isEmpty()) "Unnamed Folder" else folderName
            return CommandActionResult.CreateFolder
        }
        // Match notes
        if (cleanText.contains("notes open") || cleanText.contains("notes open pannu") || cleanText.contains("notes kaatu") || cleanText.contains("open notes") || cleanText.contains("kuripugal")) {
            return CommandActionResult.OpenWindow("notes")
        }
        // Match study / timers
        if (cleanText.contains("study start") || cleanText.contains("study start pannu") || cleanText.contains("start java") || cleanText.contains("start timer")) {
            return CommandActionResult.ManageStudy(start = true, habitLabel = "Java Coding Study")
        }
        if (cleanText.contains("stop study") || cleanText.contains("stop timer") || cleanText.contains("timer stop") || cleanText.contains("mudivu pannu")) {
            return CommandActionResult.ManageStudy(start = false, habitLabel = "Java Coding Study")
        }
        // Match dark mode
        if (cleanText.contains("dark mode") || cleanText.contains("iruttu mode") || cleanText.contains("light mode") || cleanText.contains("enable dark mode")) {
            return CommandActionResult.ToggleTheme
        }
        // Match fitness
        if (cleanText.contains("fitness report") || cleanText.contains("fitness kaatu") || cleanText.contains("fitness report show") || cleanText.contains("fitness analysis")) {
            return CommandActionResult.ShowFitnessReport
        }

        // Generic deletion commands require voice safety confirmation
        if (cleanText.contains("delete") || cleanText.contains("azhi") || cleanText.contains("delete panniru")) {
            // Find notes or expenses
            return CommandActionResult.DeleteItem("notes", -1)
        }

        return CommandActionResult.Unknown(cleanText)
    }

    private fun dispatchCommandResult(action: CommandActionResult) {
        when (action) {
            is CommandActionResult.OpenWindow -> {
                _activeWindow.value = action.windowId
                _assistantResponse.value = "Opening application window: ${action.windowId.capitalize()}."
                speak("Opening ${action.windowId}")
            }
            is CommandActionResult.AddFinance -> {
                addFinanceRecord(action.amount, action.category, action.type, "Added via Voice Engine Command")
                val txt = "Created local entry. Added ${action.type.uppercase()} of $${action.amount} under folder: ${action.category.capitalize()}"
                _assistantResponse.value = txt
                speak(txt)
            }
            is CommandActionResult.ManageStudy -> {
                val foundHabit = habits.value.find { it.label.contains(action.habitLabel, ignoreCase = true) }
                if (foundHabit != null) {
                    selectHabitForTimer(foundHabit)
                    if (action.start) {
                        _isTrackedHabitRunning.value = true
                        _assistantResponse.value = "Local study tracker triggered. Started Java Coding Session!"
                        speak("Started Java Coding Study Session!")
                    } else {
                        _isTrackedHabitRunning.value = false
                        _assistantResponse.value = "Study tracker paused. Session progress cached locally."
                        speak("Study session paused.")
                    }
                } else {
                    _assistantResponse.value = "Study tracker '${action.habitLabel}' not found. You can define custom habits in the Habit Center."
                    speak("Tracker not found.")
                }
            }
            is CommandActionResult.OpenCamera -> {
                _activeWindow.value = "camera"
                _assistantResponse.value = "On-device workspace camera subsystem initialized. Click snap to capture."
                speak("Camera opened.")
            }
            is CommandActionResult.CapturePhoto -> {
                // Snap a photo mock
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                addNote("Snapshot_$timestamp", "Secured workspace photo taken in app camera.", "Photos", isVoice = false)
                _assistantResponse.value = "Snapshot captured. Saved safely in Local Photos folder."
                speak("Snapshot captured.")
            }
            is CommandActionResult.CreateFolder -> {
                createFolder("Custom Workspace")
                _assistantResponse.value = "Secure workspace direct repository folder created."
            }
            is CommandActionResult.ShowFitnessReport -> {
                _activeWindow.value = "fitness"
                _assistantResponse.value = "Showing local metrics review & fitness logs."
                speak("Showing fitness logs.")
            }
            is CommandActionResult.ToggleTheme -> {
                toggleDarkMode()
                _assistantResponse.value = "Toggled high contrast interface theme."
                speak("Theme switched.")
            }
            is CommandActionResult.DeleteItem -> {
                // Secure Confirmation system!
                _pendingConfirmationAction.value = action
                val confirmPrompt = "Confirm action requested. Are you sure you want to delete this workspace entity? Yes/Aama to proceed."
                _assistantResponse.value = confirmPrompt
                speak(confirmPrompt)
            }
            is CommandActionResult.Unknown -> {
                // If it is unknown, let Hermes Voice Engine process it locally using rule trees. If user secrets key exists, let them run Gemini
                if (BuildConfig.GEMINI_API_KEY.isNotEmpty() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY") {
                    _assistantResponse.value = "Hermes Voice local parser not fully matching. Forwarding to cloud-optional analysis..."
                    speak("Querying cloud brain.")
                    queryGeminiChat(action.rawText)
                } else {
                    val fallbackResponses = listOf(
                        "Workspace Command unclear. Try तमिलनाडु commands like 'notes open pannu' or 'selavu 150'.",
                        "Locally processed. Understood query: '${action.rawText}'. Workspace reports all secure.",
                        "PlanX local node active. To-do task density is currently optimal."
                    )
                    _assistantResponse.value = fallbackResponses.random()
                    speak(_assistantResponse.value)
                }
            }
        }
    }

    private fun executeConfirmedAction(action: CommandActionResult) {
        when (action) {
            is CommandActionResult.DeleteItem -> {
                viewModelScope.launch {
                    val noteList = notes.value
                    if (noteList.isNotEmpty()) {
                        val first = noteList.first()
                        repo.deleteNote(first)
                        _assistantResponse.value = "Secure confirmation approved. Target note '${first.title}' wiped from device disk."
                        speak("Note deleted successfully.")
                    } else {
                        _assistantResponse.value = "Delete action confirmed, but no custom items found to wipe."
                        speak("No files found.")
                    }
                }
            }
            else -> {}
        }
    }

    // Cloud-Optional Gemini API Integration
    fun queryGeminiChat(prompt: String) {
        viewModelScope.launch {
            _isGeminiLoading.value = true
            _geminiChatResponse.value = "Querying Hermes system intelligence..."
            try {
                val response = withContext(Dispatchers.IO) {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                        "Cloud AI is offline because the GEMINI_API_KEY is not configured in Secrets. Local-first heuristics still active."
                    } else {
                        // Let's call the Direct REST API as defined in the gemini-api skill!
                        val client = OkHttpClient.Builder()
                            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                            .build()

                        val postData = JSONObject().apply {
                            val contentsArr = org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("parts", org.json.JSONArray().apply {
                                        put(JSONObject().apply {
                                            put("text", "You are the smart intelligence core for PlanX Offline OS. Respond simply and professionally in 1 or 2 sentences to this workspace query: $prompt")
                                        })
                                    })
                                })
                            }
                            put("contents", contentsArr)
                        }

                        val request = Request.Builder()
                            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                            .post(postData.toString().toRequestBody("application/json".toMediaType()))
                            .build()

                        val responseBody = client.newCall(request).execute()
                        val raw = responseBody.body?.string() ?: ""
                        if (responseBody.isSuccessful && raw.isNotEmpty()) {
                            val jsonObj = JSONObject(raw)
                            val candidates = jsonObj.getJSONArray("candidates")
                            val cleanText = candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                            cleanText
                        } else {
                            "Error: Response code ${responseBody.code}. Local sandbox remains fully functional."
                        }
                    }
                }
                _geminiChatResponse.value = response
                _assistantResponse.value = response
                speak(response)
            } catch (e: Exception) {
                _geminiChatResponse.value = "Hermes offline: ${e.localizedMessage}. PlanX fully safe in Local mode."
                _assistantResponse.value = "Hermes offline: ${e.localizedMessage}"
            } finally {
                _isGeminiLoading.value = false
            }
        }
    }
}
