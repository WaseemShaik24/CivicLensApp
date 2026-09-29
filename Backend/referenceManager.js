/**
 * CivicLens Reference Image Manager
 * Loads and caches 5 class-specific reference images for few-shot visual classification with Gemini 2.5 Flash
 */

const fs = require('fs');
const path = require('path');

const REFERENCE_MAPPING = [
  {
    category: "Pothole & Road Surface Damage",
    filename: "pothole_reference.jpg",
    altPath: "Frontend/assets/pothole.jpg"
  },
  {
    category: "Waste & Trash Accumulation",
    filename: "waste_reference.jpg",
    altPath: "Frontend/assets/waste.jpg"
  },
  {
    category: "Broken Streetlight & Pole",
    filename: "streetlight_reference.jpg",
    altPath: "Frontend/assets/streetlight.jpg"
  },
  {
    category: "Open Manhole & Broken Drain Cover",
    filename: "manhole_reference.jpg",
    altPath: "Frontend/assets/manhole.jpg"
  },
  {
    category: "Water Pipe Leak & Flood",
    filename: "water_leak_reference.jpg",
    altPath: "Frontend/assets/water.jpg"
  }
];

let cachedReferenceParts = null;

/**
 * Loads the 5 class reference images as inlineData parts for Gemini API.
 * Fail-safe: if any reference image is missing, logs warning and proceeds without crashing.
 */
function loadReferenceImageParts() {
  if (cachedReferenceParts && cachedReferenceParts.length === 5) {
    return cachedReferenceParts;
  }

  const parts = [];
  const rootDir = path.join(__dirname, '..');

  REFERENCE_MAPPING.forEach((item, idx) => {
    let targetPath = path.join(rootDir, 'reference_images', item.filename);
    if (!fs.existsSync(targetPath)) {
      targetPath = path.join(rootDir, item.altPath);
    }

    if (fs.existsSync(targetPath)) {
      try {
        const buffer = fs.readFileSync(targetPath);
        const base64 = buffer.toString('base64');
        const ext = path.extname(targetPath).toLowerCase();
        const mimeType = ext === '.png' ? 'image/png' : 'image/jpeg';

        parts.push({
          inlineData: {
            mimeType: mimeType,
            data: base64
          }
        });
        console.log(`[CivicLens Reference Manager] Loaded IMAGE ${idx + 1} (${item.category}): ${path.basename(targetPath)}`);
      } catch (err) {
        console.warn(`[CivicLens Reference Manager] Warning: Failed reading reference image ${item.filename}:`, err.message);
      }
    } else {
      console.warn(`[CivicLens Reference Manager] Warning: Reference image missing for ${item.category} at ${targetPath}`);
    }
  });

  if (parts.length === 5) {
    cachedReferenceParts = parts;
  }

  return parts;
}

module.exports = {
  loadReferenceImageParts,
  REFERENCE_MAPPING
};
