const fs = require('fs');
const path = require('path');
const admin = require('firebase-admin');

function initFirebaseAdmin() {
  if (admin.apps.length > 0) {
    return admin;
  }

  const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT_PATH;
  if (!serviceAccountPath) {
    console.warn('FIREBASE_SERVICE_ACCOUNT_PATH is not set. Firebase Admin features are disabled.');
    return admin;
  }

  const resolvedPath = path.resolve(process.cwd(), serviceAccountPath);
  if (!fs.existsSync(resolvedPath)) {
    console.warn(`Firebase service account not found at ${resolvedPath}. Firebase Admin features are disabled.`);
    return admin;
  }

  const serviceAccount = require(resolvedPath);
  const defaultStorageBucket = serviceAccount.project_id
    ? `${serviceAccount.project_id}.firebasestorage.app`
    : undefined;
  const appOptions = {
    credential: admin.credential.cert(serviceAccount),
    projectId: serviceAccount.project_id,
  };

  if (process.env.FIREBASE_STORAGE_BUCKET || defaultStorageBucket) {
    appOptions.storageBucket = process.env.FIREBASE_STORAGE_BUCKET || defaultStorageBucket;
  }

  admin.initializeApp(appOptions);

  console.log('Firebase Admin initialized');
  return admin;
}

module.exports = initFirebaseAdmin();
