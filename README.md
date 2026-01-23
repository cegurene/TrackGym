# Gimnasio - Gym Workout Tracker App

An Android application for tracking gym workouts, routines, and exercises built with Kotlin and Jetpack Compose.

## 📱 About

Gimnasio is a modern Android app designed to help users organize and track their gym workout routines. The app allows users to create custom routines, add exercises, and manage workout sets efficiently.

## ✨ Features

- **Routine Management**: Create and organize workout routines
- **Exercise Tracking**: Add exercises to routines and track sets
- **Modern UI**: Built with Jetpack Compose for a smooth user experience
- **Data Persistence**: Uses Room database for local data storage
- **Material Design 3**: Follows latest Material Design guidelines

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (SQLite)
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Build System**: Gradle with Kotlin DSL

### Key Dependencies

- AndroidX Core KTX
- AndroidX Lifecycle Runtime
- Jetpack Compose (Material 3)
- Room Database 2.6.1

## 📂 Project Structure

```
app/src/main/java/com/example/gimnasio/
├── data/
│   ├── entity/         # Room database entities
│   │   ├── RutinaEntity.kt
│   │   ├── EjercicioEntity.kt
│   │   ├── SerieEntity.kt
│   │   └── EntrenamientoEntity.kt
│   └── model/          # Data models
│       ├── Rutina.kt
│       ├── Ejercicio.kt
│       └── Serie.kt
├── ui/
│   ├── rutinas/        # Routines screen UI
│   │   ├── RutinasScreen.kt
│   │   └── NuevaRutinaDialog.kt
│   ├── theme/          # App theming
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   └── GymApp.kt       # Main app composable
└── MainActivity.kt     # Main activity
```

## 🚀 Getting Started

### Prerequisites

- Android Studio (latest version recommended)
- JDK 11 or higher
- Android SDK with API level 24 or higher

### Installation

1. Clone the repository:
```bash
git clone https://github.com/cegurene/AppGimnasio.git
cd AppGimnasio
```

2. Open the project in Android Studio:
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository and select it

3. Sync Gradle:
   - Android Studio should automatically sync Gradle
   - If not, click "Sync Project with Gradle Files" in the toolbar

### Building the App

#### Using Android Studio

1. Select your target device or emulator
2. Click the "Run" button (green play icon) or press `Shift + F10`

#### Using Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## 📄 License

This project is open source and available for educational purposes.

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

## 📧 Contact

For questions or feedback about this project, please open an issue on GitHub.
