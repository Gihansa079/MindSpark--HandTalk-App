# 🤟 MindSparks - Real-Time Sinhala Sign Language Translation App

An AI-powered mobile application designed to bridge the communication gap between hearing-impaired individuals and the general public in Sri Lanka. MindSparks translates **Sinhala Sign Language (SSL)** gestures into **Sinhala text, English text (via Google Translate API), and speech in real-time**.

-----------

## 🔄 App Translation Process & Workflow

MindSparks features a complete end-to-end user workflow for real-time and offline sign language translation:

1. **Real-time Gesture Recognition:**
   * Recognizes hand gestures live through the device camera.
   * Converts detected gestures directly into **Sinhala words**.
   * Translates the Sinhala words into **English** using the **Google Translate API**.

2. **Media Translation (Image & Video Upload):**
   * Allows users to upload pre-existing **Images** or **Videos**.
   * Processes uploaded media to recognize hand gestures, converts them into **Sinhala words**, and translates them into **English**.

3. **Learn Sign (Dataset Learning Hub):**
   * Displays the application's sign language dataset so users can learn and practice Sinhala Sign Language signs.

4. **Translation History:**
   * Logs and stores all past translated gestures (Sinhala & English text) for quick reference.

5. **Profile & App Settings:**
   * **Preferences:** Toggle Auto-play Voice and Haptic Feedback.
   * **Account Settings:** User profile management.
   * **Language & Voice Gender Selection:** Customize translation target language and TTS Voice (Male/Female).
   * **Theme Customization:** Switch between Light and Dark mode options.
   * **Help & Support / About Us / Logout:** Access app support details and secure logout.


-----------
## 🚀 Key Features

* **Real-Time Live Translation:** Live recognition of hand gestures using the camera with instant Sinhala-to-English translation.
* **Image & Video Translation:** Upload saved gallery media to translate signs into Sinhala and English text.
* **Learn Sign (Dataset Hub):** Educational module showing the sign language dataset.
* **Translation History:** Save, review, and search past translation records.
* **Customizable Preferences:** Auto-play voice output, Haptic vibration feedback, Voice Gender selection (Male/Female), Light/Dark themes.
* **Permissions & Security:** Audio/Voice and Camera permissions, secure user registration/login, and password lockout protection.


-----------
## 🧠 AI Model Architecture

MindSparks utilizes a dual AI engine setup optimized for mobile deployment using TensorFlow Lite:

| Gesture Type | Deep Learning Model | Model Size | Accuracy | Latency |
| :--- | :--- | :--- | :--- | :--- |
| **Static Gestures** | VGG19 (CNN Architecture) | `38.5 MB` | **98.14%** | < 100 ms |
| **Dynamic Gestures**| CNN + LSTM Architecture | `5.65 MB` | **85.71%** | < 150 ms |


-----------
## 🛠️ Technology Stack & Permissions

* **Mobile App:** Android (Kotlin / Java)
* **Computer Vision & Preprocessing:** MediaPipe / OpenCV
* **Machine Learning Engine:** TensorFlow Lite (VGG19, LSTM)
* **Translation API:** Google Translate API
* **Security & Database:** Password Hashing (bcrypt), Data Encryption (AES-256)
* **App Permissions:** Camera Access, Audio / Microphone Permission, Storage Access


-----------
## 📱 System Requirements

* **OS Version:** Android 10 (API Level 29) or higher
* **Hardware Requirements:**
  * Minimum 5MP Rear/Front Camera
  * Microphone for audio/voice features
  * Active Internet Connection


-----------
## 👥 Team MindSparks

**Supervised by:** Mr. E.A.C. Isuru Senaratne

| Name | Registration No | Index No |
| :--- | :-------------- | :------- |
| G.V. Samadhi | ASP/2022/078 | 5870 |
| H.J.G. Sandeepa | ASP/2022/138 | 5920 |
| K.G.A. Perera | ASP/2022/079 | 5871 |
| H.M.S.S Herath | ASP/2022/110 | 5881 |
| P.D. Thenuwara | ASP/2022/096 | 5898 |


-----------
## 📄 License

This project was developed as an academic project by Team MindSparks.
