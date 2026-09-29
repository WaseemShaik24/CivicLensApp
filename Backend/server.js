const fs = require('fs');
const path = require('path');
const http = require('http');
const crypto = require('crypto');

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

const { processHybridAiAnalysis, generateCivicComplaint } = require('./hybridAiEngine');
const { classifyWithProviderChain } = require('./aiEngine');

const PORT = process.env.PORT || 3000;

// JSON File Database paths
const DB_DIR = path.join(__dirname, '../Database');
const USERS_FILE = path.join(DB_DIR, 'users.json');
const REPORTS_FILE = path.join(DB_DIR, 'reports.json');

// Ensure DB directory & JSON files exist
if (!fs.existsSync(DB_DIR)) fs.mkdirSync(DB_DIR, { recursive: true });
if (!fs.existsSync(USERS_FILE)) fs.writeFileSync(USERS_FILE, JSON.stringify([], null, 2));
if (!fs.existsSync(REPORTS_FILE)) {
  const sampleReports = [
    {
      id: "CIVIC-2026-X892",
      userEmail: "citizen.john@civiclens.org",
      title: "Dangerous Deep Pothole on Anna Salai Road",
      category: "Potholes",
      categoryName: "Potholes",
      severity: "HIGH",
      status: "FILED",
      description: "Deep 6-inch asphalt pothole near crosswalk causing vehicle damage and traffic slowdowns.",
      department: "Greater Chennai Corporation (GCC) - Public Works",
      address: "Anna Salai, Mount Road, T. Nagar, Chennai, Tamil Nadu, India",
      latitude: 13.0604,
      longitude: 80.2496,
      timestamp: Date.now() - 600000
    }
  ];
  fs.writeFileSync(REPORTS_FILE, JSON.stringify(sampleReports, null, 2));
}

function getUsers() {
  return JSON.parse(fs.readFileSync(USERS_FILE, 'utf8'));
}
function saveUsers(users) {
  fs.writeFileSync(USERS_FILE, JSON.stringify(users, null, 2));
}
function getReports() {
  return JSON.parse(fs.readFileSync(REPORTS_FILE, 'utf8'));
}
function saveReports(reports) {
  fs.writeFileSync(REPORTS_FILE, JSON.stringify(reports, null, 2));
}
function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, DELETE');

  if (req.method === 'OPTIONS') {
    res.writeHead(200);
    return res.end();
  }

  let body = '';
  req.on('data', chunk => { body += chunk.toString(); });
  req.on('end', async () => {
    let parsedBody = {};
    if (body) {
      try { parsedBody = JSON.parse(body); } catch (e) {}
    }

    // Route: GET /api/auth/check-email?email=...
    if (req.url.startsWith('/api/auth/check-email') && req.method === 'GET') {
      const urlObj = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
      const email = urlObj.searchParams.get('email');
      if (!email) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Email query parameter required' }));
      }

      const users = getUsers();
      const user = users.find(u => u.email && u.email.toLowerCase() === email.toLowerCase());
      if (user) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          exists: true,
          provider: user.provider || 'password'
        }));
      } else {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ exists: false, provider: null }));
      }
    }

    // Route: GET /api/auth/check-phone?phone=...
    if (req.url.startsWith('/api/auth/check-phone') && req.method === 'GET') {
      const urlObj = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
      const phone = urlObj.searchParams.get('phone');
      if (!phone) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Phone query parameter required' }));
      }

      const users = getUsers();
      const user = users.find(u => u.phone && u.phone === phone.trim());
      if (user) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          exists: true,
          provider: user.provider || 'phone'
        }));
      } else {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ exists: false, provider: null }));
      }
    }

    // Route: POST /api/auth/sync-user
    if (req.url === '/api/auth/sync-user' && req.method === 'POST') {
      const { uid, email, phone, name, provider } = parsedBody;
      const users = getUsers();
      let user = null;

      if (email) {
        user = users.find(u => u.email && u.email.toLowerCase() === email.toLowerCase());
      }
      if (!user && phone) {
        user = users.find(u => u.phone && u.phone === phone.trim());
      }
      if (!user && uid) {
        user = users.find(u => u.id === uid || u.uid === uid);
      }

      if (user) {
        if (name && (!user.name || user.name === 'Citizen')) user.name = name;
        if (email && !user.email) user.email = email.toLowerCase();
        if (phone && !user.phone) user.phone = phone.trim();

        if (!user.providers) user.providers = [user.provider || 'password'];
        if (provider && !user.providers.includes(provider)) {
          user.providers.push(provider);
        }
        user.lastLogin = new Date().toISOString();
        saveUsers(users);

        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ message: 'User profile merged successfully', user }));
      } else {
        const newUser = {
          id: uid || 'usr_' + Date.now(),
          uid: uid || 'usr_' + Date.now(),
          name: name || 'Citizen',
          email: email ? email.toLowerCase() : '',
          phone: phone ? phone.trim() : '',
          provider: provider || 'google',
          providers: [provider || 'google'],
          createdAt: new Date().toISOString(),
          lastLogin: new Date().toISOString()
        };
        users.push(newUser);
        saveUsers(users);

        res.writeHead(201, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ message: 'Unified account created', user: newUser }));
      }
    }

    // Route: POST /api/auth/register
    if (req.url === '/api/auth/register' && req.method === 'POST') {
      const { name, email, password } = parsedBody;
      if (!email || !password) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Email and password are required' }));
      }

      const users = getUsers();
      const existingUser = users.find(u => u.email && u.email.toLowerCase() === email.toLowerCase());

      if (existingUser) {
        if (!existingUser.passwordHash) {
          existingUser.passwordHash = hashPassword(password);
          if (name && existingUser.name === 'Citizen') existingUser.name = name;
          if (!existingUser.providers) existingUser.providers = [existingUser.provider || 'google'];
          if (!existingUser.providers.includes('password')) existingUser.providers.push('password');
          saveUsers(users);

          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            message: 'Password created and linked to your existing account! You can now log in with either Google or Password.',
            user: { id: existingUser.id, name: existingUser.name, email: existingUser.email, providers: existingUser.providers }
          }));
        }

        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          error: 'An account with this email address already exists. Please log in using your password or sign in with Google.'
        }));
      }

      const newUser = {
        id: 'usr_' + Date.now(),
        name: name || 'Citizen',
        email: email.toLowerCase(),
        provider: 'password',
        providers: ['password'],
        passwordHash: hashPassword(password),
        createdAt: new Date().toISOString()
      };

      users.push(newUser);
      saveUsers(users);

      res.writeHead(201, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({
        message: 'Registration successful! Credential stored in Database.',
        user: { id: newUser.id, name: newUser.name, email: newUser.email, providers: newUser.providers }
      }));
    }

    // Route: POST /api/auth/login
    if (req.url === '/api/auth/login' && req.method === 'POST') {
      const { email, password } = parsedBody;
      if (!email || typeof email !== 'string' || !password) {
        res.writeHead(400, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Valid email and password are required' }));
      }

      const cleanEmail = email.trim().toLowerCase();
      const users = getUsers();
      const user = users.find(u => u.email && typeof u.email === 'string' && u.email.toLowerCase() === cleanEmail);

      if (user) {
        if (!user.passwordHash && password && password.length >= 4) {
          user.passwordHash = hashPassword(password);
          if (!user.providers) user.providers = [user.provider || 'google'];
          if (!user.providers.includes('password')) user.providers.push('password');
          saveUsers(users);

          const token = 'token_' + crypto.randomBytes(16).toString('hex');
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            message: 'Password created and linked to your existing Google account! Logged in successfully.',
            token: token,
            user: { id: user.id, name: user.name, email: user.email, providers: user.providers }
          }));
        }

        if (user.passwordHash === hashPassword(password)) {
          const token = 'token_' + crypto.randomBytes(16).toString('hex');
          res.writeHead(200, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({
            message: 'Authentication successful',
            token: token,
            user: { id: user.id, name: user.name, email: user.email, providers: user.providers || ['password'] }
          }));
        } else {
          res.writeHead(401, { 'Content-Type': 'application/json' });
          return res.end(JSON.stringify({ error: 'Invalid password entered. Please try again or click Sign in with Google.' }));
        }
      }

      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'No account found with this email. Click "Create Account" to register.' }));
    }

    // Route: GET /api/reports
    if (req.url === '/api/reports' && req.method === 'GET') {
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify(getReports()));
    }

    // Route: POST /api/reports
    if (req.url === '/api/reports' && req.method === 'POST') {
      const reports = getReports();
      const newReport = {
        id: parsedBody.id || ('CIVIC-2026-' + Math.floor(1000 + Math.random() * 9000)),
        userEmail: parsedBody.userEmail || 'citizen@civiclens.org',
        title: parsedBody.title || 'Civic Issue',
        category: parsedBody.category || 'Potholes',
        categoryName: parsedBody.categoryName || parsedBody.category || 'Potholes',
        severity: (parsedBody.severity || 'HIGH').toUpperCase(),
        status: parsedBody.status || 'FILED',
        description: parsedBody.description || '',
        department: parsedBody.department || 'Greater Chennai Corporation (GCC)',
        address: parsedBody.address || 'Anna Salai, Chennai, Tamil Nadu, India',
        img: parsedBody.img || parsedBody.photoUrl || 'https://images.unsplash.com/photo-1515162816999-a0c47dc192f7?auto=format&fit=crop&w=600&q=80',
        latitude: parsedBody.latitude || parsedBody.lat || 13.0827,
        longitude: parsedBody.longitude || parsedBody.lng || 80.2707,
        lat: parsedBody.latitude || parsedBody.lat || 13.0827,
        lng: parsedBody.longitude || parsedBody.lng || 80.2707,
        timestamp: parsedBody.timestamp || Date.now()
      };
      reports.unshift(newReport);
      saveReports(reports);

      res.writeHead(201, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ message: 'Report saved to database', report: newReport }));
    }

    // Route: DELETE /api/reports or /api/reports/:id
    if (req.url.startsWith('/api/reports') && req.method === 'DELETE') {
      const urlParts = req.url.split('?')[0].split('/');
      if (urlParts.length >= 4 && urlParts[3]) {
        const targetId = decodeURIComponent(urlParts[3]);
        let reports = getReports();
        reports = reports.filter(r => r.id !== targetId);
        saveReports(reports);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true, message: `Report ${targetId} withdrawn successfully.` }));
      } else {
        saveReports([]);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ success: true, message: 'All reports withdrawn and database cleared.' }));
      }
    }

    // Route: POST /api/ai/classify (Primary AI Classification)
    if (req.url === '/api/ai/classify' && req.method === 'POST') {
      const { imageBase64, imagePresetType } = parsedBody;
      try {
        const classification = await classifyWithProviderChain(imageBase64 || imagePresetType);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify(classification));
      } catch (err) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          success: false,
          category: "needs_review",
          severity: "MEDIUM",
          title: "",
          description: "AI unavailable, please fill in manually",
          confidence: 0,
          message: "AI unavailable, please fill in manually",
          provider: "none",
          attempts: [{ error: err.message }]
        }));
      }
    }

    // Route: POST /api/ai/generate-complaint (Category Authoritative Complaint Generation)
    if (req.url === '/api/ai/generate-complaint' && req.method === 'POST') {
      const { classification, location } = parsedBody;
      try {
        const complaint = generateCivicComplaint(classification || {}, location || {});
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify(complaint));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({ error: 'Complaint generation failed', details: err.message }));
      }
    }

    // Route: POST /api/ai/classify-hybrid (Full AI Provider Chain Pipeline)
    if ((req.url === '/api/ai/classify-hybrid' || req.url === '/api/analyze') && req.method === 'POST') {
      const { imageBase64, imagePresetType } = parsedBody;
      try {
        const hybridResult = await processHybridAiAnalysis(imageBase64, imagePresetType);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify(hybridResult));
      } catch (err) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
          success: false,
          category: "needs_review",
          severity: "MEDIUM",
          title: "",
          description: "AI unavailable, please fill in manually",
          confidence: 0,
          message: "AI unavailable, please fill in manually",
          provider: "none",
          attempts: [{ error: err.message }]
        }));
      }
    }

    // Static Frontend File Serving
    if (req.method === 'GET' && !req.url.startsWith('/api/')) {
      let safePath = req.url.split('?')[0];
      if (safePath === '/') safePath = '/index.html';
      const FRONTEND_DIR = path.join(__dirname, '../Frontend');
      const filePath = path.join(FRONTEND_DIR, safePath);

      if (filePath.startsWith(FRONTEND_DIR) && fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
        const ext = path.extname(filePath).toLowerCase();
        const mimeTypes = {
          '.html': 'text/html; charset=UTF-8',
          '.css': 'text/css; charset=UTF-8',
          '.js': 'application/javascript; charset=UTF-8',
          '.json': 'application/json',
          '.png': 'image/png',
          '.jpg': 'image/jpeg',
          '.jpeg': 'image/jpeg',
          '.svg': 'image/svg+xml',
          '.ico': 'image/x-icon'
        };
        const contentType = mimeTypes[ext] || 'application/octet-stream';
        res.writeHead(200, { 'Content-Type': contentType });
        return res.end(fs.readFileSync(filePath));
      }
    }

    // Default 404
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'Endpoint not found' }));
  });
});

server.listen(PORT, () => {
  const geminiSet = (process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY.trim().length > 5) ? 'yes' : 'no';
  const nvidiaSet = (process.env.NVIDIA_API_KEY && process.env.NVIDIA_API_KEY.trim().length > 5) ? 'yes' : 'no';
  const geminiModel = process.env.GEMINI_MODEL || 'gemini-2.5-flash';
  const nvidiaModel = process.env.NVIDIA_VISION_MODEL || 'nvidia/nemotron-3-nano-omni-30b-a3b-reasoning';

  console.log(`CivicLens Database API Server running on port ${PORT}`);
  console.log(`GEMINI_API_KEY set: ${geminiSet}`);
  console.log(`NVIDIA_API_KEY set: ${nvidiaSet}`);
  console.log(`Available AI Models: Gemini (${geminiModel}), NVIDIA (${nvidiaModel})`);
  console.log(`User Database path: ${USERS_FILE}`);
  console.log(`Reports Database path: ${REPORTS_FILE}`);
});
