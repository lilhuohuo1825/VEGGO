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
const reviewRoutes = require('./routes/reviewRoutes');
const consultationRoutes = require('./routes/consultationRoutes');
const addressRoutes = require('./routes/addressRoutes');
const treeCompleteRoutes = require('./routes/treeCompleteRoutes');
const recipeRoutes = require('./routes/recipeRoutes');
const promotionRoutes = require('./routes/promotionRoutes');
const promoImageRoutes = require('./routes/promoImageRoutes');
const fridgeRoutes = require('./routes/fridgeRoutes');
const communityRoutes = require('./routes/communityRoutes');
const blogRoutes = require('./routes/blogRoutes');

const app = express();

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use('/uploads/avatars', express.static(path.join(__dirname, '..', 'uploads', 'avatars')));
app.use('/uploads', express.static(path.join(__dirname, '..', 'uploads')));

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'VEGGO API' });
});

app.use('/api/products', productRoutes);
app.use('/api/cart', cartRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/users', userRoutes);
app.use('/api/reviews', reviewRoutes);
app.use('/api/consultations', consultationRoutes);
app.use('/api/addresses', addressRoutes);
app.use('/api/tree_complete', treeCompleteRoutes);
app.use('/api/recipes', recipeRoutes);
app.use('/api/promotions', promotionRoutes);
app.use('/api/promo-images', promoImageRoutes);
app.use('/api/fridge', fridgeRoutes);
app.use('/api/community', communityRoutes);
app.use('/api/blog', blogRoutes);

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
    if (typeof blogRoutes.ensureBlogLikeFields === 'function') {
      return blogRoutes.ensureBlogLikeFields();
    }
    return null;
  })
  .then(() => {
    app.listen(port, () => {
      console.log(`VEGGO API running on port ${port}`);
    });
  })
  .catch((error) => {
    console.error('Failed to start server:', error.message);
    process.exit(1);
  });
