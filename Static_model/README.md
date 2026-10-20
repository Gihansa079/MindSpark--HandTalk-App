🤟 SSL Static – Sinhala Sign Language Recognition Model

Overview
The ssl_static component contains the machine-learning pipeline for the MindSpark – HandTalk App, designed to recognize predefined Sinhala Sign Language (SSL) gestures from hand landmarks.

The training script, static_model_new_dataset.py, uses MediaPipe Hand Landmarker to extract hand landmarks from dataset images and trains a TensorFlow/Keras feed-forward neural network to classify the resulting numerical features. The trained network is exported to TensorFlow Lite (TFLite) for lightweight deployment.
Important: The classes and their names are read from the dataset folders at runtime. The exact number of classes and their labels therefore depend on the dataset ZIP used during training; they are not hard-coded in the script.

🧠 Model Architecture
The model is a fully connected neural network built with TensorFlow/Keras.
Property	Value
Model type	Dense neural network (feed-forward classifier)
Input features	63
Hidden layer 1	128 neurons, ReLU activation
Dropout	0.30
Hidden layer 2	64 neurons, ReLU activation
Output layer	One neuron per detected sign class
Output activation	Softmax
Optimizer	Adam
Learning rate	0.001
Loss function	Sparse categorical cross-entropy
Batch size	32
Maximum epochs	100
Early stopping	Monitors training loss; patience 10; restores best weights
Random seed	42


The number of output classes is determined automatically from the class folders found in the dataset.

🔄 Recognition Pipeline
Dataset Image / Camera Capture
             │
             ▼
       Image Conversion
       BGR → RGB
             │
             ▼
   MediaPipe Hand Landmarker
             │
             ▼
   21 Hand Landmarks Detected
             │
             ▼
  63 Features: x, y, z per landmark
             │
             ▼
   TensorFlow/Keras Neural Network
             │
             ▼
     Softmax Class Probabilities
             │
             ▼
      Highest-Scoring Class
             │
             ▼
       Predicted Sign + Confidence
The script uses the first detected hand and takes the three coordinates (x, y, and z) for each of the 21 landmarks. These are the raw MediaPipe normalized coordinates; no additional feature normalization is applied.
✋ Supported Sign Classes
Class names are discovered from the subdirectories inside the dataset ZIP. Each valid class directory should contain supported image files. The script does not define a fixed list of Sinhala sign labels.

The class names are encoded using Scikit-learn's LabelEncoder. The exported labels.txt and label_mapping.json files preserve the label order used by the model.

To see the classes used for a particular training run, check the training output and the generated label files.
📂 Dataset
The script expects a ZIP archive containing class folders and images. It supports either of these layouts:
dataset_2.zip
└── dataset/
    ├── sign_class_1/
    │   ├── image_001.jpg
    │   └── ...
    ├── sign_class_2/
    │   ├── image_001.jpg
    │   └── ...
    └── ...
or:
dataset_2.zip
└── extra_folder/
    ├── sign_class_1/
    ├── sign_class_2/
    └── ...
Supported image extensions are .jpg, .jpeg, .png, .bmp, and .webp.

The script prints the number of images found in each class directory. During landmark extraction, images that cannot be read, fail detection, or contain no detected hand are skipped. Only successfully extracted landmark samples are written to the CSV file.
Dataset paths used by the script
The notebook is configured for Google Colab and Google Drive:
- Input ZIP: MyDrive/dataset_new/dataset_2.zip
- Output directory: MyDrive/ssl_static_new_1
- Temporary extraction directory: /content/ssl_dataset
Update these paths in the script to match your own Google Drive setup.
📐 Landmark Dataset
The generated file is:
landmarks_dataset.csv
Each row contains 64 columns:
- Columns 1–63: the x, y, and z coordinates for 21 hand landmarks.
- Column 64: the sign class label.
The script checks that the CSV has 63 feature columns plus one label column and that the feature values are finite. It also checks that at least two classes are present and that each class has at least 12 successfully extracted samples.

The number of CSV rows depends on the dataset and on how many images yield valid hand landmarks; it should not be assumed to be the same as the total number of images in the archive.
🖐️ Hand Landmark Extraction
The script downloads the official MediaPipe Hand Landmarker task model when it is not already present:
hand_landmarker.task
MediaPipe is configured for image input, with up to two hands allowed by the detector. The feature extraction code then uses the first detected hand to produce a 63-value feature vector.

If no hand is detected in an image, that image is skipped during dataset creation. For an uploaded or captured test image, the prediction function returns No hand detected with zero confidence when no landmarks are found.
🏋️ Training and Evaluation
The pipeline performs these steps:
1. Extracts the dataset ZIP.
2. Finds the class directories and reports image counts.
3. Detects hands in the images and extracts 63 landmark features per successful image.
4. Saves the features and labels to landmarks_dataset.csv.
5. Encodes the class labels with LabelEncoder.
6. Splits the extracted samples into 80% training and 20% testing, using stratification and random seed 42.
7. Trains the neural network for up to 100 epochs, with batch size 32 and early stopping.
8. Evaluates predictions on the held-out test split.
9. Prints test accuracy and a per-class classification report.
10. Saves a confusion matrix and a training-history plot.
11. Converts the trained Keras model to TensorFlow Lite and checks the exported model's input and output shapes.
The script uses X_test to report evaluation metrics. The reported accuracy depends on the dataset and training run; no fixed accuracy value is assumed in this README.
Evaluation outputs
- confusion_matrix.png — compares actual labels with predicted labels on the test split.
- training_history.png — plots training accuracy and loss, along with test metrics supplied by the script's test-metrics callback.
Code note: The provided Python file references TestMetricsCallback(X_test, y_test), but does not define that class in the visible script. Ensure the callback class is defined or imported before running the training section. The plotting section also expects the callback to expose test_accuracy and test_loss histories.
🔤 Labels and Class Mapping
The script generates two label files:
labels.txt
Contains one class label per line, in the same order as the model's output indices.
label_mapping.json
Maps each output index (as a string) to its class name. For example, the structure is:
{
  "0": "class_name_1",
  "1": "class_name_2"
}
The example above is illustrative only. Actual entries are generated from the dataset labels.

When the model returns a probability vector, the predicted class is the index with the highest probability. The corresponding label is retrieved using the same label order that was used during training.
📦 Exported Model and Deployment Files
The script creates the following files in ssl_static_new_1:
File	Purpose
ssl_static_model.tflite	TensorFlow Lite version of the trained neural network
hand_landmarker.task	MediaPipe model used to detect hand landmarks
labels.txt	Output labels in model-index order
label_mapping.json	Mapping from output indices to labels
model_metadata.json	Model type, input shape and dtype, feature order, labels, test accuracy, and sample counts
landmarks_dataset.csv	Extracted landmark features and class labels
confusion_matrix.png	Test-set confusion matrix
training_history.png	Accuracy and loss plots


The script copies the deployment files into /content/mindspark_deploy and packages them into /content/mindspark_deploy.zip for download from Google Colab.
TensorFlow Lite input and output
The script checks for the following input shape:
[1, 63]
The input data type is float32. The output shape is:
[1, number_of_classes]
The output values are class probabilities from the softmax layer. The label files must be kept in sync with the exact model that is deployed.
Format clarification: This script exports a .tflite model. It does not save ssl_static_model.pkl, ssl_static_model.h5, or label_encoder.pkl. Do not list those files as outputs of this script unless they are created by another step.

📷 Image and Webcam Prediction
The notebook includes testing workflows for:
- Uploading a test image in Google Colab.
- Capturing an image using the browser webcam.
- Extracting hand landmarks from the image.
- Running inference with the trained model.
- Printing the predicted sign and confidence.
- Displaying the captured image with the prediction and confidence.
- Printing the top five class predictions when using predict_sign().
The predict_sign(image_path) function returns the predicted label and confidence. If a hand is not detected, it returns ("No hand detected", 0.0).

The webcam workflow uses Google Colab browser permissions and is not, by itself, a standalone mobile or desktop camera application.
🚀 Integration with MindSpark – HandTalk
For deployment using the exported TensorFlow Lite model, the application should follow the same preprocessing used during training:
Camera / Image
      │
      ▼
Convert Image to RGB
      │
      ▼
MediaPipe Hand Landmarker
      │
      ▼
Extract 21 Landmarks (x, y, z)
      │
      ▼
Create float32 Input [1, 63]
      │
      ▼
Run ssl_static_model.tflite
      │
      ▼
Find Highest Probability Index
      │
      ▼
Look Up Index in labels.txt
      │
      ▼
Display Predicted Sign and Confidence
Use the same landmark order and label mapping as the training pipeline. If no hand is detected, handle that case separately rather than passing an invalid feature vector to the classifier.
🛠️ Main Libraries
The notebook uses the following Python libraries and tools:
- MediaPipe — hand landmark detection.
- TensorFlow / Keras — neural network training and TFLite conversion.
- Scikit-learn — label encoding, stratified train/test split, and evaluation metrics.
- NumPy — numerical feature processing.
- Pandas — loading the landmark CSV.
- OpenCV — reading images and converting BGR images to RGB.
- Matplotlib — plotting evaluation results.
- Google Colab utilities — Google Drive mounting, browser webcam capture, file upload, and download.
A compatible environment needs these dependencies installed. The notebook uses Google Colab-specific imports such as google.colab.drive and google.colab.files; those cells will need adaptation for execution outside Colab.
🎯 Purpose
This model provides the static Sinhala Sign Language gesture-recognition component for the MindSpark – HandTalk App. It converts a single image or captured hand gesture into a predicted class label using MediaPipe landmarks and a trained neural network.

It is intended to support sign recognition and can serve as a component in a broader communication application. Recognition quality depends on the dataset, landmark detection, image conditions, and the trained model.
🔮 Future Improvements
Possible improvements include:
- Expanding and balancing the sign dataset.
- Collecting images with different users, hand sizes, backgrounds, and lighting conditions.
- Reviewing per-class precision, recall, and confusion-matrix results.
- Testing on a separate dataset collected independently from the training images.
- Adding real-time video recognition and temporal modelling for continuous signs.
- Improving confidence handling and adding an uncertain-prediction threshold.
- Integrating the TFLite model and MediaPipe preprocessing into the target mobile application.
- Adding text-to-speech output where appropriate.
🔗 Main Project
MindSpark – HandTalk App

GitHub repository:
https://github.com/Gihansa079/MindSpark--HandTalk-App

This folder's training pipeline produces the landmark dataset, trained TensorFlow Lite classifier, labels, metadata, and evaluation visuals used to support the Sinhala Sign Language recognition component.
