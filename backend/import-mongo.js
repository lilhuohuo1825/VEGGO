const mongoose = require('mongoose');
const fs = require('fs');
const path = require('path');
require('dotenv').config();

const ASSETS_PATH = path.join(__dirname, '../app/src/main/assets');

async function run() {
  try {
    // Kết nối tới MongoDB Atlas bằng link trong file .env
    await mongoose.connect(process.env.MONGODB_URI);
    console.log("✅ Đã kết nối thành công tới MongoDB Atlas!");

    // Lấy danh sách tất cả các file .json trong thư mục assets (bỏ qua các file ẩn có dấu chấm ở đầu)
    const files = fs.readdirSync(ASSETS_PATH).filter(f => f.endsWith('.json') && !f.startsWith('.'));

    for (const file of files) {
      // Bỏ đuôi .json để lấy tên bảng (VD: products.json -> products)
      const collectionName = file.replace('.json', '');
      const filePath = path.join(ASSETS_PATH, file);
      
      const dataStr = fs.readFileSync(filePath, 'utf8');
      
      // Xử lý loại bỏ các chuỗi $oid của MongoDB Export
      const cleanDataStr = dataStr.replace(/\{\s*"\$oid"\s*:\s*"([^"]+)"\s*\}/g, '"$1"');
      const items = JSON.parse(cleanDataStr);
      
      if (items && items.length > 0) {
        console.log(`⏳ Đang chèn ${items.length} dòng vào bảng '${collectionName}'...`);
        
        for (let item of items) {
            // Xóa _id cũ để MongoDB tự tạo _id chuẩn mới, tránh lỗi
            delete item._id;
        }
        
        // Trỏ trực tiếp vào Collection và chèn dữ liệu
        const collection = mongoose.connection.db.collection(collectionName);
        
        // (Tuỳ chọn) Nếu bạn muốn xoá sạch data cũ trước khi chèn data mới thì mở comment dòng dưới:
        // await collection.deleteMany({});
        
        await collection.insertMany(items);
        console.log(`👉 Xong bảng '${collectionName}'`);
      }
    }
    
    console.log("\n🎉 HOÀN TẤT! Đã bơm toàn bộ Data mẫu vào MongoDB thành công!");
  } catch (err) {
    console.error("❌ Có lỗi xảy ra:", err);
  } finally {
    // Đóng kết nối
    await mongoose.connection.close();
    process.exit(0);
  }
}

run();
