const mongoose = require('mongoose');

async function connectMongo() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    throw new Error('MONGODB_URI is missing. Add it to backend/.env');
  }

  mongoose.set('strictQuery', true);
  try {
    await mongoose.connect(uri, {
      serverSelectionTimeoutMS: 10_000,
      socketTimeoutMS: 45_000,
      maxPoolSize: 20,
    });
  } catch (error) {
    if (error.message && error.message.includes('querySrv ECONNREFUSED')) {
      throw new Error(
        'MongoDB Atlas SRV DNS lookup was refused. Switch MONGODB_URI to the direct mongodb:// host list from backend/.env.example, or use a DNS server/network that allows SRV lookups.'
      );
    }

    throw error;
  }
  console.log('Connected to MongoDB Atlas');
}

module.exports = { connectMongo };
