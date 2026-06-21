const express = require('express');
const mongoose = require('mongoose');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

const COLLECTION_CANDIDATES = ['tree-completes', 'tree_complete', 'treecomplete'];

async function loadTreeDocuments() {
  const db = mongoose.connection.db;
  for (const name of COLLECTION_CANDIDATES) {
    const collections = await db.listCollections({ name }).toArray();
    if (collections.length === 0) {
      continue;
    }
    const documents = await db.collection(name).find({}).toArray();
    if (documents.length > 0) {
      return documents;
    }
  }
  return [];
}

router.get('/', asyncHandler(async (req, res) => {
  const documents = await loadTreeDocuments();

  if (!documents || documents.length === 0) {
    return res.status(404).json({ message: 'tree_complete data not found' });
  }

  res.json(documents);
}));

module.exports = router;
