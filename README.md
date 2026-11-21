# DAVION AI TASK COMMAND

A standalone Android task manager with a futuristic holographic command interface and a conversational voice assistant.

## Current implementation
- Kotlin + Jetpack Compose Android app, minimum Android 8.0 (API 26).
- Animated holographic HUD grid, scanning sweep, reactor-style command core and floating module tiles.
- Davion AI chat screen with conversation bubbles, text input, voice input and spoken replies.
- Local task creation, priority selection, completion, deletion and persistent storage.
- Task totals, completion metrics and module navigation.
- GitHub Actions debug APK build; artifact name: `DavionTaskCommand-APK`.

## Davion assistant commands
- "Add task buy groceries"
- "Create task phone the dentist"
- "Show my tasks"
- "Open schedule"
- "Show analytics"
- "Go home"
- "Help"

The assistant provides conversational, spoken responses for the commands implemented in this build. Its current understanding is local and intent-based; it is not yet connected to a generative AI model or online AI service. Speech recognition availability and offline operation depend on the speech service installed on the Android device.

## Build APK
Open the repository's **Actions** tab and download the `DavionTaskCommand-APK` artifact from the latest successful workflow run.

## Still planned
Full generative AI chat (online provider integration), a bundled offline language model, calendar and deadline scheduling, recurring tasks, timed notifications, subtasks, project workspaces, and backup/restore are not included in this build yet.
