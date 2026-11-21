package com.davion.taskcommand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AiStudioScreen() {
    var mode by remember { mutableStateOf("CHAT") }
    var prompt by remember { mutableStateOf("") }
    val modes = listOf("CHAT", "TEXT", "IMAGE")
    Column(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DAVION AI STUDIO", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF9AF4FF))
        Text("One creative workspace for conversation, writing and image prompts.", color = Color(0xFF8BA9BA))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { item ->
                Button(onClick = { mode = item }) { Text(item) }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f).border(1.dp, Color(0x5535DDF5), RoundedCornerShape(18.dp)),
            color = Color(0xCC0B1B2B),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    when (mode) {
                        "CHAT" -> "AI CONVERSATION"
                        "TEXT" -> "TEXT GENERATOR"
                        else -> "IMAGE GENERATOR"
                    },
                    color = Color(0xFF35DDF5)
                )
                Text(
                    when (mode) {
                        "CHAT" -> "Ask questions or have a conversation with Davion."
                        "TEXT" -> "Draft letters, summaries, stories, plans or code."
                        else -> "Describe the image you want to create. A connected image-generation provider is required to render it."
                    },
                    color = Color(0xFF9AF4FF)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Provider status: NOT CONFIGURED",
                    color = Color(0xFFFFC66D),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "This screen is the integration foundation. It does not pretend to generate AI output until a provider is connected.",
                    color = Color(0xFF8BA9BA),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(if (mode == "IMAGE") "Describe an image..." else "Enter your prompt...") },
            minLines = 2
        )
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            enabled = false
        ) {
            Text(if (mode == "IMAGE") "IMAGE PROVIDER REQUIRED" else "AI PROVIDER REQUIRED")
        }
    }
}
