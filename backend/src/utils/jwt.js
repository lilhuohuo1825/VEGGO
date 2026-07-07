const jwt = require('jsonwebtoken');

const JWT_SECRET = process.env.JWT_SECRET || 'dev_jwt_secret_change_me';
const JWT_ISSUER = process.env.JWT_ISSUER || 'veggo';

function signAccessToken(payload, options = {}) {
  const expiresIn = options.expiresIn || process.env.JWT_EXPIRES_IN || '30d';
  return jwt.sign(payload, JWT_SECRET, {
    expiresIn,
    issuer: JWT_ISSUER,
  });
}

function verifyAccessToken(token) {
  return jwt.verify(token, JWT_SECRET, { issuer: JWT_ISSUER });
}

module.exports = {
  signAccessToken,
  verifyAccessToken,
};

