function toDate(value) {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value === 'string' || typeof value === 'number') {
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  if (typeof value === 'object' && value.$date) {
    const parsed = new Date(value.$date);
    return Number.isNaN(parsed.getTime()) ? null : parsed;
  }
  return null;
}

async function migrateUserDateFields(db) {
  const users = db.collection('users');
  const candidates = await users.find({
    $or: [
      { RegisterDate: { $type: 'object' } },
      { updated_at: { $type: 'object' } },
      { LastPasswordReset: { $type: 'object' } },
      { CertificateGrantedAt: { $type: 'object' } },
    ],
  }).toArray();

  let fixedCount = 0;
  for (const user of candidates) {
    const update = {};
    const registerDate = toDate(user.RegisterDate);
    const updatedAt = toDate(user.updated_at);
    const lastPasswordReset = toDate(user.LastPasswordReset);
    const certificateGrantedAt = toDate(user.CertificateGrantedAt);

    if (user.RegisterDate && registerDate) {
      update.RegisterDate = registerDate;
    }
    if (user.updated_at && updatedAt) {
      update.updated_at = updatedAt;
    }
    if (user.LastPasswordReset && lastPasswordReset) {
      update.LastPasswordReset = lastPasswordReset;
    }
    if (user.CertificateGrantedAt && certificateGrantedAt) {
      update.CertificateGrantedAt = certificateGrantedAt;
    }

    if (Object.keys(update).length > 0) {
      await users.updateOne({ _id: user._id }, { $set: update });
      fixedCount++;
    }
  }

  if (fixedCount > 0) {
    console.log(`Migrated malformed user date fields: fixed ${fixedCount} user(s)`);
  }
}

module.exports = {
  migrateUserDateFields,
  toDate,
};
