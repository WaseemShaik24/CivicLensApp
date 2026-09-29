/**
 * CivicLens Advanced Multimodal Classification & Municipal Complaint Engine
 * Powered by Gemini 2.5 Flash & NVIDIA Fallback Chain
 */

const { classifyWithProviderChain, normalizeCategory } = require('./aiEngine');

const CATEGORY_DETAILS = {
  "Potholes": {
    department: "Greater Chennai Corporation (GCC) - Highways & Public Works",
    defaultTitle: "Severe Pothole & Road Surface Fracture",
    defaultSeverity: "HIGH",
    impact: "High-risk road depression endangering two-wheeler commuters and causing vehicular suspension damage.",
    directives: "a) Deploy emergency road repair crew within 24 hours.\nb) Install reflective barricades.\nc) Complete cold-mix asphalt restoration within 48 hours."
  },
  "Waste": {
    department: "Greater Chennai Corporation (GCC) - Solid Waste Management Department",
    defaultTitle: "Illegal Garbage Dump & Waste Accumulation",
    defaultSeverity: "MEDIUM",
    impact: "Accumulated solid waste creating severe bio-hazard conditions and blocking public footpaths.",
    directives: "a) Dispatch compactor truck for immediate waste removal.\nb) Sanitize affected public zone within 12 hours."
  },
  "Streetlights": {
    department: "Tamil Nadu Generation and Distribution Corporation (TANGEDCO) / GCC Electrical Department",
    defaultTitle: "Damaged Utility Pole & Exposed Wiring",
    defaultSeverity: "HIGH",
    impact: "Critically damaged streetlight infrastructure posing electrocution and structural collapse hazards.",
    directives: "a) Isolate electrical supply immediately.\nb) Replace damaged pole and repair fixtures within 24 hours."
  },
  "Open Manholes": {
    department: "Chennai Metropolitan Water Supply and Sewerage Board (CMWSSB)",
    defaultTitle: "Exposed Open Manhole & Broken Drainage Opening",
    defaultSeverity: "HIGH",
    impact: "Fatal fall hazard for pedestrians and two-wheelers, particularly during night hours and rainy conditions.",
    directives: "a) Install heavy-duty concrete manhole cover immediately.\nb) Place warning cones and reflective markers around opening."
  },
  "Water Leaks": {
    department: "Chennai Metropolitan Water Supply and Sewerage Board (CMWSSB)",
    defaultTitle: "High-Pressure Water Main Burst & Local Flooding",
    defaultSeverity: "HIGH",
    impact: "Continuous potable water loss causing severe sub-surface asphalt erosion and street inundation.",
    directives: "a) Shut off isolation valve immediately.\nb) Excavate and patch burst pipe within 12 hours."
  },
  "needs_review": {
    department: "Greater Chennai Corporation (GCC) - Zonal Inspection Office",
    defaultTitle: "Civic Infrastructure Issue - Manual Review Needed",
    defaultSeverity: "MEDIUM",
    impact: "Visual evidence provided is inconclusive or automated AI evaluation is unavailable.",
    directives: "a) Review uploaded photographic evidence manually.\nb) Dispatch zonal inspector for on-site verification."
  }
};

/**
 * Category Authoritative Complaint Generator
 */
function generateCivicComplaint(verifiedClassification, locationData) {
  const rawCat = verifiedClassification.category;
  const finalCategory = normalizeCategory(rawCat);

  let rawConf = verifiedClassification.confidence;
  if (rawConf === undefined || rawConf === null) rawConf = 0;
  if (rawConf > 1) rawConf = rawConf / 100;
  const confidenceScore = Math.max(0, Math.min(1, rawConf));
  const confidencePercent = Math.round(confidenceScore * 100);

  const config = CATEGORY_DETAILS[finalCategory] || CATEGORY_DETAILS["needs_review"];

  const department = verifiedClassification.suggestedDepartment || config.department;
  const title = verifiedClassification.title || config.defaultTitle;
  const severity = verifiedClassification.severity || config.defaultSeverity;
  const description = verifiedClassification.description || config.impact;
  const impact = config.impact;
  const directives = config.directives;

  const dateStr = new Date().toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
  const address = (locationData && locationData.address) ? locationData.address : "Anna Salai, Chennai, Tamil Nadu, India";

  const formalLetter = `FORMAL CIVIC COMPLAINT LETTER & MUNICIPAL AUDIT REPORT
Date of Filing: ${dateStr}

TO,
The Superintending Engineer / Zonal Officer,
${department},
Chennai Municipal Corporation, Tamil Nadu, India.

SUBJECT: Formal Notice & Immediate Action Directive Request for ${title}.

Respected Officer,

I am writing this formal municipal complaint letter to bring to your immediate attention a critical civic infrastructure defect identified during an AI-assisted community audit located at:
📍 Location: ${address}

1. AUDIT FINDINGS & VERIFIED CLASSIFICATION:
- Issue Category: ${finalCategory}
- Classification Confidence: ${confidencePercent}%
- Visual Assessment: ${description}
- Municipal Severity: ${severity} SEVERITY

2. PUBLIC RISK ASSESSMENT & COMMUNITY IMPACT:
${impact}

3. REQUIRED MUNICIPAL ACTION DIRECTIVES:
${directives}

We anticipate your prompt official acknowledgment and action. Progress will be tracked live via the CivicLens Community Audit Portal.

Yours faithfully,
Citizen Auditor (Verified via CivicLens System)`;

  return {
    success: verifiedClassification.success !== false,
    category: finalCategory,
    severity: severity,
    title: title,
    description: description,
    confidence: confidenceScore,
    provider: verifiedClassification.provider || "gemini",
    attempts: verifiedClassification.attempts || [],
    reason: description,
    alternative: verifiedClassification.alternative || "None",
    status: (finalCategory === "needs_review" || confidenceScore < 0.6) ? "needs_review" : "accepted",
    aiProvider: verifiedClassification.provider || "gemini",
    summary: title,
    technicalDetails: formalLetter,
    formalLetter: formalLetter,
    suggestedDepartment: department,
    hybridConfidenceScore: confidenceScore,
    message: verifiedClassification.message || (finalCategory === "needs_review" ? "AI unavailable, please fill in manually" : undefined),
    timestamp: new Date().toISOString()
  };
}

/**
 * Master Hybrid Evaluation Entry Point
 */
async function processHybridAiAnalysis(imageBase64, imagePresetType) {
  // Classification with Provider Chain (Gemini primary, NVIDIA fallback)
  const classification = await classifyWithProviderChain(imageBase64 || imagePresetType);

  if (classification.success === false && classification.category === "needs_review") {
    const fullResult = generateCivicComplaint(classification, null);
    fullResult.success = false;
    fullResult.category = "needs_review";
    fullResult.message = classification.message || "AI unavailable, please fill in manually";
    fullResult.provider = classification.provider || "none";
    fullResult.attempts = classification.attempts || [];
    return fullResult;
  }

  // Complaint Generation using verified category
  const fullResult = generateCivicComplaint(classification, null);
  fullResult.success = true;
  fullResult.provider = classification.provider;
  fullResult.attempts = classification.attempts;
  return fullResult;
}

module.exports = {
  generateCivicComplaint,
  processHybridAiAnalysis
};
