require('dotenv').config();
const mongoose = require('mongoose');

mongoose.connect(process.env.MONGODB_URI).then(async () => {
  const db = mongoose.connection.db;
  const res = await db.collection('promotions').find({ promotion_targets: { $exists: true } }).toArray();
  console.log(JSON.stringify(res, null, 2));
  process.exit(0);
}).catch(e => {
  console.error(e);
  process.exit(1);
});
