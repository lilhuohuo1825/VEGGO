const mongoose = require('mongoose');
require('dotenv').config({ path: '/Users/lilhuohuo/Downloads/App/VEGGO-1/backend/.env' });

const mongoUri = process.env.MONGODB_URI || 'mongodb://localhost:27017/veggo';
console.log('Connecting to MongoDB:', mongoUri);

mongoose.connect(mongoUri)
  .then(async () => {
    console.log('Connected to database.');
    
    // Clear wallets collection
    const Wallet = require('/Users/lilhuohuo/Downloads/App/VEGGO-1/backend/src/models/Wallet');
    const WalletTransaction = require('/Users/lilhuohuo/Downloads/App/VEGGO-1/backend/src/models/WalletTransaction');
    
    await Wallet.deleteMany({});
    console.log('Deleted all VeggoPay wallets.');
    
    await WalletTransaction.deleteMany({});
    console.log('Deleted all VeggoPay transactions.');
    
    console.log('Database reset completed successfully.');
    process.exit(0);
  })
  .catch(err => {
    console.error('Database connection failed:', err);
    process.exit(1);
  });
