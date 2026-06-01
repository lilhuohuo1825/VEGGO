const admin = require('../config/firebaseAdmin');

async function verifyFirebaseToken(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.substring(7) : null;

  if (!token) {
    return res.status(401).json({ message: 'Missing Firebase ID token' });
  }

  if (!admin.apps || admin.apps.length === 0) {
    return res.status(503).json({ message: 'Firebase Admin is not configured on backend' });
  }

  try {
    req.firebaseUser = await admin.auth().verifyIdToken(token);
    next();
  } catch (error) {
    res.status(401).json({ message: 'Invalid Firebase ID token' });
  }
}

module.exports = verifyFirebaseToken;
