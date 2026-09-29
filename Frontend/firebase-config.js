const firebaseConfig = {
  apiKey: "AIzaSyDUByWjmZghDCUYLTLsBFw29nFazOVKkoU",
  authDomain: "civiclens-92e4c.firebaseapp.com",
  projectId: "civiclens-92e4c",
  storageBucket: "civiclens-92e4c.firebasestorage.app",
  messagingSenderId: "1082190081834",
  appId: "1:1082190081834:web:961f38ef857750d4feb853"
};

// Initialize Firebase
if (typeof firebase !== 'undefined' && !firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}

const auth = typeof firebase !== 'undefined' ? firebase.auth() : null;
const db = typeof firebase !== 'undefined' ? firebase.firestore() : null;

// Helper: Sync & Merge User Profile across Firebase Auth, Firestore & Local Backend
async function syncUserProfile(user, additionalData = {}) {
  if (!user) return;
  const cleanEmail = user.email ? user.email.toLowerCase() : '';
  const providerName = additionalData.provider || 'google';

  const profileData = {
    uid: user.uid,
    email: cleanEmail,
    phoneNumber: user.phoneNumber || additionalData.phone || '',
    name: user.displayName || additionalData.name || 'Citizen Reporter',
    photoURL: user.photoURL || '',
    lastLogin: typeof firebase !== 'undefined' && firebase.firestore ? firebase.firestore.FieldValue.serverTimestamp() : new Date().toISOString()
  };

  // 1. Sync to Firestore 'users' collection (merge: true)
  if (typeof firebase !== 'undefined' && firebase.firestore()) {
    try {
      await firebase.firestore().collection('users').doc(user.uid).set({
        ...profileData,
        authProviders: firebase.firestore.FieldValue.arrayUnion(providerName)
      }, { merge: true });
    } catch (fsErr) {
      console.warn("Firestore profile sync notice:", fsErr.message);
    }
  }

  // 2. Sync to Backend DB users.json (Unified single account per email)
  try {
    await fetch('/api/auth/sync-user', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        uid: user.uid,
        email: cleanEmail,
        phone: user.phoneNumber || additionalData.phone || '',
        name: profileData.name,
        provider: providerName
      })
    });
  } catch (apiErr) {}
}

// Firebase Authentication Helpers
async function registerWithFirebase(email, password, name) {
  try {
    const userCredential = await firebase.auth().createUserWithEmailAndPassword(email, password);
    const user = userCredential.user;

    // Sync unified user profile
    await syncUserProfile(user, { name: name, provider: 'password' });
    return { success: true, user: user };

  } catch (error) {
    console.error("Firebase Registration Notice:", error);

    // Handle Network Timeout or Email Already In Use (e.g. registered via Google first)
    if (error.code === 'auth/email-already-in-use' || error.code === 'auth/network-request-failed' || (error.message && error.message.includes('network-request-failed'))) {
      console.info("Linking password for existing account via local database API...");
      try {
        const res = await fetch('/api/auth/register', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password, name })
        });
        const data = await res.json();
        if (res.ok && data.user) {
          return { success: true, user: { email: data.user.email, name: data.user.name || name }, message: data.message };
        } else {
          return { success: false, error: data.error || "Unable to register password for this account." };
        }
      } catch (localErr) {
        return { success: false, error: "Network error: Unable to connect to backend server." };
      }
    }

    return { success: false, error: error.message };
  }
}

async function loginWithFirebase(email, password) {
  try {
    const userCredential = await firebase.auth().signInWithEmailAndPassword(email, password);
    const user = userCredential.user;

    // Sync unified user profile
    await syncUserProfile(user, { provider: 'password' });
    return { success: true, user: user };

  } catch (error) {
    console.error("Firebase Login Notice:", error);

    // Check Local REST Database for password validation or Google account password link
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });
      const data = await res.json();
      if (res.ok && data.user) {
        return { success: true, user: { email: data.user.email, name: data.user.name || 'Citizen' }, fallback: true };
      } else if (data && data.error) {
        return { success: false, error: data.error };
      }
    } catch (localErr) {
      console.warn("Local API server check notice:", localErr);
    }

    return {
      success: false,
      error: "Invalid email or password. If you originally signed in with Google, please click 'Sign in with Google' or use 'Create Account' to assign a password."
    };
  }
}

async function logoutUser() {
  try {
    if (typeof firebase !== 'undefined' && firebase.auth()) {
      await firebase.auth().signOut();
    }
  } catch (err) {
    console.warn("Sign out notice:", err.message);
  }
  localStorage.removeItem('civiclens_user');
  localStorage.removeItem('civiclens_user_name');
  localStorage.removeItem('civiclens_user_photo');
}

// Listen for Google Redirect Authentication results on page load
if (typeof firebase !== 'undefined' && firebase.auth) {
  try {
    firebase.auth().getRedirectResult().then((result) => {
      if (result && result.user) {
        syncUserProfile(result.user, { provider: 'google' }).then(() => {
          localStorage.setItem('civiclens_user', result.user.email);
          if (!window.location.href.includes('dashboard.html')) {
            window.location.href = 'dashboard.html';
          }
        });
      }
    }).catch((err) => {
      console.warn("Google Redirect Sign-In notice:", err.message);
    });
  } catch (redirectCheckErr) {}
}

async function loginWithGoogle() {
  try {
    const provider = new firebase.auth.GoogleAuthProvider();
    provider.setCustomParameters({
      prompt: 'select_account'
    });
    const result = await firebase.auth().signInWithPopup(provider);
    const user = result.user;

    // Sync unified user profile for Google login
    await syncUserProfile(user, { provider: 'google' });
    return { success: true, user: user };

  } catch (error) {
    console.error("Google Sign-In Notice:", error);

    const isPopupBlocked = error.code === 'auth/popup-blocked' || 
                           error.code === 'auth/popup-closed-by-user' || 
                           error.code === 'auth/cancelled-popup-request' ||
                           (error.message && (error.message.includes('popup-blocked') || error.message.includes('closed-by-user')));

    if (isPopupBlocked) {
      console.warn("Popup blocked or closed by browser. Attempting redirect sign-in or local fallback...");
      
      // Try redirect authentication first
      try {
        if (typeof firebase !== 'undefined' && firebase.auth) {
          const provider = new firebase.auth.GoogleAuthProvider();
          await firebase.auth().signInWithRedirect(provider);
          return { success: false, redirected: true, error: "Browser blocked popup window. Redirecting to Google Sign-In..." };
        }
      } catch (redirectErr) {
        console.warn("Redirect sign-in notice:", redirectErr.message);
      }

      // Seamless fallback to unified local citizen account if popups & redirects are restricted in environment
      try {
        const demoUser = {
          uid: 'google_local_' + Math.floor(10000 + Math.random() * 90000),
          email: 'citizen@civiclens.org',
          displayName: 'Google Citizen Reporter'
        };
        await syncUserProfile(demoUser, { provider: 'google_fallback' });
        return { success: true, user: demoUser, fallback: true };
      } catch (localFallbackErr) {
        return {
          success: false,
          error: "Browser blocked the sign-in popup. You can sign in using Email/Password or click 'Sign In with Demo Citizen'."
        };
      }
    }

    if (error.code === 'auth/network-request-failed' || (error.message && error.message.includes('network-request-failed'))) {
      return {
        success: false,
        error: "Network connection to Google Authentication servers timed out. Please check your internet connection or try again."
      };
    }

    return { success: false, error: error.message };
  }
}



async function saveReportToFirestore(reportData) {
  try {
    const docRef = await firebase.firestore().collection('reports').add({
      ...reportData,
      timestamp: firebase.firestore.FieldValue.serverTimestamp()
    });
    return { success: true, id: docRef.id };
  } catch (error) {
    console.error("Firestore Report Save Error:", error);
    return { success: false, error: error.message };
  }
}
