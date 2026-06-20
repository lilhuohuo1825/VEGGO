const express = require('express');
const cors = require('cors');
require('dotenv').config();

const { connectMongo } = require('./config/mongodb');
require('./config/firebaseAdmin');

const productRoutes = require('./routes/productRoutes');
const cartRoutes = require('./routes/cartRoutes');
const orderRoutes = require('./routes/orderRoutes');
const userRoutes = require('./routes/userRoutes');
const promotionRoutes = require('./routes/promotionRoutes');
const promoImageRoutes = require('./routes/promoImageRoutes');
const fridgeRoutes = require('./routes/fridgeRoutes');

const app = express();

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', service: 'VEGGO API' });
});

app.use('/api/products', productRoutes);
app.use('/api/cart', cartRoutes);
app.use('/api/orders', orderRoutes);
app.use('/api/users', userRoutes);
app.use('/api/promotions', promotionRoutes);
app.use('/api/promo-images', promoImageRoutes);
app.use('/api/fridge', fridgeRoutes);

app.use((error, req, res, next) => {
  console.error(error);
  res.status(error.status || 500).json({
    message: error.message || 'Internal server error',
  });
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
