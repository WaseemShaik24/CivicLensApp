#!/usr/bin/env python3
"""
CivicLens 1-Click Multi-Category Model Trainer (YOLOv8 / PyTorch)
Trains a single multi-class object detection & classification model covering ALL 5 civic defect categories.
"""

import os
import sys

# Ensure UTF-8 output encoding
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

def train_civiclens_model():
    print("=== CivicLens Multi-Category Model Trainer ===")
    print("Dataset Config: ./Datasets/data.yaml")
    print("Categories Covered:")
    print("  1. Potholes & Road Surface Damage")
    print("  2. Waste & Trash Accumulation")
    print("  3. Broken Streetlights & Electrical Hazards")
    print("  4. Water Leakage & Drainage Overflow")
    print("  5. Traffic Hazards & Road Sign Obstruction")

    try:
        from ultralytics import YOLO
        print("\nLoading pre-trained YOLOv8 architecture backbone (yolov8n.pt)...")
        model = YOLO('yolov8n.pt')

        print("Starting training run across all 5 categories for 50 epochs...")
        results = model.train(
            data='./Datasets/data.yaml',
            epochs=50,
            imgsz=640,
            batch=16,
            name='civiclens_hybrid_model'
        )

        print("\nExporting trained model to ONNX & TFLite formats for Backend & Android app...")
        model.export(format='onnx')
        model.export(format='tflite')

        print("\nTraining and Export Complete! Model saved to ./runs/detect/civiclens_hybrid_model/")

    except ImportError:
        print("\n[NOTE] 'ultralytics' library is not installed in local environment.")
        print("To run local model training, execute: pip install ultralytics torch torchvision")
        print("Or copy this script into Google Colab (Free GPU) to train in 10 minutes!")

if __name__ == "__main__":
    train_civiclens_model()
