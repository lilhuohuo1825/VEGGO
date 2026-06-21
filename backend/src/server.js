const express = require('express');
const cors = require('cors');
const path = require('path');
require('dotenv').config();

const { connectMongo } = require('./config/mongodb');
require('./config/firebaseAdmin');

const productRoutes = require('./routes/productRoutes');
const cartRoutes = require('./routes/cartRoutes');
const orderRoutes = require('./routes/orderRoutes');
const userRoutes = require('./routes/userRoutes');
// ... existing imports
const reviewRoutes = require('./routes/reviewRoutes');
const consultationRoutes = require('./routes/consultationRoutes');
const addressRoutes = require('./routes/addressRoutes');
const treeCompleteRoutes = require('./routes/treeCompleteRoutes');
const recipeRoutes = require('./routes/recipeRoutes');
console.log('reviewRoutes =', reviewRoutes);
console.log('typeof reviewRoutes =', typeof reviewRoutes);


const app = express();

console.log("productRoutes", typeof productRoutes);
console.log("cartRoutes", typeof cartRoutes);
console.log("orderRoutes", typeof orderRoutes);
console.log("userRoutes", typeof userRoutes);
console.log("reviewRoutes", typeof reviewRoutes);

app.use(cors());
app.use(express.json());
app.use('/uploads/avatars', express.static(path.join(__dirname, '..', 'uploads', 'avatars')));

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'VEGGO API' });
});

app.use('/api/products', productRoutes);
app.use('/api/cart', cartRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/users', userRoutes);
// ... after other app.use calls
app.use('/api/reviews', reviewRoutes);
app.use('/api/consultations', consultationRoutes);
app.use('/api/addresses', addressRoutes);
app.use('/api/tree_complete', treeCompleteRoutes);
app.use('/api/recipes', recipeRoutes);

app.use((error, req, res, next) => {
  console.error(error);
  const status = error.status || 500;
  let message = error.message || 'Internal server error';
  if (error.name === 'ValidationError') {
    message = 'Dữ liệu người dùng không hợp lệ. Vui lòng liên hệ hỗ trợ.';
  }
  res.status(status).json({ message });
});

const port = process.env.PORT || 5001;

connectMongo()
  .then(() => {
    app.listen(port, () => {
      console.log(`VEGGO API running on port ${port}`);
    });
  })
  .catch((error) => {
    console.error('Failed to start server:', error.message);
    process.exit(1);
  });
