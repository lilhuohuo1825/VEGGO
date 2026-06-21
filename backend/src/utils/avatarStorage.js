const fs = require('fs');
const path = require('path');
const admin = require('../config/firebaseAdmin');

const LOCAL_UPLOAD_DIR = path.resolve(process.cwd(), 'uploads', 'avatars');

function ensureLocalUploadDir() {
  if (!fs.existsSync(LOCAL_UPLOAD_DIR)) {
    fs.mkdirSync(LOCAL_UPLOAD_DIR, { recursive: true });
  }
}

function buildLocalAvatarUrl(req, fileName) {
  const configuredBase = process.env.PUBLIC_BASE_URL;
  if (configuredBase) {
    return `${configuredBase.replace(/\/$/, '')}/uploads/avatars/${fileName}`;
  }
  const host = req.get('host');
  const protocol = req.protocol;
  return `${protocol}://${host}/uploads/avatars/${fileName}`;
}

async function uploadToFirebaseStorage(buffer, contentType, userId) {
  if (!admin.apps.length) {
    return null;
  }

  try {
    const bucket = admin.storage().bucket();
    const extension = contentType.includes('png') ? 'png' : 'jpg';
    const objectPath = `user_avatars/${userId}_${Date.now()}.${extension}`;
    const file = bucket.file(objectPath);

    await file.save(buffer, {
      metadata: { contentType },
      resumable: false,
    });

    await file.makePublic();
    return `https://storage.googleapis.com/${bucket.name}/${objectPath}`;
  } catch (error) {
    console.warn('Firebase avatar upload failed, falling back to local storage:', error.message);
    return null;
  }
}

async function uploadAvatar(req, file, userId) {
  if (!file || !file.buffer) {
    return null;
  }

  const firebaseUrl = await uploadToFirebaseStorage(file.buffer, file.mimetype, userId);
  if (firebaseUrl) {
    return firebaseUrl;
  }

  ensureLocalUploadDir();
  const extension = file.mimetype.includes('png') ? 'png' : 'jpg';
  const fileName = `${userId}_${Date.now()}.${extension}`;
  const destination = path.join(LOCAL_UPLOAD_DIR, fileName);
  fs.writeFileSync(destination, file.buffer);
  return buildLocalAvatarUrl(req, fileName);
}

module.exports = {
  uploadAvatar,
  LOCAL_UPLOAD_DIR,
};
