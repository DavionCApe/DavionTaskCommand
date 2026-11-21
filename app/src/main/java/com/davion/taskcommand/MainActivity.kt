package com.davion.taskcommand

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Night = Color(0xFF050B16)
private val Panel = Color(0xCC0B1B2B)
private val Cyan = Color(0xFF35DDF5)
private val Ice = Color(0xFF9AF4FF)
private val Muted = Color(0xFF8BA9BA)

data class TaskItem(val id: Long, val title: String, val priority: String, val done: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { JarvisTaskApp() }
    }
}

@Composable
private fun JarvisTaskApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("jarvis_tasks", 0) }
    val tasks = remember {
        mutableStateListOf<TaskItem>().apply {
            try {
                val array = JSONArray(prefs.getString("items", "[]"))
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(TaskItem(o.optLong("id"), o.optString("title"), o.optString("priority", "Normal"), o.optBoolean("done")))
                }
            } catch (_: Exception) { }
        }
    }
    fun persist() {
        val array = JSONArray()
        tasks.forEach { t ->
            array.put(JSONObject().put("id", t.id).put("title", t.title).put("priority", t.priority).put("done", t.done))
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    var expanded by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("HOME") }
    var showAdd by remember { mutableStateOf(false) }
    var newTask by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var status by remember { mutableStateOf("SYSTEM ONLINE • READY") }
    var voiceText by remember { mutableStateOf("") }

    fun runCommand(raw: String) {
        val command = raw.trim()
        voiceText = command
        val lower = command.lowercase()
        when {
            lower.startsWith("add task ") || lower.startsWith("create task ") -> {
                val title = command.substringAfter("task ", "").trim()
                if (title.isNotBlank()) {
                    tasks.add(TaskItem(System.currentTimeMillis(), title, "Normal"))
                    persist()
                    page = "TASKS"
                    status = "TASK CREATED"
                }
            }
            lower.contains("show task") || lower.contains("my tasks") -> { page = "TASKS"; status = "TASK LIST OPEN" }
            lower.contains("schedule") || lower.contains("calendar") -> { page = "SCHEDULE"; status = "SCHEDULE OPEN" }
            lower.contains("project") -> { page = "PROJECTS"; status = "PROJECTS OPEN" }
            lower.contains("reminder") -> { page = "REMINDERS"; status = "REMINDERS OPEN" }
            lower.contains("analytics") || lower.contains("progress") -> { page = "ANALYTICS"; status = "ANALYTICS OPEN" }
            lower == "home" || lower.contains("go home") -> { page = "HOME"; status = "HOME DISPLAYED" }
            else -> status = "COMMAND RECEIVED • TRY “ADD TASK …”"
        }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) runCommand(spoken) else status = "NO VOICE INPUT RECEIVED"
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Ice, background = Night, surface = Panel,
        onPrimary = Night, onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF07182A), Night, Color(0xFF02060D)))
        )) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("JARVIS", color = Ice, fontSize = 12.sp, letterSpacing = 5.sp, fontWeight = FontWeight.Bold)
                        Text("TASK COMMAND", color = Color.White, fontSize = 20.sp, letterSpacing = 2.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DAVION SYSTEMS", color = Muted, fontSize = 9.sp, letterSpacing = 2.sp)
                        Text("● ENCRYPTED LOCAL", color = Cyan, fontSize = 9.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Cyan))
                    Spacer(Modifier.width(8.dp))
                    Text(status, color = Cyan, fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
                Spacer(Modifier.height(12.dp))

                if (page == "HOME") {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ReactorOrb(Modifier.size(260.dp).clickable { expanded = !expanded })
                        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TOUCH THE CORE", color = Ice, fontSize = 11.sp, letterSpacing = 3.sp)
                            Text("Tap the reactor to access command modules", color = Muted, fontSize = 10.sp)
                        }
                        if (expanded) {
                            Column(
                                Modifier.align(Alignment.Center).padding(top = 250.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val rows = listOf(
                                    listOf("TASKS", "SCHEDULE", "PROJECTS"),
                                    listOf("REMINDERS", "ANALYTICS", "SETTINGS")
                                )
                                rows.forEach { row ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { label ->
                                            ModuleTile(label, Modifier.weight(1f)) {
                                                page = label
                                                expanded = false
                                                status = "$label MODULE ACTIVE"
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("TOTAL TASKS", tasks.size.toString(), Modifier.weight(1f))
                        MetricCard("COMPLETED", tasks.count { it.done }.toString(), Modifier.weight(1f))
                        MetricCard("REMAINING", tasks.count { !it.done }.toString(), Modifier.weight(1f))
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(page, color = Ice, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { page = "HOME"; status = "HOME DISPLAYED" }) { Text("← HOME", color = Cyan) }
                    }
                    Spacer(Modifier.height(10.dp))
                    when (page) {
                        "TASKS" -> {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("Your active task queue", color = Muted, fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                FilledTonalButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, null); Text(" ADD") }
                            }
                            Spacer(Modifier.height(8.dp))
                            if (tasks.isEmpty()) EmptyPanel("NO TASKS YET", "Add a task or say “JARVIS, add task …”")
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(tasks, key = { it.id }) { task ->
                                    TaskRow(task, onToggle = {
                                        val index = tasks.indexOfFirst { it.id == task.id }
                                        if (index >= 0) tasks[index] = task.copy(done = !task.done)
                                        persist()
                                    }, onDelete = { tasks.removeAll { it.id == task.id }; persist() })
                                }
                            }
                        }
                        "SCHEDULE" -> EmptyPanel("SCHEDULE MODULE", "Your task deadlines and calendar planning will appear here.")
                        "PROJECTS" -> EmptyPanel("PROJECT CONTROL", "Group related tasks into projects. Project workspace is coming in the next build.")
                        "REMINDERS" -> EmptyPanel("REMINDER CONTROL", "Task reminders and notification scheduling are planned for the next build.")
                        "ANALYTICS" -> {
                            MetricCard("COMPLETION RATE", if (tasks.isEmpty()) "0%" else "${tasks.count { it.done } * 100 / tasks.size}%", Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            MetricCard("OPEN TASKS", tasks.count { !it.done }.toString(), Modifier.fillMaxWidth())
                        }
                        else -> EmptyPanel("SYSTEM SETTINGS", "Local task storage is enabled. Voice input uses the Android speech service available on this device.")
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                        .background(Panel).border(1.dp, Cyan.copy(alpha = .45f), RoundedCornerShape(18.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("VOICE COMMAND", color = Ice, fontSize = 10.sp, letterSpacing = 2.sp)
                        Text(if (voiceText.isBlank()) "Speak to JARVIS" else voiceText, color = Color.White, fontSize = 12.sp, maxLines = 1)
                    }
                    IconButton(onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-ZA")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a JARVIS task command")
                            }
                            voiceLauncher.launch(intent)
                        } catch (_: Exception) { status = "SPEECH SERVICE NOT AVAILABLE" }
                    }) {
                        Icon(Icons.Default.Mic, "Speak", tint = Cyan, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("OFFLINE TASK STORAGE  •  VOICE INPUT DEPENDS ON DEVICE SPEECH SUPPORT", Modifier.fillMaxWidth().padding(bottom = 12.dp), color = Muted, fontSize = 8.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp)
            }

            if (showAdd) {
                AlertDialog(
                    onDismissRequest = { showAdd = false },
                    title = { Text("NEW TASK", color = Ice) },
                    text = {
                        Column {
                            OutlinedTextField(value = newTask, onValueChange = { newTask = it }, label = { Text("Task description") }, singleLine = true)
                            Spacer(Modifier.height(10.dp))
                            Text("PRIORITY", color = Muted, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Low", "Normal", "High", "Critical").forEach {
                                    FilterChip(selected = priority == it, onClick = { priority = it }, label = { Text(it, fontSize = 10.sp) })
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (newTask.isNotBlank()) {
                                tasks.add(TaskItem(System.currentTimeMillis(), newTask.trim(), priority))
                                persist()
                                newTask = ""
                                priority = "Normal"
                            }
                            showAdd = false
                        }) { Text("CREATE", color = Cyan) }
                    },
                    dismissButton = { TextButton(onClick = { showAdd = false }) { Text("CANCEL") } },
                    containerColor = Color(0xFF102437)
                )
            }
        }
    }
}

@Composable
private fun ReactorOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "reactor")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(9000), RepeatMode.Restart), label = "rotation")
    val pulse by transition.animateFloat(.88f, 1.06f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "pulse")
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension * .31f * pulse
        drawCircle(Brush.radialGradient(listOf(Ice.copy(alpha = .32f), Cyan.copy(alpha = .13f), Color.Transparent), c, radius * 2.1f), radius * 2.1f, c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFBDFBFF), Color(0xFF168FAF), Color(0xFF071A35)), c, radius), radius, c)
        drawCircle(Cyan.copy(alpha = .85f), radius * 1.28f, c, style = Stroke(width = 2.dp.toPx()))
        drawCircle(Ice.copy(alpha = .55f), radius * 1.52f, c, style = Stroke(width = 1.dp.toPx(), pathEffect = null))
        drawArc(Cyan, rotation, 105f, false, topLeft = Offset(c.x - radius * 1.65f, c.y - radius * 1.65f), size = androidx.compose.ui.geometry.Size(radius * 3.3f, radius * 3.3f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        drawArc(Ice.copy(alpha = .7f), -rotation * .7f, 65f, false, topLeft = Offset(c.x - radius * 1.82f, c.y - radius * 1.82f), size = androidx.compose.ui.geometry.Size(radius * 3.64f, radius * 3.64f), style = Stroke(width = 1.dp.toPx()))
        for (i in 0..11) {
            val a = Math.toRadians((i * 30.0) + rotation)
            val r = radius * 1.9f
            drawCircle(Cyan.copy(alpha = .8f), 2.5.dp.toPx(), Offset(c.x + kotlin.math.cos(a).toFloat() * r, c.y + kotlin.math.sin(a).toFloat() * r))
        }
    }
}

@Composable
private fun ModuleTile(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val icon = when (label) {
        "TASKS" -> Icons.Default.TaskAlt
        "SCHEDULE" -> Icons.Default.CalendarMonth
        "REMINDERS" -> Icons.Default.Notifications
        "ANALYTICS" -> Icons.Default.Analytics
        else -> Icons.Default.Settings
    }
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(Panel)
            .border(1.dp, Cyan.copy(alpha = .5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(vertical = 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White, fontSize = 9.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, Cyan.copy(alpha = .3f), RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
        Text(value, color = Ice, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyPanel(title: String, detail: String) {
    Column(Modifier.fillMaxWidth().padding(top = 24.dp).clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, Cyan.copy(alpha = .35f), RoundedCornerShape(18.dp)).padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.CheckCircle, null, tint = Cyan, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = Ice, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(6.dp))
        Text(detail, color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TaskRow(task: TaskItem, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Panel).border(1.dp, Cyan.copy(alpha = .25f), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = Cyan, uncheckedColor = Muted))
        Column(Modifier.weight(1f)) {
            Text(task.title, color = if (task.done) Muted else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("PRIORITY • ${task.priority.uppercase()}", color = Cyan, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Close, "Delete task", tint = Muted) }
    }
}
