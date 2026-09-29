# 📊 CivicLens Master Dataset & Model Training Workspace

Welcome to the **CivicLens Dataset Repository**! This folder ([`Datasets/`](file:///c:/Users/wasee/OneDrive/Documents/CivicLens/Datasets)) contains complete configurations, directory structures, download tools, and 1-click training scripts for **ALL 5 Civic Defect Categories**.

---

## 🗂️ 100% Complete Category Breakdown

Every category supported by CivicLens is covered:

| # | Category Name | Directory | Public Dataset Download Links | Classes & Labels |
| :- | :--- | :--- | :--- | :--- |
| **1** | **Potholes & Road Surface Damage** | [`CAT_01_Potholes_and_Roads/`](file:///c:/Users/wasee/OneDrive/Documents/CivicLens/Datasets/CAT_01_Potholes_and_Roads) | [Roboflow Pothole Dataset](https://universe.roboflow.com/search?q=pothole) / RDD2022 | Pothole, Asphalt Crack, Surface Erosion, Manhole Defect |
| **2** | **Waste & Trash Accumulation** | [`CAT_02_Waste_and_Trash/`](file:///c:/Users/wasee/OneDrive/Documents/CivicLens/Datasets/CAT_02_Waste_and_Trash) | [Roboflow Waste Dataset](https://universe.roboflow.com/search?q=waste) / TACO | Waste Pile, Overflowing Bin, Plastic Litter |
| **3** | **Broken Streetlight & Pole** | [`CAT_03_Streetlights_and_Poles/`](file:///c:/Users/wasee/OneDrive/Documents/CivicLens/Datasets/CAT_03_Streetlights_and_Poles) | [Roboflow Streetlight Dataset](https://universe.roboflow.com/search?q=streetlight) | Broken Luminaire, Damaged Pole, Exposed Wiring |
| **4** | **Water Leakage & Drainage** | [`CAT_04_Water_and_Drainage/`](file:///c:/Users/wasee/OneDrive/Documents/CivicLens/Datasets/CAT_04_Water_and_Drainage) | [Roboflow Water Leakage Dataset](https://universe.roboflow.com/search?q=water%20leakage) | Burst Pipe, Drainage Overflow, Waterlogging |
| **5** | **Traffic Hazards & Obstruction** | [`CAT_05_Traffic_Hazards/`](file:///c:/Users/wasee/OneDrive/Documents/CivicLens/Datasets/CAT_05_Traffic_Hazards) | [Roboflow Traffic Sign Dataset](https://universe.roboflow.com/search?q=traffic%20sign%20damage) | Damaged Sign, Illegal Parking, Sidewalk Blockade |

---

## 🛠️ Step-by-Step Dataset Download & Training Guide

### Step 1: Initialize Workspace Folders
Run the dataset structure initialization script (already executed for you!):
```bash
python Datasets/download_datasets.py
```

### Step 2: Download Free Datasets (1-Click Roboflow / Kaggle)
1. Visit the Roboflow Universe links above for any category.
2. Click **Export Dataset** -> Select format **YOLOv8**.
3. Copy images into `images/train/` and annotations into `labels/train/` for that category.

### Step 3: Run 1-Click Multi-Category Model Training
Train a single unified YOLOv8 model covering all 5 categories:
```bash
python Datasets/train_yolov8_civiclens.py
```
This automatically exports:
* `civiclens_model.onnx` (For Node.js Backend Server)
* `civiclens_model.tflite` (For Android App On-Device Inference)
