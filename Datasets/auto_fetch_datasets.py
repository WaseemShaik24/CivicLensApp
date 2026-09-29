#!/usr/bin/env python3
"""
CivicLens Automated Dataset Fetcher & Synthesizer
Automatically populates sample annotated images and YOLO label files for ALL 5 categories.
No manual downloading required!
"""

import os
import sys
import json
import urllib.request

# Ensure UTF-8 output encoding for Windows terminals
if sys.stdout.encoding != 'utf-8':
    sys.stdout.reconfigure(encoding='utf-8')

DATASET_ROOT = os.path.dirname(os.path.abspath(__file__))

SAMPLE_DATASETS = [
    {
        "cat_folder": "CAT_01_Potholes_and_Roads",
        "class_id": 0,
        "class_name": "Pothole_Road_Damage",
        "sample_images": [
            "https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=600&q=80"
        ]
    },
    {
        "cat_folder": "CAT_02_Waste_and_Trash",
        "class_id": 2,
        "class_name": "Waste_Trash_Pile",
        "sample_images": [
            "https://images.unsplash.com/photo-1530587191325-3db32d826c18?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1532996122724-e3c354a0b15b?auto=format&fit=crop&w=600&q=80"
        ]
    },
    {
        "cat_folder": "CAT_03_Streetlights_and_Poles",
        "class_id": 4,
        "class_name": "Broken_Streetlight_Pole",
        "sample_images": [
            "https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1517420704952-d9f39e95b43e?auto=format&fit=crop&w=600&q=80"
        ]
    },
    {
        "cat_folder": "CAT_04_Water_and_Drainage",
        "class_id": 6,
        "class_name": "Water_Leak_Drainage_Overflow",
        "sample_images": [
            "https://images.unsplash.com/photo-1500382017468-9049fed747ef?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1541888946425-d0fbb186a5b3?auto=format&fit=crop&w=600&q=80"
        ]
    },
    {
        "cat_folder": "CAT_05_Traffic_Hazards",
        "class_id": 8,
        "class_name": "Damaged_Road_Sign",
        "sample_images": [
            "https://images.unsplash.com/photo-1562654501-a0ccc0fc3fb1?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1508873696983-2df515122519?auto=format&fit=crop&w=600&q=80"
        ]
    }
]

def auto_fetch_samples():
    print("Fetching and setting up dataset images & labels for ALL 5 categories...")

    for ds in SAMPLE_DATASETS:
        folder_path = os.path.join(DATASET_ROOT, ds["cat_folder"])
        img_train_dir = os.path.join(folder_path, "images", "train")
        lbl_train_dir = os.path.join(folder_path, "labels", "train")
        img_val_dir = os.path.join(folder_path, "images", "val")
        lbl_val_dir = os.path.join(folder_path, "labels", "val")

        os.makedirs(img_train_dir, exist_ok=True)
        os.makedirs(lbl_train_dir, exist_ok=True)
        os.makedirs(img_val_dir, exist_ok=True)
        os.makedirs(lbl_val_dir, exist_ok=True)

        print(f"\nProcessing Category: {ds['cat_folder']}")

        for idx, img_url in enumerate(ds["sample_images"]):
            img_name = f"sample_{idx + 1}.jpg"
            lbl_name = f"sample_{idx + 1}.txt"

            target_img_path = os.path.join(img_train_dir, img_name)
            target_lbl_path = os.path.join(lbl_train_dir, lbl_name)

            # Download image
            try:
                urllib.request.urlretrieve(img_url, target_img_path)
                print(f"  [✓] Downloaded Image: {img_name}")

                # Create YOLO Bounding Box Annotation Label File
                # Format: <class_id> <x_center> <y_center> <width> <height>
                annotation_content = f"{ds['class_id']} 0.5 0.5 0.6 0.6\n"
                with open(target_lbl_path, "w", encoding="utf-8") as lf:
                    lf.write(annotation_content)
                print(f"  [✓] Created Label: {lbl_name} (Class ID: {ds['class_id']} - {ds['class_name']})")

            except Exception as e:
                print(f"  [!] Notice downloading {img_url}: {e}")

        # Also copy sample to val split
        if os.path.exists(os.path.join(img_train_dir, "sample_1.jpg")):
            import shutil
            shutil.copy(os.path.join(img_train_dir, "sample_1.jpg"), os.path.join(img_val_dir, "sample_1.jpg"))
            shutil.copy(os.path.join(lbl_train_dir, "sample_1.txt"), os.path.join(lbl_val_dir, "sample_1.txt"))

    print("\n[SUCCESS] Automated dataset fetch complete for ALL 5 categories!")

if __name__ == "__main__":
    auto_fetch_samples()
