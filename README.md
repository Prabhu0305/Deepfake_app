# 🔍 Deepfake Detection App

An Android application built with **Kotlin** that detects deepfake images and videos using on-device machine learning.

---

## 📋 Table of Contents

- [About](#about)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Usage](#usage)
- [License](#license)

---

## About

The **Deepfake Detection App** is a native Android application designed to help users identify AI-generated or manipulated media. It leverages machine learning models to analyze images or video frames and classify them as real or deepfake, running inference directly on the device for privacy and speed.

---

## ✨ Features

- 📸 Analyze images from the gallery or camera
- 🤖 On-device ML inference (no internet required)
- ✅ Real vs. Deepfake classification with confidence score
- 🔒 Privacy-first — media never leaves the device
- 📱 Clean, intuitive Android UI

---

## 🛠 Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Android SDK | Mobile application framework |
| Gradle (KTS) | Build system |
| ML Model (TFLite / custom) | Deepfake detection inference |

---

## ✅ Prerequisites

- **Android Studio** Hedgehog or later
- **JDK 17+**
- Android device or emulator running **API 26+** (Android 8.0 Oreo)
- Git

---

## 🚀 Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/Prabhu0305/Deepfake_app.git
cd Deepfake_app
```

### 2. Open in Android Studio

- Launch Android Studio
- Select **File → Open** and navigate to the cloned directory
- Wait for Gradle sync to complete

### 3. Build & Run

- Connect an Android device or start an emulator
- Click **Run ▶** or press `Shift + F10`

---

## 📁 Project Structure

```
Deepfake_app/
├── app/                    # Main application module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/       # Kotlin source files
│   │   │   ├── res/        # Layouts, drawables, strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/           # Unit tests
├── gradle/                 # Gradle wrapper files
├── build.gradle.kts        # Project-level build config
├── settings.gradle.kts     # Project settings
└── README.md
```

---

## 📱 Usage

1. Launch the app on your Android device.
2. Tap **Select Image** to pick a photo from your gallery, or use the **Camera** option to capture one.
3. The app analyzes the image and displays a result:
   - ✅ **Real** — the image appears authentic
   - ⚠️ **Deepfake** — the image shows signs of AI manipulation
4. A confidence percentage is shown alongside the classification.

---

## 📄 License

This project is licensed under the **Apache License 2.0**. See the [LICENSE](LICENSE) file for details.

---

## 🙋 Author

**Prabhu0305 (Ashirvadhan)**
[GitHub Profile](https://github.com/Prabhu0305)
**Uchihakamal1816 (Sai Kamal)**
[GitHub Profile](https://github.com/Uchihakamal1816)
