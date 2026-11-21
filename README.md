# JARVIS TASK COMMAND

A standalone Android task manager with a futuristic JARVIS-inspired command interface.

## Current build
- Android application (Kotlin + Jetpack Compose), min Android 8.0 (API 26).
- Dark holographic reactor home screen with touch-to-open module navigation.
- Task creation, priority selection, completion, deletion and local persistence.
- English voice-command input through the Android speech-recognition service.
- Spoken status feedback through Android Text-to-Speech.
- Basic task metrics and analytics screen.
- GitHub Actions builds a debug APK and uploads it as `DavionTaskCommand-APK`.

## Voice commands
Try phrases such as:
- "Add task buy groceries"
- "Create task phone the dentist"
- "Show my tasks"
- "Open schedule"
- "Show analytics"
- "Go home"

Voice recognition availability and whether recognition works offline depend on the speech services installed and configured on the Android device. The app stores tasks locally; the current version does not include a bundled offline large-language model or cloud AI backend.

## Build
The workflow at `.github/workflows/android.yml` builds with JDK 17, Gradle 8.9 and Android Gradle Plugin 8.7.3. Open the repository's **Actions** tab and select the latest successful run to download the `DavionTaskCommand-APK` artifact.

## Planned next modules
Calendar/deadline scheduling, recurring tasks, reminders/notifications, subtasks, project workspaces, backup/restore, and optional online AI integration are not yet implemented in this first build.
