# SeventyFivePerc (75%) 🎓
### *Smart Academic Management & Attendance Tracker*

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**SeventyFivePerc** is an advanced academic assistant designed to help students navigate the complexities of university attendance requirements. Beyond simple tracking, it leverages machine learning to bridge the gap between physical documents (timetables/calendars) and digital management.

---

## 🌟 Key Pillars

### 🧠 Smart OCR Extraction System
The app features a sophisticated OCR engine powered by **Google ML Kit** that understands academic layouts:
- **Spatial Grid Analysis**: Automatically detects whether a timetable is oriented with days as rows or columns by analyzing coordinate variance.
- **Legend Resolution**: Automatically parses subject abbreviations (e.g., "CS301" → "Operating Systems") using a built-in legend detection algorithm.
- **Intelligent Noise Filtering**: Uses heuristic filters to strip out ERP metadata, room numbers, and faculty names to keep your schedule clean.
- **Calendar Parsing**: Supports complex date ranges and multi-month academic calendars to track holidays and exams.

### 🎙️ Offline Voice Intelligence
Experience hands-free interaction with **Vosk-based Speech Recognition**:
- **100% Offline**: All voice processing happens on-device—no internet, no data leaks.
- **Natural Commands**: Mark attendance, check upcoming classes, or query "Ghost Mode" status via voice.
- **Low Latency**: Optimized for quick interactions during class transitions.

### 📈 Attendance Analytics & Prediction
- **The "Safety" Margin**: Instantly see how many classes you can afford to miss while staying above 75%.
- **Predictive Modeling**: Input future scenarios (e.g., "What if I take next Friday off?") to visualize the impact on your eligibility.
- **Trend Visualization**: Beautiful Compose-based charts showing your attendance consistency over weeks and months.

---

## 🛠️ Technical Architecture

SeventyFivePerc follows the **MVVM (Model-View-ViewModel)** architectural pattern and utilizes modern Android libraries:

| Layer | Technology |
|---|---|
| **UI** | Jetpack Compose (Material 3), Navigation Compose |
| **Logic** | Kotlin Coroutines & Flow |
| **Database** | Room Persistence Library (Offline-first) |
| **ML/AI** | Google ML Kit (OCR), Vosk SDK (STT) |
| **Image Handling** | Coil (with GIF support for Auras) |
| **DI** | ViewModel Factory Pattern |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or higher.
- Android SDK 26+ (Android 8.0).
- At least 500MB of free space (for offline voice models).

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/kayakushwah/Attendence_Tracker.git
   ```
2. Open the project in Android Studio.
3. The **Vosk model** for English (`model-en-us`) is included in the assets folder. If it's missing, download it from [alphacephei.com/vosk/models](https://alphacephei.com/vosk/models).
4. Build and run on a physical device for the best voice recognition experience.

---

## 📂 Project Organization

```text
app/src/main/java/com/seventyfiveperc/app/
├── data/               # Room Entities, DAOs, and DataStore
├── logic/              # Attendance Calculator & Assistant Logic
├── service/            # Background services for Voice Listening
├── speech/             # Vosk & Google Speech Engines
└── ui/
    ├── ocr/            # Spatial parsing & Grid detection logic
    ├── dashboard/      # Main stats and Agenda view
    ├── timetable/      # Schedule management
    └── components/     # Reusable Compose UI elements
```

---

## 🤝 Contributing
We welcome contributions that improve OCR accuracy or add new language models! 

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.

---
**Created by [Kaya Kushwah](https://github.com/kayakushwah)**  
*Helping students stay eligible, one class at a time.*
