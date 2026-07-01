const express = require('express');
const mongoose = require('mongoose');

const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

const ACCOUNT_ID = 'account-thuc-quyen';
const ACCOUNT_NAME = 'Thuc Quyen';
const ACCOUNT_AVATAR_URL = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=500&q=80';

router.get('/', asyncHandler(async (req, res) => {
  const limit = Number(req.query.limit || 0);
  const viewerId = req.query.viewerId || req.query.customerId || '';
  await ensureBlogLikeFields();
  const cursor = blogsCollection()
    .find({})
    .sort({ publishedAtMillis: -1, pubDate: -1, PubDate: -1, _id: -1 });
  if (Number.isFinite(limit) && limit > 0) {
    cursor.limit(limit);
  }
  const blogs = await cursor.toArray();
  res.json(blogs.map((blog) => normalizeBlog(blog, viewerId)));
}));

router.get('/:blogId/comments', asyncHandler(async (req, res) => {
  const blogId = req.params.blogId;
  const viewerId = req.query.viewerId || req.query.customerId || '';
  if (!blogId) {
    return res.status(400).json({ message: 'blogId is required' });
  }

  const comments = await blogCommentsCollection()
    .find({ BlogID: blogId })
    .sort({ CreatedAt: -1 })
    .toArray();
  const users = await usersByCustomerIds(comments.map((comment) => comment.CustomerID));
  res.json(comments.map((comment) => normalizeBlogComment(comment, users.get(comment.CustomerID), viewerId)));
}));

router.get('/:blogId', asyncHandler(async (req, res) => {
  const blogId = req.params.blogId;
  const viewerId = req.query.viewerId || req.query.customerId || '';
  await ensureBlogLikeFields();
  const blog = await blogsCollection().findOne({
    $or: [
      { id: blogId },
      { BlogID: blogId },
      objectIdQuery(blogId),
    ],
  });
  if (!blog) {
    return res.status(404).json({ message: 'Blog not found' });
  }
  res.json(normalizeBlog(blog, viewerId));
}));

router.post('/:blogId/like', asyncHandler(async (req, res) => {
  const blogId = req.params.blogId;
  const customerId = req.body.customerId || req.body.CustomerID || ACCOUNT_ID;
  if (!blogId || !customerId) {
    return res.status(400).json({ message: 'blogId and customerId are required' });
  }

  const blogs = blogsCollection();
  await ensureBlogLikeFields();
  const blog = await blogs.findOne({
    $or: [
      { id: blogId },
      { BlogID: blogId },
      objectIdQuery(blogId),
    ],
  });
  if (!blog) {
    return res.status(404).json({ message: 'Blog not found' });
  }

  const likedIds = likedCustomerIds(blog);
  const currentlyLiked = likedIds.includes(customerId);
  const nextLikedIds = currentlyLiked
    ? likedIds.filter((id) => id !== customerId)
    : [...likedIds, customerId];

  await blogs.updateOne(
    { _id: blog._id },
    {
      $set: {
        likedCustomerIds: nextLikedIds,
        likeCount: nextLikedIds.length,
        updatedAt: new Date(),
      },
    }
  );

  const updated = await blogs.findOne({ _id: blog._id });
  res.json(normalizeBlog(updated, customerId));
}));

router.post('/comments', asyncHandler(async (req, res) => {
  const blogId = req.body.blogId || req.body.BlogID;
  const customerId = req.body.customerId || req.body.CustomerID || ACCOUNT_ID;
  const content = String(req.body.content || req.body.Content || '').trim();
  if (!blogId || !customerId || !content) {
    return res.status(400).json({ message: 'blogId, customerId and content are required' });
  }

  const user = await userByCustomerId(customerId);
  const now = new Date();
  const comment = {
    CommentID: new mongoose.Types.ObjectId().toHexString(),
    BlogID: blogId,
    CustomerID: customerId,
    UserName: user?.FullName || user?.name || req.body.userName || ACCOUNT_NAME,
    UserImageUrl: user?.avatarUrl || req.body.userImageUrl || ACCOUNT_AVATAR_URL,
    Content: content,
    LikedCustomerIDs: [],
    LikeCount: 0,
    CreatedAt: now,
    UpdatedAt: now,
  };

  await blogCommentsCollection().insertOne(comment);
  res.status(201).json(normalizeBlogComment(comment, user, customerId));
}));

router.post('/comments/:commentId/like', asyncHandler(async (req, res) => {
  const commentId = req.params.commentId;
  const customerId = req.body.customerId || req.body.CustomerID || ACCOUNT_ID;
  if (!commentId || !customerId) {
    return res.status(400).json({ message: 'commentId and customerId are required' });
  }

  const comments = blogCommentsCollection();
  const comment = await comments.findOne({ $or: [{ CommentID: commentId }, objectIdQuery(commentId)] });
  if (!comment) {
    return res.status(404).json({ message: 'Blog comment not found' });
  }

  const likedIds = likedCustomerIds(comment);
  const currentlyLiked = likedIds.includes(customerId);
  const nextLikedIds = currentlyLiked
    ? likedIds.filter((id) => id !== customerId)
    : [...likedIds, customerId];
  const nextLikeCount = nextLikedIds.length;

  await comments.updateOne(
    { _id: comment._id },
    {
      $set: {
        LikedCustomerIDs: nextLikedIds,
        LikeCount: nextLikeCount,
        UpdatedAt: new Date(),
      },
    }
  );

  const updated = await comments.findOne({ _id: comment._id });
  const user = await userByCustomerId(updated.CustomerID);
  res.json(normalizeBlogComment(updated, user, customerId));
}));

function blogCommentsCollection() {
  return mongoose.connection.db.collection('blog_comments');
}

function blogsCollection() {
  return mongoose.connection.db.collection('blogs');
}

async function ensureBlogLikeFields() {
  const blogs = blogsCollection();
  await blogs.updateMany(
    { likeCount: { $exists: false } },
    { $set: { likeCount: 0 } }
  );
  await blogs.updateMany(
    { likedCustomerIds: { $exists: false } },
    { $set: { likedCustomerIds: [] } }
  );
}

function normalizeBlog(blog, viewerId = '') {
  const publishedAt = normalizeDate(blog.pubDate || blog.PubDate || blog.publishedAt || blog.PublishedAt);
  const updatedAt = normalizeDate(blog.updatedAt || blog.UpdatedAt || blog.updated_at || blog.ModifiedAt || blog.modifiedAt || publishedAt);
  const likedIds = likedCustomerIds(blog);
  return {
    id: blog.id || blog.BlogID || String(blog._id || ''),
    _id: String(blog._id || ''),
    imageUrl: blog.img || blog.imageUrl || blog.ImageUrl || '',
    img: blog.img || blog.imageUrl || blog.ImageUrl || '',
    title: blog.title || blog.Title || '',
    excerpt: blog.excerpt || blog.Excerpt || '',
    pubDate: publishedAt,
    publishedAt,
    publishedAtMillis: blog.publishedAtMillis || (publishedAt ? Date.parse(publishedAt) || 0 : 0),
    updatedAt,
    author: blog.author || blog.Author || '',
    categoryTag: blog.categoryTag || blog.CategoryTag || '',
    content: blog.content || blog.Content || '',
    views: blog.views || blog.Views || 0,
    likeCount: blog.LikeCount ?? blog.likeCount ?? likedIds.length,
    likedByCurrentUser: viewerId ? likedIds.includes(viewerId) : false,
  };
}

function normalizeDate(value) {
  if (!value) {
    return '';
  }
  if (value instanceof Date) {
    return value.toISOString();
  }
  if (typeof value === 'object' && value.$date) {
    return normalizeDate(value.$date);
  }
  return String(value);
}

async function userByCustomerId(customerId) {
  if (!customerId) {
    return null;
  }
  return mongoose.connection.db.collection('users').findOne({ CustomerID: customerId });
}

async function usersByCustomerIds(customerIds) {
  const uniqueIds = [...new Set((customerIds || []).filter(Boolean))];
  if (!uniqueIds.length) {
    return new Map();
  }
  const users = await mongoose.connection.db.collection('users')
    .find({ CustomerID: { $in: uniqueIds } })
    .toArray();
  return new Map(users.map((user) => [user.CustomerID, user]));
}

function normalizeBlogComment(comment, user, viewerId = '') {
  const likedIds = likedCustomerIds(comment);
  return {
    id: comment.CommentID || String(comment._id || ''),
    blogId: comment.BlogID || comment.blogId || '',
    customerId: comment.CustomerID || comment.customerId || '',
    userName: user?.FullName || user?.name || comment.UserName || '',
    userImageUrl: user?.avatarUrl || comment.UserImageUrl || '',
    content: comment.Content || comment.content || '',
    likeCount: comment.LikeCount ?? comment.likeCount ?? likedIds.length,
    likedByCurrentUser: viewerId ? likedIds.includes(viewerId) : false,
    createdAt: comment.CreatedAt || comment.createdAt || '',
  };
}

function likedCustomerIds(comment) {
  if (Array.isArray(comment.LikedCustomerIDs)) {
    return [...new Set(comment.LikedCustomerIDs.filter(Boolean))];
  }
  if (Array.isArray(comment.CommentLikedCustomerIDs)) {
    return [...new Set(comment.CommentLikedCustomerIDs.filter(Boolean))];
  }
  if (Array.isArray(comment.likedCustomerIds)) {
    return [...new Set(comment.likedCustomerIds.filter(Boolean))];
  }
  return [];
}

function objectIdQuery(id) {
  if (!mongoose.Types.ObjectId.isValid(id)) {
    return { _id: null };
  }
  return { _id: new mongoose.Types.ObjectId(id) };
}

router.ensureBlogLikeFields = ensureBlogLikeFields;

module.exports = router;
