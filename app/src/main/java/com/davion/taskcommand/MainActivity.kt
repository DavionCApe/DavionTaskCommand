package com.davion.taskcommand

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
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
data class ChatMessage(val fromUser: Boolean, val text: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DavionTaskApp() }
    }
}

@Composable
private fun DavionTaskApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("davion_tasks", 0) }
    val tts = remember { TextToSpeech(context) { } }
    DisposableEffect(tts) { onDispose { tts.shutdown() } }
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
    var chatInput by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf(
        ChatMessage(false, "Hello. I'm Davion, your personal task command assistant. I can organise tasks, show your workload and guide you through the app. What would you like to do?")
    ) }

    fun runCommand(raw: String) {
        val command = raw.trim()
        if (command.isBlank()) return
        voiceText = command
        chatMessages.add(ChatMessage(true, command))
        val lower = command.lowercase()
        var reply = ""
        when {
            lower.startsWith("add task ") || lower.startsWith("create task ") -> {
                val title = command.substringAfter("task ", "").trim()
                if (title.isNotBlank()) {
                    tasks.add(TaskItem(System.currentTimeMillis(), title, "Normal"))
                    persist()
                    page = "DAVION AI"
                    status = "TASK CREATED"
                    reply = "I've added “$title” to your task list with normal priority. Would you like to set a deadline or change its priority?"
                } else reply = "Tell me what the task is, for example: add task phone the dentist."
            }
            lower.contains("show task") || lower.contains("my tasks") || lower.contains("task list") -> {
                page = "TASKS"
                val open = tasks.count { !it.done }
                val done = tasks.count { it.done }
                reply = "Here's your task overview. You have $open open tasks and $done completed. I've opened your task list."
                status = "TASK LIST OPEN"
            }
            lower.contains("schedule") || lower.contains("calendar") -> {
                page = "SCHEDULE"
                reply = "I've opened your schedule module. Calendar and deadline planning are part of the next development stage."
                status = "SCHEDULE OPEN"
            }
            lower.contains("project") -> {
                page = "PROJECTS"
                reply = "Project control is open. You can use this area as your project workspace; project grouping is still being developed."
                status = "PROJECTS OPEN"
            }
            lower.contains("reminder") -> {
                page = "REMINDERS"
                reply = "I've opened reminders. Timed notifications are not active in this version yet."
                status = "REMINDERS OPEN"
            }
            lower.contains("analytics") || lower.contains("progress") || lower.contains("how am i doing") -> {
                page = "ANALYTICS"
                reply = "Your completion rate is ${if (tasks.isEmpty()) 0 else tasks.count { it.done } * 100 / tasks.size} percent. You have ${tasks.count { !it.done }} tasks still open. I've opened your analytics."
                status = "ANALYTICS OPEN"
            }
            lower == "home" || lower.contains("go home") || lower.contains("dashboard") -> {
                page = "HOME"
                reply = "Taking you back to your command dashboard."
                status = "HOME DISPLAYED"
            }
            lower.contains("hello") || lower.contains("hi davion") || lower == "hi" -> {
                page = "DAVION AI"
                reply = "Hello! I'm online as your on-device Davion assistant. I can help you manage your tasks and navigate the app. What should we work on?"
            }
            lower.contains("help") || lower.contains("what can you do") -> {
                page = "DAVION AI"
                reply = "I can create tasks, show your task list, open schedule, projects, reminders and analytics, and take you home. Try saying: add task buy groceries. More advanced AI reasoning and online chat are not connected yet."
            }
            else -> {
                page = "DAVION AI"
                reply = "I heard you say: “$command”. I can currently understand task-management and app-navigation requests. Try “add task …”, “show my tasks”, or “open analytics”. I don't want to pretend I have full generative AI connected when I don't."
                status = "DAVION ASSISTANT READY"
            }
        }
        chatMessages.add(ChatMessage(false, reply))
        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "davion-response")
    }


@Composable
private fun HudBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "hud-scan")
    val sweep by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5200), RepeatMode.Restart),
        label = "scan-position"
    )
    Canvas(modifier) {
        val step = 34.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(Color(0xFF2AAAC2).copy(alpha = .055f), Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(Color(0xFF2AAAC2).copy(alpha = .045f), Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        val scanY = size.height * sweep
        drawLine(Cyan.copy(alpha = .11f), Offset(0f, scanY), Offset(size.width, scanY), 2.dp.toPx())
        val inset = 12.dp.toPx()
        val arm = 24.dp.toPx()
        val corners = listOf(
            Offset(inset, inset), Offset(size.width - inset, inset),
            Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset)
        )
        corners.forEachIndexed { index, p ->
            val sx = if (index % 2 == 0) 1f else -1f
            val sy = if (index < 2) 1f else -1f
            drawLine(Cyan.copy(alpha = .32f), p, Offset(p.x + arm * sx, p.y), 2.dp.toPx())
            drawLine(Cyan.copy(alpha = .32f), p, Offset(p.x, p.y + arm * sy), 2.dp.toPx())
        }
        drawCircle(Cyan.copy(alpha = .07f), size.minDimension * .39f, Offset(size.width / 2, size.height * .43f), style = Stroke(width = 1.dp.toPx()))
    }
}
