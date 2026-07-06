const mongoose = require('mongoose');

async function createCommunityNotification({
  customerId,
  type,
  title,
  body,
  action = 'Xem chi tiết',
  targetId = '',
  actorName = '',
  recipeTitle = '',
}) {
  const recipientId = String(customerId || '').trim();
  if (!recipientId) {
    return;
  }

  await mongoose.connection.db.collection('notifications').insertOne({
    CustomerID: recipientId,
    category: 'community',
    type: type || 'community',
    title: title || 'Cập nhật cộng đồng',
    body: body || '',
    action,
    iconText: '♥',
    targetType: 'community',
    targetId: String(targetId || '').trim(),
    actorName: String(actorName || '').trim(),
    recipeTitle: String(recipeTitle || '').trim(),
    isRead: false,
    createdAt: new Date(),
  });
}

async function notifyRecipeComment({
  recipeOwnerId,
  commenterId,
  commenterName,
  recipeId,
  recipeTitle,
  commentPreview,
}) {
  if (!recipeOwnerId || recipeOwnerId === commenterId) {
    return;
  }

  const preview = String(commentPreview || '').trim();
  const shortPreview = preview.length > 80 ? `${preview.slice(0, 77)}...` : preview;

  await createCommunityNotification({
    customerId: recipeOwnerId,
    type: 'community_comment',
    title: 'Bình luận mới trên công thức của bạn',
    body: `${commenterName || 'Một người dùng'} đã bình luận về "${recipeTitle || 'công thức của bạn'}": ${shortPreview}`,
    action: 'Xem bình luận',
    targetId: recipeId,
    actorName: commenterName,
    recipeTitle,
  });
}

async function notifyCommentLike({
  commentOwnerId,
  likerId,
  likerName,
  recipeId,
  recipeTitle,
}) {
  if (!commentOwnerId || commentOwnerId === likerId) {
    return;
  }

  await createCommunityNotification({
    customerId: commentOwnerId,
    type: 'community_comment_like',
    title: 'Có người thích bình luận của bạn',
    body: `${likerName || 'Một người dùng'} đã thích bình luận của bạn trong "${recipeTitle || 'công thức'}".`,
    action: 'Xem bài viết',
    targetId: recipeId,
    actorName: likerName,
    recipeTitle,
  });
}

async function notifyNewFollower({
  followedCustomerId,
  followerId,
  followerName,
}) {
  if (!followedCustomerId || followedCustomerId === followerId) {
    return;
  }

  await createCommunityNotification({
    customerId: followedCustomerId,
    type: 'community_follow',
    title: 'Người theo dõi mới',
    body: `${followerName || 'Một người dùng'} đã bắt đầu theo dõi bạn.`,
    action: 'Xem hồ sơ',
    targetId: followerId,
    actorName: followerName,
  });
}

module.exports = {
  createCommunityNotification,
  notifyRecipeComment,
  notifyCommentLike,
  notifyNewFollower,
};
