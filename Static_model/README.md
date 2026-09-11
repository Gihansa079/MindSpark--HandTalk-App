# 🤟 SSL Static – Sinhala Sign Language Recognition Model

## Overview

The `ssl_static` folder contains the machine learning resources used by the **MindSpark – HandTalk App** for Sinhala Sign Language (SSL) recognition.

The model is designed to recognize predefined Sinhala sign language gestures from hand landmark features and return the corresponding Sinhala sign class.

This folder contains the trained models, label encoder, dataset, extracted landmark data, and training-related files required for the SSL recognition component.

---

## 🧠 Machine Learning Model

The main classification model used in this folder is a **Random Forest Classifier**.

The model learns patterns from hand landmark features extracted from Sinhala sign language images.

### Model Configuration

| Property          | Value                    |
| ----------------- | ------------------------ |
| Algorithm         | Random Forest Classifier |
| Number of Trees   | 300                      |
| Input Features    | 63                       |
| Number of Classes | 7                        |
| Random State      | 42                       |

The 63 input features represent numerical information obtained from detected hand landmarks.

---

## 🔄 Recognition Process

The general recognition pipeline is:

```text
Input Image / Camera Frame
          │
          ▼
     Hand Detection
          │
          ▼
   Landmark Extraction
          │
          ▼
  63 Landmark Features
          │
          ▼
 Random Forest Classifier
          │
          ▼
   Predicted Class ID
          │
          ▼
     Label Encoder
          │
          ▼
 Sinhala Sign Language Word
```

The system therefore converts a visual hand gesture into numerical landmark features and uses the trained Random Forest model to identify the corresponding sign.

---

## ✋ Supported Sign Classes

The current model supports **7 Sinhala Sign Language classes**:

| Class ID | Sign         |
| -------: | ------------ |
|        0 | ayubowan     |
|        1 | good (hodai) |
|        2 | his          |
|        3 | house        |
|        4 | i_love_you   |
|        5 | naraka       |
|        6 | oba          |

The `label_encoder.pkl` file is used to map the numerical predictions to these class names.

---

## 📂 Folder Contents

```text
ssl_static/
│
├── dataset/
│   ├── ayubowan/
│   ├── good(hodai)/
│   ├── his/
│   ├── house/
│   ├── i_love_you/
│   ├── naraka/
│   └── oba/
│
├── landmarks_dataset.csv
│
├── label_encoder.pkl
│
├── ssl_static_model.pkl
│
├── ssl_static_model.h5
│
├── ssl_static_model.tflite
│
├── training_history.png
│
└── README.md
```

---

## 📊 Dataset

The `dataset` directory contains images belonging to the seven supported sign classes.

The dataset includes approximately **786 JPG images**.

### Dataset Distribution

| Sign Class   |  Images |
| ------------ | ------: |
| ayubowan     |     124 |
| good (hodai) |     113 |
| his          |     112 |
| naraka       |     112 |
| oba          |     111 |
| house        |     111 |
| i_love_you   |     103 |
| **Total**    | **786** |

---

## 📐 Landmark Dataset

The file:

```text
landmarks_dataset.csv
```

contains the extracted hand landmark information used for machine learning.

It contains:

* **536 samples**
* **64 columns**
* **63 landmark features**
* Sign/class information

The landmark features provide a numerical representation of the hand position and configuration.

Using landmarks instead of raw image pixels makes the classification model more lightweight and suitable for application integration.

---

## 🌲 Random Forest Model

The main model is stored as:

```text
ssl_static_model.pkl
```

It is a Scikit-learn Random Forest model containing **300 decision trees**.

Random Forest was selected because it can effectively classify structured numerical features such as hand landmark coordinates.

During prediction, each decision tree provides a classification result, and the Random Forest combines the results to produce the final prediction.

---

## 🔤 Label Encoder

The file:

```text
label_encoder.pkl
```

stores the label encoding used during training.

The classifier returns a numerical class, and the label encoder converts it into the corresponding sign name.

For example:

```text
Prediction
    ↓
Class ID = 4
    ↓
Label Encoder
    ↓
i_love_you
```

---

## 📦 Available Model Formats

Three model files are included.

### 1. Pickle Model

```text
ssl_static_model.pkl
```

The main Scikit-learn model used for Random Forest prediction.

### 2. H5 Model

```text
ssl_static_model.h5
```

Model stored in HDF5 format for compatible environments.

### 3. TensorFlow Lite Model

```text
ssl_static_model.tflite
```

A TensorFlow Lite model format intended for lightweight deployment and integration into supported applications and devices.

---

## 📈 Training History

The file:

```text
training_history.png
```

contains a visual representation of the model training process.

It can be used to review the training performance of the model.

---

## 🚀 Integration with MindSpark HandTalk

The `ssl_static` model can be integrated into the main HandTalk application using the following workflow:

```text
Camera
  │
  ▼
Capture Hand Gesture
  │
  ▼
Detect Hand Landmarks
  │
  ▼
Generate 63 Features
  │
  ▼
Load ssl_static_model.pkl
  │
  ▼
Predict Sign Class
  │
  ▼
Load label_encoder.pkl
  │
  ▼
Convert Class ID → Sign
  │
  ▼
Display Recognized Sign
```

---

## 🎯 Purpose

The purpose of this model is to provide the Sinhala Sign Language recognition component of the **MindSpark – HandTalk App**.

It is intended to:

* Recognize Sinhala sign language gestures
* Convert hand gestures into class labels
* Support communication through sign language
* Provide an AI-based recognition component for the HandTalk application
* Enable future real-time sign language recognition

---

## 🔮 Future Improvements

Future improvements can include:

* Adding more Sinhala sign language classes
* Increasing the size and diversity of the dataset
* Improving recognition accuracy
* Supporting continuous sign language recognition
* Real-time camera-based recognition
* Improving hand detection under different lighting conditions
* Adding text-to-speech output
* Deploying the model on mobile devices
* Optimizing the model for real-time inference

---

## 🔗 Main Project

**MindSpark – HandTalk App**

GitHub repository:

https://github.com/Gihansa079/MindSpark--HandTalk-App

The `ssl_static` folder provides the Sinhala Sign Language recognition model and supporting resources used by the main application.
