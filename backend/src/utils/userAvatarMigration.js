async function migrateUserAvatarField(db) {
  const users = db.collection('users');
  const usersWithAvatar = await users.find({ Avatar: { $exists: true } }).toArray();

  let copiedCount = 0;
  for (const user of usersWithAvatar) {
    const legacyAvatar = typeof user.Avatar === 'string' ? user.Avatar.trim() : '';
    const avatarUrl = typeof user.avatarUrl === 'string' ? user.avatarUrl.trim() : '';
    const update = { $unset: { Avatar: '' } };

    if (legacyAvatar && !avatarUrl) {
      update.$set = { avatarUrl: legacyAvatar };
      copiedCount++;
    }

    await users.updateOne({ _id: user._id }, update);
  }

  if (usersWithAvatar.length > 0) {
    console.log(`Migrated user Avatar field: copied ${copiedCount}, removed ${usersWithAvatar.length}`);
  }
}

module.exports = {
  migrateUserAvatarField,
};
