require('dotenv').config();
const mongoose = require('mongoose');

async function run() {
  await mongoose.connect(process.env.MONGODB_URI);
  const rawOrder = await mongoose.connection.collection('orders').findOne({});
  console.log('Raw order keys:', Object.keys(rawOrder));
  console.log('Raw order fields related to ID:', {
    CustomerID: rawOrder.CustomerID,
    userId: rawOrder.userId,
    user_id: rawOrder.user_id,
  });
  console.log('Items array:', rawOrder.items || rawOrder.Items || rawOrder.OrderDetails || 'not found');
  process.exit(0);
}

run();
