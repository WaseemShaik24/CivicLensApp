const fs = require('fs');
const path = require('path');
const https = require('https');
const crypto = require('crypto');
const { validateAndFetchImageBase64 } = require('./imageUtils');

// Load environment variables
try {
  require('dotenv').config({ path: path.join(__dirname, '../.env') });
} catch {
  const envPath = path.join(__dirname, '../.env');
  if (fs.existsSync(envPath)) {
    const lines = fs.readFileSync(envPath, 'utf8').split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed && !trimmed.startsWith('#') && trimmed.includes('=')) {
        const eqIdx = trimmed.indexOf('=');
        const k = trimmed.substring(0, eqIdx).trim();
        const v = trimmed.substring(eqIdx + 1).trim();
        if (k && !process.env[k]) process.env[k] = v;
      }
    }
  }
}

// Local cache storage
const DB_DIR = path.join(__dirname, '../Database');
const CACHE_FILE = path.join(DB_DIR, 'ai_cache.json');
let aiCache = {};

function loadCache() {
  try {
    if (fs.existsSync(CACHE_FILE)) {
      aiCache = JSON.parse(fs.readFileSync(CACHE_FILE, 'utf8'));
    }
  } catch (e) {
    aiCache = {};
  }
}
loadCache();

function getCachedResult(hash) {
  return aiCache[hash] || null;
}

function setCachedResult(hash, data) {
  aiCache[hash] = data;
  try {
    if (!fs.existsSync(DB_DIR)) fs.mkdirSync(DB_DIR, { recursive: true });
    fs.writeFileSync(CACHE_FILE, JSON.stringify(aiCache, null, 2));
  } catch (e) {
    console.warn('[AI Engine] Failed writing cache:', e.message);
  }
}

const ALLOWED_CATEGORIES = [
  "Open Manholes",
  "Potholes",
  "Waste",
  "Streetlights",
  "Water Leaks",
  "needs_review"
];

function normalizeCategory(raw) {
  if (!raw || typeof raw !== 'string') return "needs_review";
  const str = raw.trim().toLowerCase();
  if (str.includes('pothole') || str.includes('road surface') || str.includes('asphalt') || str.includes('road damage')) {
    return "Potholes";
  }
  if (str.includes('waste') || str.includes('trash') || str.includes('garbage') || str.includes('dump') || str.includes('refuse')) {
    return "Waste";
  }
  if (str.includes('streetlight') || str.includes('street light') || str.includes('lamp') || str.includes('pole') || str.includes('wire')) {
    return "Streetlights";
  }
  if (str.includes('manhole') || str.includes('drain cover') || str.includes('drainage opening')) {
    return "Open Manholes";
  }
  if (str.includes('water leak') || str.includes('water pipe') || str.includes('flood') || str.includes('burst pipe') || str.includes('pipe leak')) {
    return "Water Leaks";
  }
  if (str.includes('review') || str.includes('unknown') || str.includes('unverified')) {
    return "needs_review";
  }
  const exact = ALLOWED_CATEGORIES.find(c => c.toLowerCase() === str);
  return exact || "needs_review";
}

function normalizeSeverity(raw) {
  if (!raw || typeof raw !== 'string') return "MEDIUM";
  const s = raw.trim().toUpperCase();
  if (s === 'CRITICAL' || s === 'HIGH') return "HIGH";
  if (s === 'LOW') return "LOW";
  return "MEDIUM";
}

function stripReasoningAndExtractJson(text) {
  if (!text || typeof text !== 'string') return null;

  // Strip <think>, <thought>, <reasoning> tags
  let cleaned = text
    .replace(/<think>[\s\S]*?(?:<\/think>|$)/gi, '')
    .replace(/<thought>[\s\S]*?(?:<\/thought>|$)/gi, '')
    .replace(/<reasoning>[\s\S]*?(?:<\/reasoning>|$)/gi, '')
    .replace(/^(?:Thinking Process|Reasoning):[\s\S]*?\n\n/i, '')
    .trim();

  // Strip markdown code block
  const mdMatch = cleaned.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (mdMatch && mdMatch[1]) {
    cleaned = mdMatch[1].trim();
  }

  // Parse JSON
  try {
    return JSON.parse(cleaned);
  } catch {
    const firstBrace = cleaned.indexOf('{');
    const lastBrace = cleaned.lastIndexOf('}');
    if (firstBrace !== -1 && lastBrace > firstBrace) {
      try {
        return JSON.parse(cleaned.substring(firstBrace, lastBrace + 1));
      } catch {}
    }
  }

  // Regex fallback
  const catMatch = cleaned.match(/"category"\s*:\s*"([^"]+)"/i);
  const sevMatch = cleaned.match(/"severity"\s*:\s*"([^"]+)"/i);
  const titleMatch = cleaned.match(/"title"\s*:\s*"([^"]+)"/i);
  const descMatch = cleaned.match(/"description"\s*:\s*"([^"]+)"/i);
  const confMatch = cleaned.match(/"confidence"\s*:\s*([0-9.]+)/i);

  if (catMatch) {
    return {
      category: catMatch[1],
      severity: sevMatch ? sevMatch[1] : "MEDIUM",
      title: titleMatch ? titleMatch[1] : "Civic Defect Identified",
      description: descMatch ? descMatch[1] : "Visual inspection completed.",
      confidence: confMatch ? parseFloat(confMatch[1]) : 0.8
    };
  }

  return null;
}

function parseRetryDelay(headers, bodyText) {
  if (headers && headers['retry-after']) {
    const v = parseFloat(headers['retry-after']);
    if (!isNaN(v) && v > 0) return Math.ceil(v);
  }
  if (bodyText) {
    const sMatch = bodyText.match(/try again in ([0-9.]+)\s*s/i) || bodyText.match(/in ([0-9.]+)s/i);
    if (sMatch && sMatch[1]) return Math.ceil(parseFloat(sMatch[1]));
    const msMatch = bodyText.match(/([0-9]+)m([0-9.]+)s/i);
    if (msMatch) return parseInt(msMatch[1]) * 60 + Math.ceil(parseFloat(msMatch[2]));
  }
  return 5;
}

const SYSTEM_PROMPT = `You are an expert civic infrastructure defect classifier for CivicLens.
Analyze the provided image and classify the civic infrastructure issue into exactly ONE of the following categories:
- Open Manholes
- Potholes
- Waste
- Streetlights
- Water Leaks
- needs_review

Determine:
1. category: Exactly one of the six categories above. If the image is unclear, ambiguous, or does not clearly show civic infrastructure defect, choose "needs_review".
2. severity: Exactly "LOW", "MEDIUM", or "HIGH".
3. title: A concise, descriptive headline of the issue (e.g. "Severe Pothole on Roadway").
4. description: A clear municipal audit description of the defect and recommended repair action.
5. confidence: A float between 0.0 and 1.0 representing classification certainty.

Return ONLY a valid JSON object in this exact format:
{
  "category": "Potholes",
  "severity": "HIGH",
  "title": "Severe Pothole on Roadway",
  "description": "Deep asphalt depression endangering vehicles and pedestrians.",
  "confidence": 0.95
}`;

/**
 * Gemini Provider Call
 */
async function callGeminiProvider(targetImageObj, retryCount = 0) {
  const apiKey = (process.env.GEMINI_API_KEY || '').trim();
  if (!apiKey || apiKey.length < 5 || apiKey.includes('YOUR_')) {
    return { skipped: true, reason: 'GEMINI_API_KEY not configured' };
  }

  const rawModel = (process.env.GEMINI_MODEL || 'gemini-2.5-flash').trim();
  const model = rawModel.replace(/^models\//, '');
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(apiKey)}`;

  const payload = JSON.stringify({
    contents: [
      {
        role: "user",
        parts: [
          {
            inlineData: {
              mimeType: targetImageObj.mimeType,
              data: targetImageObj.base64
            }
          },
          {
            text: SYSTEM_PROMPT
          }
        ]
      }
    ],
    generationConfig: {
      responseMimeType: "application/json",
      temperature: 0.1,
      maxOutputTokens: 1000
    }
  });

  return new Promise((resolve) => {
    try {
      const parsedUrl = new URL(url);
      const req = https.request({
        hostname: parsedUrl.hostname,
        path: parsedUrl.pathname + parsedUrl.search,
        method: 'POST',
        timeout: 25000,
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload)
        }
      }, (res) => {
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', async () => {
          const status = res.statusCode;

          if (status === 429) {
            const isDaily = /daily|quota|tpd|rpd/i.test(body);
            const delay = parseRetryDelay(res.headers, body);
            if (!process.env.DISABLE_AI_RETRY && !isDaily && delay <= 20 && retryCount < 2) {
              await new Promise(r => setTimeout(r, (delay + 1) * 1000));
              return callGeminiProvider(targetImageObj, retryCount + 1).then(resolve);
            }
            return resolve({
              success: false,
              statusCode: 429,
              isRateLimit: true,
              isDaily: isDaily,
              retryAfter: delay,
              error: isDaily ? 'Daily quota exceeded' : 'Rate limit reached'
            });
          }

          if (status === 404) {
            return resolve({
              success: false,
              statusCode: 404,
              isModelNotFound: true,
              error: `Gemini model '${model}' not found`
            });
          }

          if (status >= 500) {
            return resolve({
              success: false,
              statusCode: status,
              isServerError: true,
              error: `Gemini server error (${status})`
            });
          }

          if (status !== 200) {
            return resolve({
              success: false,
              statusCode: status,
              error: `Gemini API error (${status}): ${body.substring(0, 150)}`
            });
          }

          try {
            const data = JSON.parse(body);
            const rawText = data?.candidates?.[0]?.content?.parts?.[0]?.text;
            const parsedJson = stripReasoningAndExtractJson(rawText);

            if (!parsedJson) {
              return resolve({
                success: false,
                statusCode: 200,
                error: 'Invalid JSON returned by Gemini'
              });
            }

            const category = normalizeCategory(parsedJson.category);
            const severity = normalizeSeverity(parsedJson.severity);
            let conf = parseFloat(parsedJson.confidence);
            if (isNaN(conf)) conf = 0.8;
            if (conf > 1) conf = conf / 100;
            conf = Math.max(0, Math.min(1, conf));

            return resolve({
              success: true,
              provider: 'gemini',
              model: model,
              category: category,
              severity: severity,
              title: parsedJson.title || `${category} Identified`,
              description: parsedJson.description || `Municipal audit assessment for ${category}.`,
              confidence: conf
            });
          } catch (e) {
            return resolve({
              success: false,
              statusCode: 200,
              error: 'Failed to parse Gemini response: ' + e.message
            });
          }
        });
      });

      req.on('timeout', () => {
        req.destroy();
        resolve({ success: false, timeout: true, error: 'Gemini request timed out' });
      });

      req.on('error', (err) => {
        resolve({ success: false, error: 'Gemini network error: ' + err.message });
      });

      req.write(payload);
      req.end();
    } catch (err) {
      resolve({ success: false, error: 'Gemini call failure: ' + err.message });
    }
  });
}

/**
 * NVIDIA Provider Call (OpenAI-compatible)
 */
async function callNvidiaProvider(targetImageObj, retryCount = 0) {
  const apiKey = (process.env.NVIDIA_API_KEY || '').trim();
  if (!apiKey || apiKey.length < 5 || apiKey.includes('YOUR_')) {
    return { skipped: true, reason: 'NVIDIA_API_KEY not configured' };
  }

  const baseUrl = (process.env.NVIDIA_BASE_URL || 'https://integrate.api.nvidia.com/v1').replace(/\/+$/, '');
  const model = (process.env.NVIDIA_VISION_MODEL || 'nvidia/nemotron-3-nano-omni-30b-a3b-reasoning').trim();
  const url = `${baseUrl}/chat/completions`;

  const payload = JSON.stringify({
    model: model,
    messages: [
      {
        role: "user",
        content: [
          {
            type: "image_url",
            image_url: {
              url: `data:${targetImageObj.mimeType};base64,${targetImageObj.base64}`
            }
          },
          {
            type: "text",
            text: SYSTEM_PROMPT
          }
        ]
      }
    ],
    temperature: 0.1,
    max_tokens: 1000
  });

  return new Promise((resolve) => {
    try {
      const parsedUrl = new URL(url);
      const isHttps = parsedUrl.protocol === 'https:';
      const client = isHttps ? https : require('http');

      const req = client.request({
        hostname: parsedUrl.hostname,
        port: parsedUrl.port || (isHttps ? 443 : 80),
        path: parsedUrl.pathname + parsedUrl.search,
        method: 'POST',
        timeout: 25000,
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`,
          'Content-Length': Buffer.byteLength(payload)
        }
      }, (res) => {
        let body = '';
        res.on('data', chunk => body += chunk);
        res.on('end', async () => {
          const status = res.statusCode;

          if (status === 429) {
            const isDaily = /daily|quota|tpd|rpd/i.test(body);
            const delay = parseRetryDelay(res.headers, body);
            if (!isDaily && delay <= 20 && retryCount < 2) {
              await new Promise(r => setTimeout(r, (delay + 1) * 1000));
              return callNvidiaProvider(targetImageObj, retryCount + 1).then(resolve);
            }
            return resolve({
              success: false,
              statusCode: 429,
              isRateLimit: true,
              isDaily: isDaily,
              retryAfter: delay,
              error: isDaily ? 'Daily quota exceeded' : 'Rate limit reached'
            });
          }

          if (status === 404) {
            return resolve({
              success: false,
              statusCode: 404,
              isModelNotFound: true,
              error: `NVIDIA model '${model}' not found`
            });
          }

          if (status >= 500) {
            return resolve({
              success: false,
              statusCode: status,
              isServerError: true,
              error: `NVIDIA server error (${status})`
            });
          }

          if (status !== 200) {
            return resolve({
              success: false,
              statusCode: status,
              error: `NVIDIA API error (${status}): ${body.substring(0, 150)}`
            });
          }

          try {
            const data = JSON.parse(body);
            const rawContent = data?.choices?.[0]?.message?.content;
            const parsedJson = stripReasoningAndExtractJson(rawContent);

            if (!parsedJson) {
              return resolve({
                success: false,
                statusCode: 200,
                error: 'Invalid JSON returned by NVIDIA'
              });
            }

            const category = normalizeCategory(parsedJson.category);
            const severity = normalizeSeverity(parsedJson.severity);
            let conf = parseFloat(parsedJson.confidence);
            if (isNaN(conf)) conf = 0.8;
            if (conf > 1) conf = conf / 100;
            conf = Math.max(0, Math.min(1, conf));

            return resolve({
              success: true,
              provider: 'nvidia',
              model: model,
              category: category,
              severity: severity,
              title: parsedJson.title || `${category} Identified`,
              description: parsedJson.description || `Municipal audit assessment for ${category}.`,
              confidence: conf
            });
          } catch (e) {
            return resolve({
              success: false,
              statusCode: 200,
              error: 'Failed to parse NVIDIA response: ' + e.message
            });
          }
        });
      });

      req.on('timeout', () => {
        req.destroy();
        resolve({ success: false, timeout: true, error: 'NVIDIA request timed out' });
      });

      req.on('error', (err) => {
        resolve({ success: false, error: 'NVIDIA network error: ' + err.message });
      });

      req.write(payload);
      req.end();
    } catch (err) {
      resolve({ success: false, error: 'NVIDIA call failure: ' + err.message });
    }
  });
}

/**
 * Execute Provider Chain with Fallback and Second-Opinion logic
 */
async function classifyWithProviderChain(targetImageInput) {
  let targetImageObj;
  try {
    targetImageObj = await validateAndFetchImageBase64(targetImageInput);
  } catch (err) {
    return {
      success: false,
      category: "needs_review",
      severity: "MEDIUM",
      title: "",
      description: "AI unavailable, please fill in manually",
      confidence: 0,
      message: "Image validation failed: " + err.message,
      provider: "none",
      attempts: []
    };
  }

  // Check SHA-256 Hash Cache
  const imageHash = crypto.createHash('sha256').update(targetImageObj.base64).digest('hex');
  const cached = getCachedResult(imageHash);
  if (cached && cached.success) {
    return {
      ...cached,
      fromCache: true
    };
  }

  const rawChain = (process.env.PROVIDER_CHAIN || 'gemini,nvidia')
    .split(',')
    .map(p => p.trim().toLowerCase())
    .filter(Boolean);

  const attempts = [];
  let primarySuccessResult = null;

  for (const providerName of rawChain) {
    if (providerName === 'gemini') {
      const res = await callGeminiProvider(targetImageObj);
      if (res.skipped) {
        attempts.push({ provider: 'gemini', status: 'skipped', reason: res.reason });
        continue;
      }
      if (res.success) {
        attempts.push({ provider: 'gemini', status: 'success', category: res.category, confidence: res.confidence });
        primarySuccessResult = res;

        // Stage 3 Rule: If Gemini confidence < 0.6 and NVIDIA configured, get a second opinion
        const nvidiaKey = (process.env.NVIDIA_API_KEY || '').trim();
        if (res.confidence < 0.6 && nvidiaKey && nvidiaKey.length > 5 && !nvidiaKey.includes('YOUR_')) {
          const secondOpinion = await callNvidiaProvider(targetImageObj);
          if (secondOpinion.success) {
            attempts.push({ provider: 'nvidia', status: 'second_opinion_success', category: secondOpinion.category, confidence: secondOpinion.confidence });
            if (secondOpinion.category.toLowerCase() === res.category.toLowerCase() && res.category !== 'needs_review') {
              // Agreement raises confidence
              primarySuccessResult.confidence = Math.min(0.95, Math.max(res.confidence, secondOpinion.confidence) + 0.25);
              primarySuccessResult.description += " (Verified by secondary AI consensus).";
            } else {
              // Disagreement
              if (secondOpinion.confidence >= 0.75) {
                primarySuccessResult = secondOpinion;
              } else {
                primarySuccessResult.category = "needs_review";
                primarySuccessResult.confidence = Math.max(res.confidence, secondOpinion.confidence);
                primarySuccessResult.description = "Conflicting classifications between AI providers. Manual review needed.";
              }
            }
          } else {
            attempts.push({ provider: 'nvidia', status: 'second_opinion_failed', error: secondOpinion.error });
          }
        }
        break;
      } else {
        attempts.push({
          provider: 'gemini',
          status: 'failed',
          error: res.error,
          statusCode: res.statusCode,
          retryAfter: res.retryAfter
        });
      }
    } else if (providerName === 'nvidia') {
      const res = await callNvidiaProvider(targetImageObj);
      if (res.skipped) {
        attempts.push({ provider: 'nvidia', status: 'skipped', reason: res.reason });
        continue;
      }
      if (res.success) {
        attempts.push({ provider: 'nvidia', status: 'success', category: res.category, confidence: res.confidence });
        primarySuccessResult = res;
        break;
      } else {
        attempts.push({
          provider: 'nvidia',
          status: 'failed',
          error: res.error,
          statusCode: res.statusCode,
          retryAfter: res.retryAfter
        });
      }
    }
  }

  if (primarySuccessResult) {
    const finalResult = {
      success: true,
      category: primarySuccessResult.category,
      severity: primarySuccessResult.severity,
      title: primarySuccessResult.title,
      description: primarySuccessResult.description,
      confidence: primarySuccessResult.confidence,
      provider: primarySuccessResult.provider,
      attempts: attempts
    };

    setCachedResult(imageHash, finalResult);
    return finalResult;
  }

  // All providers failed or not configured
  return {
    success: false,
    category: "needs_review",
    severity: "MEDIUM",
    title: "",
    description: "AI unavailable, please fill in manually",
    confidence: 0,
    message: "AI unavailable, please fill in manually",
    provider: "none",
    attempts: attempts
  };
}

module.exports = {
  classifyWithProviderChain,
  ALLOWED_CATEGORIES,
  normalizeCategory,
  normalizeSeverity
};
