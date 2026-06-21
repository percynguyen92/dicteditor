[Tiếng Việt](README.md)

# DictEditor

DictEditor is an Android dictionary editor app. It is designed to manage **Chinese-Vietnamese** dictionaries.

## Features

- **Find and Replace**: Support batch find and replace using Regex.
- **Import & Export**: Import and export data to a `.txt` file formatted as:
```
	愉悦度=độ vui vẻ
	愁衣食=lo cơm áo
	惹火了=chọc giận
	惹恼了=chọc giận
	惹怒了=chọc giận
	惹姐姐=Nhã tỷ tỷ
	惹哭了=chọc khóc
	惹到了=chọc phải
```
- **Undo & Redo**: Full support for Undo/Redo.

## Tech Stack

- **Framework**: Android SDK (Kotlin), Jetpack Compose
- **Design System**: Material Design 3 + Glassmorphism effect by Haze package
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow

## Getting Started

### Prerequisites

- Android Studio Koala or newer
- Android SDK 34+
- Java JDK 17
- Android Device: Android 7.0 (API level 24) or newer

### Building and Running

1. Clone this repository.
2. Open the project folder in Android Studio.
3. Allow Gradle to sync and download the dependencies.
4. Run the application on an Android Emulator or physical device (API level 26+ recommended).

### Todo:
- **AI Integration**: Use AI to suggest meanings. Currently, connection to AI services via another portal app is not supported. I will consider integrating it into this app or publishing the portal.
- **SQLite**: The app currently reads and writes directly to txt files. Planning to switch to SQLite to speed up processing of large VP and NE files.
