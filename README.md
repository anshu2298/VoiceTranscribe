# VoiceTranscribe

A native Android app for voice recording and AI-powered transcription using [Gladia](https://gladia.io). All recordings and transcripts are stored locally on your device.

## Download

[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0.0-teal?style=for-the-badge&logo=android)](https://github.com/anshu2298/VoiceTranscribe/releases/latest/download/VoiceTranscribe-v1.0.0.apk)

> **Requires Android 8.0+**
>
> On install, Android will ask to "Allow installs from unknown sources" — tap Allow.

## Features

- **Real-time transcription** — words appear on screen as you speak via Gladia's WebSocket API
- **High-quality final transcript** — after recording, audio is sent to Gladia's batch API for a polished result
- **Local storage** — all recordings and transcripts saved on-device, no cloud database
- **Searchable history** — full-text search across all your transcripts
- **Audio playback** — replay any recording with a scrubber
- **Export & share** — export transcript as `.txt` or share via any app

## Setup

1. Install the APK (download above)
2. Get a free API key from [app.gladia.io](https://app.gladia.io)
3. Open the app → tap **Settings** (top-right) → paste your API key → Save
4. Tap the **mic button** to start recording

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- Room (SQLite + FTS4 search)
- OkHttp WebSocket (live transcription)
- Retrofit + Moshi (batch transcription)
- WorkManager (background polling)
- ExoPlayer (audio playback)
- Hilt (dependency injection)

## Build from Source

Requirements: Android Studio + JDK 17+

```bash
git clone https://github.com/anshu2298/VoiceTranscribe.git
cd VoiceTranscribe
# Open in Android Studio and run, or:
./gradlew assembleDebug
```
