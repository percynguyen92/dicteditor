# DictEditor - Material 3 Android Dictionary Editor

DictEditor is a modern, premium Android dictionary editor built with Jetpack Compose, Material 3, and MVVM architecture. It is designed to manage Chinese-Vietnamese dictionary entries with advanced editing capabilities, drag-and-drop meaning reordering, and integrated AI-assisted translation.

## Features

- **Material 3 UI/UX**: Clean, responsive layout with glassmorphism design elements and top-positioned notifications.
- **Word Meanings Management**: Support for in-place editing, addition, deletion, and drag-and-drop reordering of word meanings.
- **Han-Viet Translation Lookup**: Automatic Sino-Vietnamese (Hán Việt) phonetic lookup and suggestion.
- **AI Translate Portal (ATP) Integration**: Connect with external translation services via AIDL connection.
- **Import & Export**: Easily batch import words and export your dictionary files.
- **Undo & Redo**: Full support for rolling back and redoing edits to prevent data loss.

## Tech Stack

- **Framework**: Android SDK (Kotlin), Jetpack Compose
- **Design System**: Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **Concurrency**: Kotlin Coroutines & Flow
- **IPC**: AIDL (Android Interface Definition Language)

## Getting Started

### Prerequisites

- Android Studio Koala or newer
- Android SDK 34+
- Java JDK 17

### Building and Running

1. Clone this repository.
2. Open the project folder in Android Studio.
3. Allow Gradle to sync and download the dependencies.
4. Run the application on an Android Emulator or physical device (API level 26+ recommended).

### Release and Publishing

To publish the application to Google Play Store or distribute the APK:

1. Create a release signing configuration in `app/build.gradle.kts`.
2. Generate a signed bundle/APK via **Build > Generate Signed Bundle / APK** in Android Studio.
3. Configure the proguard rules in `app/proguard-rules.pro` if code shrinking is enabled.
