import { initializeApp } from "firebase/app";
import { getFirestore, collection, doc, setDoc, writeBatch } from "firebase/firestore";
import fs from "fs";

// Cấu hình Firebase
const firebaseConfig = {
    apiKey: 'AIzaSyBeHvvkoZ38RqjjHIcv7Hwf5GhfknB4FFA',
    authDomain: 'veggo-123.firebaseapp.com',
    projectId: 'veggo-123',
    storageBucket: 'veggo-123.firebasestorage.app',
    messagingSenderId: '435688023132',
    appId: '1:435688023132:web:70fc8cbd13ad6044b9b299'
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

const ASSETS_PATH = "../app/src/main/assets";

async function uploadCollection(collectionName, fileName, idField = "id") {
    console.log(`Bắt đầu upload ${collectionName} từ ${fileName}...`);
    try {
        const dataStr = fs.readFileSync(`${ASSETS_PATH}/${fileName}`, "utf8");
        const items = JSON.parse(dataStr);
        
        console.log(`Tìm thấy ${items.length} items. Đang đẩy lên Firestore...`);
        
        // Firestore batch giới hạn 500 operation mỗi lần
        const BATCH_SIZE = 450;
        let batchCount = 0;
        let batch = writeBatch(db);
        let count = 0;

        for (const item of items) {
            // Mặc định thêm thuộc tính isActive = true cho các bảng để phục vụ xoá mềm
            if (item.isActive === undefined) {
                item.isActive = true;
            }

            const docId = item[idField] ? String(item[idField]) : doc(collection(db, collectionName)).id;
            const docRef = doc(db, collectionName, docId);
            
            batch.set(docRef, item);
            count++;

            if (count === BATCH_SIZE) {
                await batch.commit();
                batchCount++;
                console.log(`Đã đẩy batch ${batchCount} (${batchCount * BATCH_SIZE} items)`);
                batch = writeBatch(db);
                count = 0;
            }
        }

        if (count > 0) {
            await batch.commit();
            console.log(`Đã đẩy batch cuối cùng. Tổng cộng: ${items.length} items.`);
        }
        
        console.log(`✅ Upload thành công: ${collectionName}\n`);
    } catch (e) {
        console.error(`❌ Lỗi khi upload ${collectionName}:`, e);
    }
}

async function run() {
    // 1. Chạy upload Categories trước
    await uploadCollection("categories", "categories.json");
    
    // 2. Chạy upload Products
    // Vì file products.json khá lớn, ta sẽ ưu tiên upload file này
    await uploadCollection("products", "products.json");

    // Dừng tiến trình
    console.log("Hoàn tất mọi thao tác!");
    process.exit(0);
}

run();
