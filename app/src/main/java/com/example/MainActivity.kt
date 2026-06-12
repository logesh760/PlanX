package com.example

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.AspectRatio

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Text-To-Speech for offline voice feedback!
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                isTtsInitialized = true
            }
        }

        setContent {
            val viewModel: AppViewModel = viewModel()

            // Connect Speeker
            LaunchedEffect(isTtsInitialized) {
                if (isTtsInitialized) {
                    viewModel.onSpeechRequested = { phrase ->
                        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }

            val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val wallpaper by viewModel.wallpaperStyle.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkspaceDesktop(viewModel = viewModel, wallpaperStyle = wallpaper)
                }
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// ---------------- BACKGROUNDS & WALLPAPERS ----------------
@Composable
fun getWallpaperBrush(style: Int): Brush {
    return when (style) {
        0 -> Brush.verticalGradient(
            colors = listOf(com.example.ui.theme.ImmersiveBackground, Color(0xFF0C0A0F), com.example.ui.theme.ImmersiveBackground) // Immersive Theme Dark Space
        )
        1 -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF007F), Color(0xFF8A2BE2), Color(0xFFFF8C00)) // Neon Cyber Horizon Sunset
        )
        2 -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F261E), Color(0xFF081210), Color(0xFF141218)) // Emerald Forest Zen Dark
        )
        else -> Brush.verticalGradient(
            colors = listOf(com.example.ui.theme.ImmersiveBackground, com.example.ui.theme.ImmersiveBackground) // Pure Obsidian Brutalist Console
        )
    }
}

// ---------------- MAIN OS WORKSPACE DESKTOP ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkspaceDesktop(viewModel: AppViewModel, wallpaperStyle: Int) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val finances by viewModel.finances.collectAsStateWithLifecycle()
    val fitnessLogs by viewModel.fitnessLogs.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val widgets by viewModel.widgets.collectAsStateWithLifecycle()
    val activeWindow by viewModel.activeWindow.collectAsStateWithLifecycle()
    val isLightOrDark by viewModel.isDarkMode.collectAsStateWithLifecycle()

    // Workspace search filter
    var globalSearchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    // XP calculation: 10XP per completed task point
    val xpPoints = remember(tasks) {
        tasks.filter { it.isCompleted }.sumOf { it.points }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getWallpaperBrush(wallpaperStyle))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Grid background effect for brutalist themes
        if (wallpaperStyle == 3 || wallpaperStyle == 1 || wallpaperStyle == 0) {
            Canvas(modifier = Modifier.fillMaxSize().alpha(0.06f)) {
                val gridSpacing = 40.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(Color.Gray, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(Color.Gray, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                    y += gridSpacing
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. TOP SYSTEM STATUS BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // OS BRANDING + XP
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFD0BCFF), Color(0xFF381E72))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "PlanX",
                            color = com.example.ui.theme.ImmersiveTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "V-OS ALPHA",
                            color = com.example.ui.theme.ImmersivePrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Gamified User Score XP Base Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(com.example.ui.theme.ImmersiveSurfaceVariant)
                            .border(1.dp, com.example.ui.theme.ImmersiveOutline, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${xpPoints} XP",
                            color = com.example.ui.theme.ImmersivePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // LOCAL SYSTEM CLOCK (updates on composure)
                val timeStamp = remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    while (true) {
                        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                        timeStamp.value = sdf.format(Date())
                        delay(10000)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Local Sandbox",
                            tint = com.example.ui.theme.ImmersiveTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Local Sandboxed",
                            color = com.example.ui.theme.ImmersiveTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = timeStamp.value,
                        color = com.example.ui.theme.ImmersiveTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. DYNAMIC WORKSPACE BODY WITH DRAGGABLE WIDGETS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // If there's an active app launcher query, highlight find results overlay
                if (isSearchFocused && globalSearchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .clickable { isSearchFocused = false }
                            .padding(16.dp)
                            .testTag("search_overlay")
                    ) {
                        Column {
                            Text("Fast Unified Index Search (Local First)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            val filteredNotes = notes.filter { it.title.contains(globalSearchQuery, ignoreCase = true) || it.content.contains(globalSearchQuery, ignoreCase = true) }
                            val filteredTasks = tasks.filter { it.title.contains(globalSearchQuery, ignoreCase = true) || it.description.contains(globalSearchQuery, ignoreCase = true) }
                            val filteredFinance = finances.filter { it.category.contains(globalSearchQuery, ignoreCase = true) || it.notes.contains(globalSearchQuery, ignoreCase = true) }

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item { Text("Custom Notes Tracker matches (${filteredNotes.size})", color = Color.Gray, fontSize = 12.sp) }
                                items(filteredNotes) { note ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { viewModel.openWindow("notes") },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color.DarkGray)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(note.title, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(note.content, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                item { Text("To-Do Matrix matches (${filteredTasks.size})", color = Color.Gray, fontSize = 12.sp) }
                                items(filteredTasks) { task ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { viewModel.openWindow("tasks") },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color.DarkGray)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(task.title, color = Color.White, fontWeight = FontWeight.Bold)
                                            Text(task.description, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }

                                item { Text("Finance Matches (${filteredFinance.size})", color = Color.Gray, fontSize = 12.sp) }
                                items(filteredFinance) { fin ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { viewModel.openWindow("finance") },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(1.dp, Color.DarkGray)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("${fin.type.uppercase()} - $${fin.amount}", color = if (fin.type == "income") Color.Green else Color.Red, fontWeight = FontWeight.Bold)
                                            Text("Category: ${fin.category} (${fin.notes})", color = Color.LightGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Regular interactive desktop workspace with configurable widgets!
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Floating launcher shortcuts grid on left side
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LauncherAppIcon(title = "File Room", icon = Icons.Default.Folder, tag = "file_manager_launcher") { viewModel.openWindow("file_manager") }
                            LauncherAppIcon(title = "Memo Pad", icon = Icons.Default.Book, tag = "notes_launcher") { viewModel.openWindow("notes") }
                            LauncherAppIcon(title = "Task Matrix", icon = Icons.Default.CheckCircle, tag = "tasks_launcher") { viewModel.openWindow("tasks") }
                            LauncherAppIcon(title = "Finance", icon = Icons.Default.AttachMoney, tag = "finance_launcher") { viewModel.openWindow("finance") }
                            LauncherAppIcon(title = "Fitness Gym", icon = Icons.Default.DirectionsRun, tag = "fitness_launcher") { viewModel.openWindow("fitness") }
                            LauncherAppIcon(title = "Study Center", icon = Icons.Default.Timer, tag = "study_launcher") { viewModel.openWindow("study_tracker") }
                            LauncherAppIcon(title = "Settings", icon = Icons.Default.Settings, tag = "settings_launcher") { viewModel.openWindow("settings") }
                        }

                        // Search and index panel top center
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .widthIn(max = 280.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedTextField(
                                value = globalSearchQuery,
                                onValueChange = {
                                    globalSearchQuery = it
                                    isSearchFocused = true
                                },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.LightGray) },
                                placeholder = { Text("Search Workspace Files...", color = Color.Gray, fontSize = 12.sp) },
                                shape = RoundedCornerShape(24.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.3f),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color.DarkGray
                                ),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("workspace_search")
                            )
                        }

                        // WIDGET LAYER (Renders active widgets that can be dragged!)
                        widgets.filter { it.isVisible }.forEach { widget ->
                            DraggableWidgetCard(
                                widget = widget,
                                viewModel = viewModel,
                                content = {
                                    RenderWidgetBody(
                                        type = widget.type,
                                        viewModel = viewModel,
                                        notes = notes,
                                        tasks = tasks,
                                        finances = finances,
                                        fitnessLogs = fitnessLogs,
                                        habits = habits
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // 3. SECURED MICROPHONE TRIGGER & VOICE OPERATING CONSOLE
            VoiceOSConsolePanel(viewModel = viewModel)
        }

        // 4. FLOATING ACTIVE APP WINDOW (Acts like popup OS programs!)
        AnimatedVisibility(
            visible = activeWindow != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            if (activeWindow != null) {
                OSWindowOverlay(
                    title = activeWindow!!.capitalize(),
                    windowId = activeWindow!!,
                    onClose = { viewModel.openWindow(null) },
                    content = {
                        when (activeWindow) {
                            "settings" -> SettingsAppWindow(viewModel = viewModel, widgets = widgets)
                            "notes" -> NotesAppWindow(viewModel = viewModel, notes = notes)
                            "tasks" -> TasksAppWindow(viewModel = viewModel, tasks = tasks)
                            "finance" -> FinanceAppWindow(viewModel = viewModel, finances = finances)
                            "fitness" -> FitnessAppWindow(viewModel = viewModel, logs = fitnessLogs)
                            "study_tracker" -> StudyCenterAppWindow(viewModel = viewModel, habits = habits)
                            "file_manager" -> FileManagerAppWindow(viewModel = viewModel, notes = notes, finances = finances)
                            "camera" -> CameraSimAppWindow(viewModel = viewModel)
                        }
                    }
                )
            }
        }
    }
}

// ---------------- APP SHORTCUTS CONFIG ----------------
@Composable
fun LauncherAppIcon(title: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(tag)
            .width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                shadow = Shadow(color = Color.Black, blurRadius = 2f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

// ---------------- SECURED INTERACTIVE DRAGGABLE WIDGETS ----------------
@Composable
fun DraggableWidgetCard(
    widget: WidgetEntity,
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(widget.posX) }
    var offsetY by remember { mutableStateOf(widget.posY) }

    val shape = RoundedCornerShape(28.dp)

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(
                when (widget.sizeClass) {
                    "compact" -> 160.dp
                    "medium" -> 220.dp
                    else -> 320.dp
                }
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    // Update database layout state on complete drag
                    viewModel.updateWidgetLayout(
                        id = widget.id,
                        posX = offsetX,
                        posY = offsetY,
                        sizeClass = widget.sizeClass,
                        colorHex = widget.colorHex,
                        opacity = widget.opacity,
                        isVisible = widget.isVisible
                    )
                }
            }
            .clip(shape)
            .background(com.example.ui.theme.ImmersiveSurface)
            .border(1.dp, com.example.ui.theme.ImmersiveOutline, shape)
            .testTag("widget_${widget.id}")
            .padding(16.dp)
    ) {
        Column {
            // Widget Header Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (widget.type) {
                            "study_tracker" -> Icons.Default.Timeline
                            "habit_tracker" -> Icons.Default.CheckCircle
                            "fitness_tracker" -> Icons.Default.TrendingUp
                            "expense_tracker" -> Icons.Default.PieChart
                            "todo_list" -> Icons.Default.ViewAgenda
                            "notes" -> Icons.Default.StickyNote2
                            "calendar" -> Icons.Default.CalendarToday
                            "pomodoro" -> Icons.Default.HourglassTop
                            else -> Icons.Default.Widgets
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = com.example.ui.theme.ImmersivePrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = widget.title.uppercase(),
                        color = com.example.ui.theme.ImmersivePrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Hot actions: Toggle size / Close
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val nextSize = when (widget.sizeClass) {
                                "compact" -> "medium"
                                "medium" -> "expanded"
                                else -> "compact"
                            }
                            viewModel.updateWidgetLayout(
                                id = widget.id,
                                posX = offsetX,
                                posY = offsetY,
                                sizeClass = nextSize,
                                colorHex = widget.colorHex,
                                opacity = widget.opacity,
                                isVisible = widget.isVisible
                            )
                        },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = "Resize", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { viewModel.toggleWidgetVisibility(widget.id, false) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Widget Content Body
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

// ---------------- RENDER WIDGET CONFIGS ----------------
@Composable
fun RenderWidgetBody(
    type: String,
    viewModel: AppViewModel,
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    finances: List<FinanceEntity>,
    fitnessLogs: List<FitnessEntity>,
    habits: List<HabitEntity>
) {
    when (type) {
        "pomodoro" -> {
            val seconds by viewModel.pomodoroSeconds.collectAsStateWithLifecycle()
            val running by viewModel.isPomodoroRunning.collectAsStateWithLifecycle()
            val mode by viewModel.pomodoroMode.collectAsStateWithLifecycle()

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = if (mode == "WORK") "FOCUS" else "BREAK", color = if (mode == "WORK") Color(0xFFEF4444) else Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                val mins = seconds / 60
                val secs = seconds % 60
                val timeStr = String.format("%02d:%02d", mins, secs)
                Text(
                    text = timeStr,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(
                        onClick = { viewModel.startStopPomodoro() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(if (running) "Pause" else "Start", fontSize = 10.sp, color = Color.White)
                    }
                    Button(
                        onClick = { viewModel.resetPomodoro() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("Reset", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
        "todo_list" -> {
            val uncompleted = tasks.filter { !it.isCompleted }.take(3)
            Column {
                if (uncompleted.isEmpty()) {
                    Text("All tasks sorted! Well done.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    uncompleted.forEach { task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTaskCompleted(task.id, task.isCompleted) },
                                modifier = Modifier.size(24.dp),
                                colors = CheckboxDefaults.colors(uncheckedColor = Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        "expense_tracker" -> {
            val totalIncome = finances.filter { it.type == "income" }.sumOf { it.amount }
            val totalExpense = finances.filter { it.type == "expense" }.sumOf { it.amount }
            val savings = totalIncome - totalExpense

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Savings:", color = Color.Gray, fontSize = 10.sp)
                    Text("$${String.format("%.1f", savings)}", color = if (savings >= 0) Color(0xFF10B981) else Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Expense:", color = Color.Gray, fontSize = 10.sp)
                    Text("$${String.format("%.1f", totalExpense)}", color = Color(0xFFF59E0B), fontSize = 11.sp)
                }
            }
        }
        "study_tracker" -> {
            val currentTimerHabit by viewModel.activeTrackedHabit.collectAsStateWithLifecycle()
            val runningTimer by viewModel.isTrackedHabitRunning.collectAsStateWithLifecycle()
            val secondsTimer by viewModel.trackedHabitSeconds.collectAsStateWithLifecycle()

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                if (currentTimerHabit == null) {
                    Text("No Active Tracked Habit. Tap Study Center launcher to start.", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center)
                } else {
                    Text(currentTimerHabit!!.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    val hrs = secondsTimer / 3600
                    val mins = (secondsTimer % 3600) / 60
                    val secs = secondsTimer % 60
                    Text(
                        text = String.format("%02d:%02d:%02d", hrs, mins, secs),
                        color = Color(0xFF10B981),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    IconButton(onClick = { viewModel.toggleTrackedHabitTimer() }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (runningTimer) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Trigger tracking",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        "fitness_tracker" -> {
            // Display stats step counts
            val stepsToday = fitnessLogs.filter { it.type == "Steps" }.sumOf { it.value }.toInt()
            val targetSteps = 10000
            val ratio = if (targetSteps > 0) stepsToday.toFloat() / targetSteps else 0f

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Steps Today:", color = Color.LightGray, fontSize = 11.sp)
                    Text("$stepsToday / $targetSteps", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { ratio.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF06B6D4),
                    trackColor = Color.DarkGray
                )
            }
        }
        "habit_tracker" -> {
            Column {
                habits.take(2).forEach { habit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(habit.label, color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("🔥 ${habit.streak}d", color = Color(0xFFFF8C00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { viewModel.logHabitProgress(habit.id, 1.0) }, modifier = Modifier.size(16.dp)) {
                            Icon(Icons.Default.Add, contentDescription = "Log", tint = Color.Green, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
        "notes" -> {
            Column {
                if (notes.isEmpty()) {
                    Text("Notes disk is empty.", color = Color.Gray, fontSize = 10.sp)
                } else {
                    val first = notes.first()
                    Text(first.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(first.content, color = Color.LightGray, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        else -> {
            Text("OS Local Sandbox widget online.", color = Color.Gray, fontSize = 10.sp)
        }
    }
}

// ---------------- VOICE OS SYSTEM BOTTOM CONSOLE ----------------
@Composable
fun VoiceOSConsolePanel(viewModel: AppViewModel) {
    val currentInput by viewModel.currentCommandInput.collectAsStateWithLifecycle()
    val assistantFeedback by viewModel.assistantResponse.collectAsStateWithLifecycle()
    val isPulseListening by viewModel.isListening.collectAsStateWithLifecycle()
    val pendingAction by viewModel.pendingConfirmationAction.collectAsStateWithLifecycle()

    var manualInputText by remember { mutableStateOf("") }
    val isKeyboardVisible = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(com.example.ui.theme.ImmersiveSurfaceVariant)
            .border(BorderStroke(1.dp, com.example.ui.theme.ImmersiveOutline), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .padding(20.dp)
    ) {
        // Voice Console Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio Pulsing Status Indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isPulseListening) Color.Red else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hermes Voice OS (Offline-First)",
                        color = com.example.ui.theme.ImmersiveTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Offline local semantic engine actively waiting for commands.",
                    color = com.example.ui.theme.ImmersiveTextSecondary,
                    fontSize = 11.sp
                )
            }

            // Keyboard/Dictation toggle
            IconButton(
                onClick = { isKeyboardVisible.value = !isKeyboardVisible.value },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(com.example.ui.theme.ImmersiveSurface)
            ) {
                Icon(
                    imageVector = if (isKeyboardVisible.value) Icons.Default.KeyboardHide else Icons.Default.Keyboard,
                    contentDescription = "Manual Console Mode",
                    tint = com.example.ui.theme.ImmersivePrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Confirmation required flashing banner
        if (pendingAction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7F1D1D))
                    .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = "Security Alert", tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Voice confirmation needed: Safe disk wipe action requested. Confirm with: Yes / Aama", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Response bubble
        if (assistantFeedback.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(com.example.ui.theme.ImmersiveSurface)
                    .border(1.dp, com.example.ui.theme.ImmersiveOutline, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = assistantFeedback,
                    color = com.example.ui.theme.ImmersivePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("assistant_response")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interaction bar: standard micro trigger + direct typing fallback
        if (isKeyboardVisible.value) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualInputText,
                    onValueChange = { manualInputText = it },
                    placeholder = { Text("Command input (Tamil/English/Numbers)", color = com.example.ui.theme.ImmersiveTextSecondary, fontSize = 12.sp) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = com.example.ui.theme.ImmersiveSurface,
                        unfocusedContainerColor = com.example.ui.theme.ImmersiveSurface,
                        focusedTextColor = com.example.ui.theme.ImmersiveTextPrimary,
                        unfocusedTextColor = com.example.ui.theme.ImmersiveTextPrimary,
                        focusedBorderColor = com.example.ui.theme.ImmersivePrimary,
                        unfocusedBorderColor = com.example.ui.theme.ImmersiveOutline
                    ),
                    modifier = Modifier.weight(1f).testTag("console_text_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        viewModel.processVoiceCommandText(manualInputText)
                        manualInputText = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.example.ui.theme.ImmersivePrimary,
                        contentColor = com.example.ui.theme.ImmersiveOnPrimary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(48.dp).testTag("submit_voice_command")
                ) {
                    Text("EXEC", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Simulated Voice input presets so emulator user can experience Tamil & English voice parsing effortlessly
            Text("Try simulated spoken commands (Tap to trigger):", color = com.example.ui.theme.ImmersiveTextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceCommandPresetChip("study start pannu", viewModel)
                VoiceCommandPresetChip("notes open pannu", viewModel)
                VoiceCommandPresetChip("Spent 150 for food", viewModel)
                VoiceCommandPresetChip("Add income 5000 from work", viewModel)
                VoiceCommandPresetChip("delete notes", viewModel)
                VoiceCommandPresetChip("open settings", viewModel)
                VoiceCommandPresetChip("camera open pannu", viewModel)
                VoiceCommandPresetChip("take photo", viewModel)
            }
        }
    }
}

@Composable
fun VoiceCommandPresetChip(text: String, viewModel: AppViewModel) {
    Box(
        modifier = Modifier
            .clickable { viewModel.processVoiceCommandText(text) }
            .clip(RoundedCornerShape(24.dp))
            .background(com.example.ui.theme.ImmersiveSurface)
            .border(1.dp, com.example.ui.theme.ImmersiveOutline, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = com.example.ui.theme.ImmersivePrimary, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, color = com.example.ui.theme.ImmersiveTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ---------------- MODERN OS FLOATING WINDOW CONTAINER ----------------
@Composable
fun OSWindowOverlay(
    title: String,
    windowId: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .testTag("window_${windowId}")
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Window Top drag-handle / Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close Window", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                // Window app container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

// ---------------- SETTINGS PROGRAM APP ----------------
@Composable
fun SettingsAppWindow(viewModel: AppViewModel, widgets: List<WidgetEntity>) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val wallpaper by viewModel.wallpaperStyle.collectAsStateWithLifecycle()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Wallpaper Sandbox Customization", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WallpaperPickerBox("Slate Dust Space", 0, wallpaper, onClick = { viewModel.setWallpaperStyle(0) })
                WallpaperPickerBox("Neon Cyber Sunset", 1, wallpaper, onClick = { viewModel.setWallpaperStyle(1) })
                WallpaperPickerBox("Forest Zen Dark", 2, wallpaper, onClick = { viewModel.setWallpaperStyle(2) })
                WallpaperPickerBox("Obsidian Minimal", 3, wallpaper, onClick = { viewModel.setWallpaperStyle(3) })
            }
        }

        item {
            Divider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Workspace Widgets Manager Toggle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(widgets) { widget ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(widget.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Type: ${widget.type.capitalize()} layout", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = widget.isVisible,
                    onCheckedChange = { viewModel.toggleWidgetVisibility(widget.id, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                )
            }
        }

        item {
            Divider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("System Disk Database Diagnostics", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color.DarkGray)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Backend DB Core: SQLite Embedded", color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Encryption status: Local Sandboxed Hardware Crypt", color = Color.White, fontSize = 11.sp)
                    Text("Sync services: Cloud Optional (Drive sync ready)", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun WallpaperPickerBox(name: String, style: Int, activeStyle: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(getWallpaperBrush(style))
            .border(
                if (style == activeStyle) 2.dp else 1.dp,
                if (style == activeStyle) Color(0xFF10B981) else Color.DarkGray,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            name,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(shadow = Shadow(color = Color.Black, blurRadius = 2f))
        )
    }
}

// ---------------- NOTES PROGRAM APP ----------------
@Composable
fun NotesAppWindow(viewModel: AppViewModel, notes: List<NoteEntity>) {
    var noteTitle by remember { mutableStateOf("") }
    var noteContent by remember { mutableStateOf("") }
    val currentFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()

    val filteredNotes = notes.filter { it.folder == currentFolder }

    Row(modifier = Modifier.fillMaxSize()) {
        // Folders side drawers
        Column(
            modifier = Modifier
                .width(100.dp)
                .fillMaxHeight()
                .border(1.dp, Color.DarkGray)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("File Folders", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            folders.forEach { f ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (f == currentFolder) Color(0xFF2563EB) else Color.Transparent)
                        .clickable { viewModel.selectFolder(f) }
                        .padding(8.dp)
                ) {
                    Text(f, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Notes Composer and Reader list
        Column(modifier = Modifier.weight(1f)) {
            // Quick note generator
            Text("New workspace log entry of category $currentFolder:", color = Color.LightGray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                placeholder = { Text("Log Title", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = noteContent,
                onValueChange = { noteContent = it },
                placeholder = { Text("Content memo specifics...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                textStyle = TextStyle(color = Color.White)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    if (noteTitle.isNotEmpty()) {
                        viewModel.addNote(noteTitle, noteContent, currentFolder)
                        noteTitle = ""
                        noteContent = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save Entry", color = Color.White, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Disk Files list:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredNotes) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(note.content, color = Color.LightGray, fontSize = 11.sp)
                            }
                            IconButton(onClick = { viewModel.deleteNoteById(note.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Trash", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TO-DO PROGRAM APP ----------------
@Composable
fun TasksAppWindow(viewModel: AppViewModel, tasks: List<TaskEntity>) {
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var pointsReward by remember { mutableStateOf("15") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Create custom task with reward points:", color = Color.LightGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                placeholder = { Text("Task Heading", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
            OutlinedTextField(
                value = pointsReward,
                onValueChange = { pointsReward = it },
                placeholder = { Text("XP Price", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.width(80.dp).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = taskDesc,
            onValueChange = { taskDesc = it },
            placeholder = { Text("Task description...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            textStyle = TextStyle(color = Color.White)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChip("HIGH", priority) { priority = "HIGH" }
                PriorityChip("MEDIUM", priority) { priority = "MEDIUM" }
                PriorityChip("LOW", priority) { priority = "LOW" }
            }
            Button(
                onClick = {
                    if (taskTitle.isNotEmpty()) {
                        viewModel.addTask(taskTitle, taskDesc, priority, pointsReward.toIntOrNull() ?: 10, 1)
                        taskTitle = ""
                        taskDesc = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Launch Task", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Task Backlog Boards:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks.size) { index ->
                val task = tasks[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (task.isCompleted) Color(0xFF065F46) else Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(task.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { viewModel.toggleTaskCompleted(task.id, task.isCompleted) },
                                colors = CheckboxDefaults.colors(checkedColor = Color.Green)
                            )
                        }
                        Text(task.description, color = Color.LightGray, fontSize = 10.sp, maxLines = 2)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (task.priority) {
                                            "HIGH" -> Color.Red
                                            "MEDIUM" -> Color.Yellow
                                            else -> Color.Green
                                        }.copy(alpha = 0.2f)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(task.priority, color = Color.White, fontSize = 8.sp)
                            }
                            Text("+${task.points}XP", color = Color(0xFF3B82F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityChip(name: String, active: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active == name) Color.White.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------- FINANCE PROGRAM APP ----------------
@Composable
fun FinanceAppWindow(viewModel: AppViewModel, finances: List<FinanceEntity>) {
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var transType by remember { mutableStateOf("expense") }

    val totalIncome = finances.filter { it.type == "income" }.sumOf { it.amount }
    val totalExpense = finances.filter { it.type == "expense" }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FinanceStatSum("Net Ledger", "$${String.format("%.1f", netSavings)}", if (netSavings >= 0) Color.Green else Color.Red)
            FinanceStatSum("Inbound Value", "$${String.format("%.1f", totalIncome)}", Color.Green)
            FinanceStatSum("Outbound Spent", "$${String.format("%.1f", totalExpense)}", Color.Yellow)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Add New Ledger record:", color = Color.LightGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                placeholder = { Text("Amount ($)", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
            OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it },
                placeholder = { Text("Category", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = notesText,
            onValueChange = { notesText = it },
            placeholder = { Text("Optional description notes...", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            textStyle = TextStyle(color = Color.White)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChip("EXPENSE", if (transType == "expense") "EXPENSE" else "") { transType = "expense" }
                PriorityChip("INCOME", if (transType == "income") "INCOME" else "") { transType = "income" }
            }
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && categoryText.isNotEmpty()) {
                        viewModel.addFinanceRecord(amt, categoryText, transType, notesText)
                        amountText = ""
                        categoryText = ""
                        notesText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
            ) {
                Text("Log Ledger", color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Recent Ledger History Logs:", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(finances) { fin ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(fin.category, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(fin.notes, color = Color.Gray, fontSize = 10.sp)
                    }
                    Text(
                        text = if (fin.type == "income") "+$${fin.amount}" else "-$${fin.amount}",
                        color = if (fin.type == "income") Color.Green else Color.Red,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceStatSum(label: String, valuelS: String, clr: Color) {
    Card(
        modifier = Modifier.width(96.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.Gray, fontSize = 9.sp)
            Text(valuelS, color = clr, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------- FITNESS HUB APP ----------------
@Composable
fun FitnessAppWindow(viewModel: AppViewModel, logs: List<FitnessEntity>) {
    var typeSelection by remember { mutableStateOf("Steps") }
    var trackingValue by remember { mutableStateOf("") }

    val stepsLogged = logs.filter { it.type == "Steps" }.sumOf { it.value }.toInt()
    val waterLogged = logs.filter { it.type == "Water" }.sumOf { it.value }.toInt()
    val pushupsLogged = logs.filter { it.type == "Push-ups" }.sumOf { it.value }.toInt()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FitnessMetricBox("Steps", "$stepsLogged steps", Color(0xFF06B6D4))
            FitnessMetricBox("Water Intake", "$waterLogged ml", Color(0xFF3B82F6))
            FitnessMetricBox("Pushups Completed", "$pushupsLogged done", Color(0xFFEC4899))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Add active biomeasurement:", color = Color.LightGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = trackingValue,
                    onValueChange = { trackingValue = it },
                    placeholder = { Text("Goal unit measure (e.g., 2000)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = TextStyle(color = Color.White)
                )
            }

            Button(
                onClick = {
                    val valF = trackingValue.toDoubleOrNull() ?: 0.0
                    if (valF > 0) {
                        viewModel.addFitnessLog(typeSelection, valF)
                        trackingValue = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
            ) {
                Text("Log Metrics", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityChip("Steps", if (typeSelection == "Steps") "Steps" else "") { typeSelection = "Steps" }
            PriorityChip("Water", if (typeSelection == "Water") "Water" else "") { typeSelection = "Water" }
            PriorityChip("Pushups", if (typeSelection == "Push-ups") "Pushups" else "") { typeSelection = "Push-ups" }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Fitness Activity Log history:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(logs) { log ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(log.type, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${log.value.toInt()}", color = Color(0xFF06B6D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FitnessMetricBox(label: String, scoreLabel: String, tintCol: Color) {
    Card(
        modifier = Modifier.width(96.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = tintCol, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, color = Color.Gray, fontSize = 8.sp, textAlign = TextAlign.Center)
            Text(scoreLabel, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

// ---------------- STUDY CENTER APP ----------------
@Composable
fun StudyCenterAppWindow(viewModel: AppViewModel, habits: List<HabitEntity>) {
    var habitLabel by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Define dynamic tracked habit or study metrics:", color = Color.LightGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = habitLabel,
                onValueChange = { habitLabel = it },
                placeholder = { Text("Label (e.g., DSA Focus)", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it },
                placeholder = { Text("Target", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.width(80.dp).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Button(
            onClick = {
                val targVal = targetValue.toDoubleOrNull() ?: 5.0
                if (habitLabel.isNotEmpty()) {
                    viewModel.addHabit(habitLabel, "COUNT", targVal, "#E91E63")
                    habitLabel = ""
                    targetValue = ""
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Create Tracker", fontSize = 11.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Personal Trackers Matrix (Tap to start Timer session):", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(habits) { h ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.selectHabitForTimer(h) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(h.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Streak: 🔥 ${h.streak} days", color = Color(0xFFFF8C00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val progressRatio = if (h.targetValue > 0) h.currentValue / h.targetValue else 0.0
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Progress: ${h.currentValue.toInt()} / ${h.targetValue.toInt()}", color = Color.LightGray, fontSize = 10.sp)
                            Text("${(progressRatio * 100).toInt()}% Done", color = Color.Green, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressRatio.toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFFEF4444),
                            trackColor = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

// ---------------- FILE MANAGER APP ----------------
@Composable
fun FileManagerAppWindow(viewModel: AppViewModel, notes: List<NoteEntity>, finances: List<FinanceEntity>) {
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()
    var folderNameText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Create custom Workspace Directory Folder:", color = Color.LightGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = folderNameText,
                onValueChange = { folderNameText = it },
                placeholder = { Text("Folder Name", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.weight(1f).height(48.dp),
                textStyle = TextStyle(color = Color.White)
            )
            Button(
                onClick = {
                    if (folderNameText.isNotEmpty()) {
                        viewModel.createFolder(folderNameText)
                        folderNameText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Create", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Available Secure Directory Structure:", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(folders.size) { index ->
                val f = folders[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color.DarkGray)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(f, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        // Display count of local elements inside
                        val count = remember(notes, finances, f) {
                            if (f == "Finance Receipts") finances.size else notes.filter { it.folder == f }.size
                        }
                        Text("$count items", color = Color.Gray, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// ---------------- CAMERA WORKSPACE APP SIMULATOR ----------------
@Composable
fun CameraSimAppWindow(viewModel: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Camera, contentDescription = null, tint = Color.Green, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("WORKSPACE LENS SUBSYSTEM ONLINE", color = Color.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Hardware Sandboxed - 100% Offline Secured", color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    viewModel.addNote("Snapshot_$stamp", "Secured workspace photo taken in app camera.", "Photos")
                    viewModel.openWindow(null)
                    Toast.makeText(viewModel.getApplication(), "Captured! Saved in Photos folder.", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SNAP CAPTURE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
