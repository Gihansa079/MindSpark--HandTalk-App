# 🖐️ MindSpark – HandTalk App

**Real-Time Sinhala Sign Language Translation into Sinhala–English Text and Speech using Artificial Intelligence**

An Android application that translates Sinhala Sign Language hand gestures into Sinhala text, English text, and spoken audio in real time — helping bridge communication between the Deaf/hard-of-hearing community and Sinhala/English speakers.

> Group Project – ICT3411 / COM3405, Department of Computing, Rajarata University of Sri Lanka

---

## 📌 Overview

HandTalk App uses a smartphone camera to capture hand gestures, tracks hand landmarks in real time, and classifies both static and dynamic Sinhala Sign Language gestures using on-device machine learning models. Recognized gestures are mapped to Sinhala text, translated into English, and converted to speech — enabling smooth two-way communication.

---

## ✨ Key Features

- 🔐 **Secure User Authentication** – account creation, login, and session management
- 🎥 **Real-Time Camera Integration** – live gesture capture using Android CameraX with on-screen hand positioning guidance
- ✋ **Hand Landmark Tracking** – powered by MediaPipe, with live gesture quality validation and manual capture control
- 🧠 **AI Gesture Recognition** – TensorFlow Lite models for:
  - Static gesture recognition (CNN)
  - Dynamic gesture sequence recognition (LSTM/CNN)
- 🔤 **Gesture-to-Sinhala Text Mapping** – converts recognized gestures into Sinhala text
- 🌐 **Sinhala–English Translation** – integrated with the Google Translation API, including API retry/error handling
- 🔊 **Bilingual Text-to-Speech** – Android TextToSpeech engine configured for Sinhala and English audio output
- 🗄️ **Translation History** – encrypted local storage using PostgreSQL
- 🎧 **Audio Controls** – playback controls and audio permission handling
- ⚙️ **User Settings & Preferences** – customizable app behavior and preferences
- 🔔 **Notifications & System Handling** – in-app alerts, error feedback, and exit handling
- 🎨 **Accessibility-First UI/UX** – designed in Figma and implemented in Android XML, with clear navigation flow, help/tutorial screens, and error feedback alerts
- 🔒 **Data Protection** – secure handling of user and translation data

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Platform | Android (Java/Kotlin) |
| Camera | CameraX |
| Hand Tracking | MediaPipe |
| Machine Learning | TensorFlow Lite (CNN, LSTM/CNN) |
| Translation | Google Translation API |
| Speech | Android TextToSpeech |
| Local Storage | PostgreSQL (encrypted) |
| UI/UX Design | Figma, Android XML |

---

## 🏗️ How It Works

```
Camera Feed → MediaPipe Hand Landmark Detection → Gesture Quality Validation
      → TensorFlow Lite Model (Static/Dynamic Gesture Recognition)
      → Gesture-to-Sinhala Text Mapping
      → Google Translation API (Sinhala → English)
      → Android TextToSpeech (Bilingual Audio Output)
      → Encrypted Local History (PostgreSQL)
```

---

## 👥 Team – MindSparks

| Name | Registration No. | Responsibility Area |
|---|---|---|
| G.V. Samadhi | ASP/2022/078 | ML model training & deployment (TensorFlow Lite), preprocessing pipeline, data protection |
| H.J.G. Sandeepa | ASP/2022/138 | Real-time camera integration, MediaPipe hand tracking, live gesture validation |
| K.G.A. Perera | ASP/2022/079 | Encrypted local storage, audio permissions & playback, settings, notifications |
| H.M.S.S. Herath | ASP/2022/110 | Gesture-to-text mapping, translation API integration, text-to-speech configuration |
| P.D. Thenuwara | ASP/2022/096 | Authentication, UI/UX design (Figma/XML), navigation flow, help & error feedback |

**Main Supervisor:** Mr. E.A.C. Isuru Senaratne, Department of Computing, Rajarata University of Sri Lanka

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable version)
- Android SDK (minimum API level as configured in the project)
- PostgreSQL (for local translation history storage)
- Google Cloud Translation API key

### Installation

```bash
# Clone the repository
git clone https://github.com/Gihansa079/MindSpark--HandTalk-App.git

# Open the project in Android Studio
# Sync Gradle and install dependencies

# Add your Google Translation API key in the appropriate config file
# Build and run the app on an emulator or physical Android device
```

---

## 📄 Project Info

- **Project Title:** Real-Time Sinhala Sign Language Translation into Sinhala-English Text and Speech using Artificial Intelligence
- **Team Name:** MindSparks
- **Module:** ICT3411 / COM3405 – ICT/CS Group Project
- **University:** Rajarata University of Sri Lanka, Department of Computing

---

## 📃 License

This project was developed for academic purposes as part of the ICT3411/COM3405 Group Project at Rajarata University of Sri Lanka. Please contact the team for reuse or collaboration inquiries.
