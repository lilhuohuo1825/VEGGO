const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// Get all blogs (excluding inactive ones)
router.get('/', asyncHandler(async (req, res) => {
  const blogs = await mongoose.connection.db
    .collection('blogs')
    .find({ status: { $ne: 'Inactive' } })
    .toArray();

  const mappedBlogs = blogs.map(blog => {
    if (blog.pubDate && blog.pubDate.$date) {
      blog.pubDate = blog.pubDate.$date;
    }
    return blog;
  });

  res.json(mappedBlogs);
}));

// Get blog by Specific ID
router.get('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { blog_id: req.params.id };
  }

  const blog = await mongoose.connection.db.collection('blogs').findOne(query);
  if (!blog) {
    return res.status(404).json({ message: 'Blog not found' });
  }

  if (blog.pubDate && blog.pubDate.$date) {
    blog.pubDate = blog.pubDate.$date;
  }

  res.json(blog);
}));

// Create blog
router.post('/', asyncHandler(async (req, res) => {
  const newBlog = { ...req.body, status: 'Active', createdAt: new Date(), updatedAt: new Date() };
  const result = await mongoose.connection.db.collection('blogs').insertOne(newBlog);
  res.status(201).json({ _id: result.insertedId, ...newBlog });
}));

// Update blog
router.put('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { blog_id: req.params.id };
  }

  const updateData = { ...req.body, updatedAt: new Date() };
  delete updateData._id;

  await mongoose.connection.db.collection('blogs').updateOne(query, { $set: updateData });
  const updatedBlog = await mongoose.connection.db.collection('blogs').findOne(query);
  res.json(updatedBlog);
}));

// Delete blog
router.delete('/:id', asyncHandler(async (req, res) => {
  let query = {};
  if (mongoose.Types.ObjectId.isValid(req.params.id)) {
    query = { _id: new mongoose.Types.ObjectId(req.params.id) };
  } else {
    query = { blog_id: req.params.id };
  }

  await mongoose.connection.db.collection('blogs').updateOne(query, { $set: { status: 'Inactive', updatedAt: new Date() } });
  res.json({ success: true });
}));

module.exports = router;
