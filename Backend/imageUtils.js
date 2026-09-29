const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

/**
 * Image Validation & Conversion Helper
 */
function validateAndFetchImageBase64(urlOrBase64) {
  return new Promise((resolve, reject) => {
    if (!urlOrBase64 || typeof urlOrBase64 !== 'string') {
      return reject(new Error("Invalid image payload: Payload is empty or not a string"));
    }

    let str = urlOrBase64.trim();

    // Check if local file asset path or direct file path
    if (!str.startsWith('http') && (fs.existsSync(str) || str.includes('assets') || str.includes('reference_images') || str.endsWith('.jpg') || str.endsWith('.png') || str.endsWith('.jpeg'))) {
      let candidatePath = str;
      if (!fs.existsSync(candidatePath)) {
        const normalized = str.replace(/\\/g, '/');
        const cleanPath = normalized.replace(/^.*?assets\//, 'assets/');
        candidatePath = path.join(__dirname, '../Frontend', cleanPath);
      }
      if (fs.existsSync(candidatePath) && fs.statSync(candidatePath).isFile()) {
        const buffer = fs.readFileSync(candidatePath);
        const ext = path.extname(candidatePath).toLowerCase();
        const mime = ext === '.png' ? 'image/png' : 'image/jpeg';
        return resolve({ base64: buffer.toString('base64'), mimeType: mime, bytes: buffer.length });
      }
    }

    if (str.startsWith('http://') || str.startsWith('https://')) {
      const httpModule = str.startsWith('https://') ? https : http;
      httpModule.get(str, (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          return validateAndFetchImageBase64(res.headers.location).then(resolve).catch(reject);
        }
        const contentType = res.headers['content-type'] || 'image/jpeg';
        const chunks = [];
        res.on('data', chunk => chunks.push(chunk));
        res.on('end', () => {
          const buffer = Buffer.concat(chunks);
          if (buffer.length < 500) {
            return reject(new Error("Image payload too small or corrupted"));
          }
          resolve({
            base64: buffer.toString('base64'),
            mimeType: contentType.split(';')[0],
            bytes: buffer.length
          });
        });
      }).on('error', (err) => reject(new Error("Network error downloading image: " + err.message)));
    } else {
      let clean = str;
      let mimeType = 'image/jpeg';
      if (clean.startsWith('data:image/png')) {
        mimeType = 'image/png';
        clean = clean.split(',')[1];
      } else if (clean.startsWith('data:image/webp')) {
        mimeType = 'image/webp';
        clean = clean.split(',')[1];
      } else if (clean.includes(',')) {
        clean = clean.split(',')[1];
      }

      if (!clean || clean.length < 100) {
        return reject(new Error("Image Base64 string is too short or invalid"));
      }

      const byteLength = Math.round(clean.length * 0.75);
      resolve({ base64: clean, mimeType: mimeType, bytes: byteLength });
    }
  });
}

module.exports = {
  validateAndFetchImageBase64
};
