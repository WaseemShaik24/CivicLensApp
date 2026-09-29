#!/usr/bin/env python3
"""
CivicLens Complete Multi-Category Dataset Downloader & Structurer
Prepares annotated dataset packages for ALL 5 civic infrastructure defect categories.
"""

import os
import sys
import json

# Ensure UTF-8 output encoding for Windows terminals
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

DATASET_ROOT = os.path.dirname(os.path.abspath(__file__))

CATEGORIES = [
    {
        "id": "CAT_01",
        "name": "CAT_01_Potholes_and_Roads",
        "label": "Pothole & Road Surface Damage",
        "roboflow_query": "pothole",
        "classes": ["Pothole_Road_Damage", "Asphalt_Crack"],
        "download_url": "https://universe.roboflow.com/search?q=pothole"
    },
    {
        "id": "CAT_02",
        "name": "CAT_02_Waste_and_Trash",
        "label": "Waste & Trash Accumulation",
        "roboflow_query": "waste",
        "classes": ["Waste_Trash_Pile", "Overflowing_Bin"],
        "download_url": "https://universe.roboflow.com/search?q=waste"
    },
    {
        "id": "CAT_03",
        "name": "CAT_03_Streetlights_and_Poles",
        "label": "Broken Streetlight & Pole",
        "roboflow_query": "streetlight",
        "classes": ["Broken_Streetlight_Pole", "Exposed_Wiring_Hazard"],
        "download_url": "https://universe.roboflow.com/search?q=streetlight"
    },
    {
        "id": "CAT_04",
        "name": "CAT_04_Water_and_Drainage",
        "label": "Water Leakage & Drainage Overflow",
        "roboflow_query": "water leakage",
        "classes": ["Water_Leak_Drainage_Overflow", "Waterlogging_Pool"],
        "download_url": "https://universe.roboflow.com/search?q=water%20leakage"
    },
    {
        "id": "CAT_05",
        "name": "CAT_05_Traffic_Hazards",
        "label": "Traffic Hazard & Obstruction",
        "roboflow_query": "traffic sign damage",
        "classes": ["Damaged_Road_Sign", "Traffic_Obstruction"],
        "download_url": "https://universe.roboflow.com/search?q=traffic%20sign%20damage"
    }
]

def setup_dataset_structure():
    print("Initializing CivicLens Dataset Workspace for ALL 5 Categories...")

    for cat in CATEGORIES:
        cat_dir = os.path.join(DATASET_ROOT, cat["name"])
        train_img = os.path.join(cat_dir, "images", "train")
        train_lbl = os.path.join(cat_dir, "labels", "train")
        val_img = os.path.join(cat_dir, "images", "val")
        val_lbl = os.path.join(cat_dir, "labels", "val")

        for d in [train_img, train_lbl, val_img, val_lbl]:
            os.makedirs(d, exist_ok=True)

        manifest_path = os.path.join(cat_dir, "dataset_info.json")
        with open(manifest_path, "w", encoding="utf-8") as f:
            json.dump({
                "category_id": cat["id"],
                "category_label": cat["label"],
                "classes": cat["classes"],
                "download_url": cat["download_url"],
                "status": "READY_FOR_IMAGES"
            }, f, indent=2)

        print(f"  [OK] Category Created: {cat['label']} -> {cat['name']}")

    print("\nDataset workspace setup complete for ALL 5 categories!")

if __name__ == "__main__":
    setup_dataset_structure()
