# SSL Dynamic Sign Language Recognition

A deep learning system for recognizing **dynamic Sinhala Sign Language (SSL)** gestures from video input. The project contains **two complementary models** that classify 30-frame sequences into 7 word categories, along with a metadata-driven loading system.

---

## 📌 Overview

This repository hosts two trained models for dynamic sign recognition:

| Model | Architecture | Input | Classes | Val Accuracy |
|-------|--------------|-------|---------|--------------|
| **Model A** | CNN + BiLSTM (MobileNetV2 backbone) | `(30, 224, 224, 3)` RGB frames | 7 | **90.0%** |
| **Model B** | GCN + BiLSTM (MediaPipe landmarks) | `(30, 75, 3)` keypoints | 7 | **45.7%** |

Both models share the same 7 output classes and consume the same 30-frame temporal window, but differ in **what they see** — raw pixels vs. skeletal landmarks.

---

## 🎯 Classes

Both models predict one of the following 7 categories:

| Index | Class Name |
|-------|------------|
| 0 | `100-1 million` |
| 1 | `20-99` |
| 2 | `A-Z` |
| 3 | `Additional words` |
| 4 | `Months` |
| 5 | `SSL Sentences` |
| 6 | `Verbs` |

---

## 🏗️ Architecture

### Model A — CNN + BiLSTM (MobileNetV2)
- **Backbone**: MobileNetV2 (pretrained, frozen or fine-tuned)
- **Temporal head**: Bidirectional LSTM
- **Input**: 30 RGB frames at 224×224×3
- **Best for**: Rich visual features — hand shape, face, context
- **Val accuracy**: **90.0%**

### Model B — GCN + BiLSTM (MediaPipe Landmarks)
- **Landmark extractor**: MediaPipe Pose + Hand Landmarker
- **Graph convolution**: GCN over landmark connections
- **Temporal head**: Bidirectional LSTM
- **Input**: 30 frames × 75 keypoints × 3 coordinates (x, y, z)
- **Best for**: Lightweight, pose-invariant, privacy-friendly inference
- **Val accuracy**: **45.7%**

---

## 📁 Repository Structure

