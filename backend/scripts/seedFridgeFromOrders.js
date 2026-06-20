/**
 * Script: seedFridgeFromOrders.js
 * Mục đích: Seed fridge_items từ các orders có status "completed"
 *           TRỪ user CUS000006 (để dùng account đó test thêm thủ công)
 *
 * Chạy: node backend/scripts/seedFridgeFromOrders.js
 */

require('dotenv').config({ path: require('path').resolve(__dirname, '../.env') });

const mongoose = require('mongoose');

// userId của CUS000006 — sẽ bị loại trừ
const EXCLUDE_CUSTOMER_ID = 'CUS000006';

async function run() {
  const mongoUri = process.env.MONGO_URI || process.env.MONGODB_URI;
  if (!mongoUri) {
    console.error('❌ Không tìm thấy MONGO_URI trong .env');
    process.exit(1);
  }

  console.log('🔌 Đang kết nối MongoDB...');
  await mongoose.connect(mongoUri);
  console.log('✅ Đã kết nối MongoDB');

  const db = mongoose.connection.db;

  // Với Order model, userId field là "CustomerID" (dạng CUS000XXX)
  console.log(`ℹ️  Loại trừ user có CustomerID = "${EXCLUDE_CUSTOMER_ID}"`);

  // Query tất cả completed orders (trừ CUS000006)
  const completedOrders = await db.collection('orders').find({
    status: 'completed',
    CustomerID: { $ne: EXCLUDE_CUSTOMER_ID }
  }).toArray();

  console.log(`📦 Tìm thấy ${completedOrders.length} đơn hàng completed (đã loại trừ ${EXCLUDE_CUSTOMER_ID})`);

  if (completedOrders.length === 0) {
    console.log('ℹ️  Không có đơn hàng nào để seed.');
    await mongoose.disconnect();
    return;
  }

  // Lấy OrderID list
  const orderIds = completedOrders.map(o => o.OrderID).filter(Boolean);

  // Tìm order_details tương ứng
  const orderDetails = await db.collection('order_details').find({
    OrderID: { $in: orderIds }
  }).toArray();

  console.log(`📋 Tìm thấy ${orderDetails.length} order_details`);

  // Map OrderID -> order (để lấy CustomerID và createdAt)
  const orderMap = {};
  for (const order of completedOrders) {
    orderMap[order.OrderID] = order;
  }

  const fridgeItemsToInsert = [];

  for (const detail of orderDetails) {
    if (!detail.items || detail.items.length === 0) continue;

    const order = orderMap[detail.OrderID];
    if (!order) continue;

    const purchaseDate = order.createdAt || new Date();
    const expiryDate = new Date(purchaseDate);
    expiryDate.setDate(expiryDate.getDate() + 7);

    const userId = order.CustomerID; // userId trong fridge = CustomerID

    for (const item of detail.items) {
      fridgeItemsToInsert.push({
        userId: userId,
        name: item.productName || 'Nguyên liệu',
        quantity: item.quantity || 1,
        purchaseDate: purchaseDate,
        expiryDate: expiryDate,
        source: 'history',
        sku: item.sku || '',
        image: item.image || '',
        unit: item.unit || 'kg',
        locationCode: '',
        createdAt: new Date(),
        updatedAt: new Date(),
      });
    }
  }

  console.log(`🧺 Chuẩn bị thêm ${fridgeItemsToInsert.length} fridge items...`);

  if (fridgeItemsToInsert.length > 0) {
    const result = await db.collection('fridge_items').insertMany(fridgeItemsToInsert, { ordered: false });
    console.log(`✅ Đã thêm ${result.insertedCount} fridge items thành công!`);
  } else {
    console.log('ℹ️  Không có nguyên liệu nào để thêm (các đơn hàng không có chi tiết sản phẩm).');
  }

  await mongoose.disconnect();
  console.log('🔌 Đã ngắt kết nối MongoDB');
}

run().catch(err => {
  console.error('❌ Lỗi khi seed:', err);
  mongoose.disconnect();
  process.exit(1);
});
