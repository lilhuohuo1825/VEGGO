const express = require('express');
const cors = require('cors');
const http = require('http');
const path = require('path');
require('dotenv').config();
const { Server } = require('socket.io');

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
const promotionTargetRoutes = require('./routes/promotionTargetRoutes');
const promotionUsageRoutes = require('./routes/promotionUsageRoutes');
const warehouseRoutes = require('./routes/warehouseRoutes');
const certificateRoutes = require('./routes/certificateRoutes');
const notificationRoutes = require('./routes/notificationRoutes');
const { migrateUserAvatarField } = require('./utils/userAvatarMigration');
const { migrateUserDateFields } = require('./utils/userDateFieldMigration');
const { seedConsultationsIfEmpty } = require('./utils/consultationSeed');
const { processScheduledDeliveryReminders } = require('./services/scheduledDeliveryReminderService');
const paymentRoutes = require('./routes/paymentRoutes');
const chatRoutes = require('./routes/chatRoutes');
const supportRoutes = require('./routes/supportRoutes');
const categoryRoutes = require('./routes/categoryRoutes');
const forecastRoutes = require('./routes/forecastRoutes');
const walletRoutes = require('./routes/walletRoutes');
const recurringOrderRoutes = require('./routes/recurringOrderRoutes');
const mongoose = require('mongoose');
const { verifyAccessToken } = require('./utils/jwt');
const { createSupportSocket } = require('./sockets/supportSocket');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: process.env.SOCKET_IO_CORS_ORIGIN ? process.env.SOCKET_IO_CORS_ORIGIN.split(',') : '*',
    methods: ['GET', 'POST'],
    credentials: true,
  },
});

io.use((socket, next) => {
  try {
    const token =
      (socket.handshake && socket.handshake.auth && socket.handshake.auth.token) ||
      (socket.handshake && socket.handshake.headers && socket.handshake.headers.authorization
        ? String(socket.handshake.headers.authorization).replace(/^Bearer\s+/i, '')
        : null);

    if (!token) return next(new Error('Unauthorized'));
    const decoded = verifyAccessToken(token);
    socket.data.auth = decoded;
    return next();
  } catch (err) {
    return next(new Error('Unauthorized'));
  }
});

createSupportSocket(io);

app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));
app.use('/uploads/avatars', express.static(path.join(__dirname, '..', 'uploads', 'avatars')));
app.use('/uploads', express.static(path.join(__dirname, '..', 'uploads')));
app.use('/admin', express.static(path.join(__dirname, 'public-admin')));

app.use('/api', (req, res, next) => {
  if (req.method === 'GET') {
    res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate, proxy-revalidate');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');
  }
  next();
});

const toImageBuffer = (data) => {
  if (!data) return null;
  if (Buffer.isBuffer(data)) return data;
  if (data instanceof Uint8Array) return Buffer.from(data);

  if (typeof data.value === 'function') {
    const value = data.value();
    if (Buffer.isBuffer(value)) return value;
    if (value instanceof Uint8Array) return Buffer.from(value);
    if (typeof value === 'string') return Buffer.from(value, 'binary');
  }

  if (data.buffer) {
    const byteOffset = Number(data.byteOffset || 0) || 0;
    const byteLength = Number(data.byteLength || data.length || data.position || 0) || 0;

    if (data.buffer instanceof ArrayBuffer) {
      return byteLength > 0
        ? Buffer.from(data.buffer, byteOffset, byteLength)
        : Buffer.from(data.buffer);
    }

    if (Buffer.isBuffer(data.buffer) || data.buffer instanceof Uint8Array) {
      return byteLength > 0
        ? Buffer.from(data.buffer.subarray(byteOffset, byteOffset + byteLength))
        : Buffer.from(data.buffer);
    }
  }

  return null;
};

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
app.use('/api/promotion-targets', promotionTargetRoutes);
app.use('/api/promotion-usages', promotionUsageRoutes);
app.use('/api/blogs', blogRoutes);
app.use('/api/warehouses', warehouseRoutes);
app.use('/api/certificates', certificateRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/payment', paymentRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/support', supportRoutes);
app.use('/api/categories', categoryRoutes);
app.use('/api/forecast', forecastRoutes);
app.use('/api/wallet', walletRoutes);
app.use('/api/recurring-orders', recurringOrderRoutes);

// GET /api/promo-images/:token - lấy ảnh banner khuyến mãi theo token ngắn
app.get('/api/promo-images/:token', async (req, res) => {
  try {
    const token = String(req.params.token || '').trim();
    if (!token) return res.status(400).json({ success: false, message: 'Missing token' });

    const doc = await mongoose.connection.db.collection('promoBannerImages').findOne({ token });
    if (!doc || !doc.data) return res.status(404).json({ success: false, message: 'Image not found' });

    const buffer = toImageBuffer(doc.data);
    if (!Buffer.isBuffer(buffer)) return res.status(500).json({ success: false, message: 'Invalid image buffer' });

    res.setHeader('Content-Type', doc.mimeType || 'image/jpeg');
    res.setHeader('Cache-Control', 'public, max-age=86400');
    res.end(buffer);
  } catch (err) {
    console.error('[GET /api/promo-images/:token] Error:', err);
    res.status(500).json({ success: false, message: 'Server error' });
  }
});

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
  .then(async () => {
    if (typeof blogRoutes.ensureBlogLikeFields === 'function') {
      await blogRoutes.ensureBlogLikeFields();
    }

    // Seed default warehouses if empty
    const db = mongoose.connection.db;
    await migrateUserAvatarField(db);
    await migrateUserDateFields(db);

    const warehouseCount = await db.collection('warehouses').countDocuments();
    if (warehouseCount === 0) {
      console.log('Seeding default VEGGO warehouses...');
      await db.collection('warehouses').insertMany([
        {
          name: 'Kho trung tâm Hà Nội',
          code: 'WH-HN-01',
          address: '12 Tràng Thi, Hàng Trống, Hoàn Kiếm, Hà Nội',
          location: { lat: 21.0285, lng: 105.8542 },
          isActive: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        {
          name: 'Kho Đà Nẵng',
          code: 'WH-DN-01',
          address: '45 Lê Duẩn, Hải Châu, Đà Nẵng',
          location: { lat: 16.0544, lng: 108.2022 },
          isActive: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        },
        {
          name: 'Kho trung tâm TP. Hồ Chí Minh',
          code: 'WH-SG-01',
          address: '78 Nguyễn Huệ, Bến Nghé, Quận 1, Hồ Chí Minh',
          location: { lat: 10.7769, lng: 106.7009 },
          isActive: true,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        }
      ]);
    }

    await seedConsultationsIfEmpty();

    const runDeliveryReminders = async () => {
      try {
        const created = await processScheduledDeliveryReminders(mongoose.connection.db);
        if (created > 0) {
          console.log(`[delivery-reminder] Created ${created} admin reminder(s)`);
        }
      } catch (error) {
        console.error('[delivery-reminder] Failed:', error.message);
      }
    };
    await runDeliveryReminders();
    setInterval(runDeliveryReminders, 5 * 60 * 1000);

    server.listen(port, '0.0.0.0', () => {
      console.log(`VEGGO API running on http://0.0.0.0:${port}`);
    });
  })
  .catch((error) => {
    console.error('Failed to start server:', error.message);
    process.exit(1);
  });

// Trigger reload 3
