const express = require('express');
const mongoose = require('mongoose');
const { GoogleGenAI } = require('@google/genai');
const FridgeItem = require('../models/FridgeItem');
const FridgeLocation = require('../models/FridgeLocation');
const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');
const { AI_CONFIG } = require('../config/aiConfig');

// Initialize Gemini Client
const ai = new GoogleGenAI({ apiKey: process.env.GEMINI_API_KEY });
const router = express.Router();

// Lấy danh sách nguyên liệu của user
router.get('/:userId', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const items = await FridgeItem.find({ userId }).sort({ expiryDate: 1 });
  res.json(items);
}));

// Nhận diện nguyên liệu bằng AI (Gemini Vision)
router.post('/ai/recognize', asyncHandler(async (req, res) => {
  const { imageBase64 } = req.body;
  if (!imageBase64) {
    return res.status(400).json({ message: 'imageBase64 is required' });
  }

  try {
    const prompt = `Bạn là hệ thống nhận diện thực phẩm/nguyên liệu thông minh. Hãy phân tích ảnh này (có thể là hóa đơn, bao bì sản phẩm, ảnh thực phẩm thực tế, hoặc ảnh cửa hàng) và trích xuất TOÀN BỘ các mặt hàng thực phẩm/đồ uống.

Nhận diện TẤT CẢ các loại bao gồm: rau củ quả, thịt cá hải sản, mì tôm/mì gói, đồ ăn liền, bánh kẹo, sữa và chế phẩm, đồ uống (cà phê, trà, nước ngọt), gia vị, dầu ăn, ngũ cốc, đồ khô, đồ đông lạnh, snack, và mọi loại thực phẩm khác.

Trả về JSON với định dạng CHÍNH XÁC là một MẢNG các object (không có text khác ngoài JSON):
[{"name": "Tên hiển thị chi tiết (vd: Quả dưa leo, Sữa chua Vinamilk 100g)", "searchKeyword": "Danh từ gốc siêu ngắn gọn (chỉ 1-2 từ, không tính từ/trạng thái) để tìm kiếm (vd: Rong biển, Dưa leo, Cà phê)", "generalCategory": "Danh mục chung tiếng Việt để dự phòng (vd: Đồ khô, Rau xanh, Gia vị)", "quantity": số_lượng_hoặc_null, "unit": "đơn vị (cái/gói/kg/l/hộp...)", "purchaseDate": "YYYY-MM-DD nếu có trên hóa đơn, nếu không thì null"}]

Lưu ý: 
- Nếu là hóa đơn: trích xuất tất cả mặt hàng thực phẩm trong đó
- Nếu là ảnh sản phẩm/bao bì: nhận diện sản phẩm đó 
- Nếu là ảnh thực phẩm thực tế: nhận diện loại thực phẩm
- Trả về mảng JSON hợp lệ, không thêm markdown hay text giải thích`;

    // The imageBase64 sent from Android might or might not have a data URI prefix
    let cleanBase64 = imageBase64;
    let mimeType = 'image/jpeg';
    if (imageBase64.startsWith('data:')) {
      const parts = imageBase64.split(';');
      mimeType = parts[0].split(':')[1];
      cleanBase64 = parts[1].split(',')[1];
    }

    const callGeminiWithRetry = async (retries = 3, delay = 2000) => {
      for (let i = 0; i < retries; i++) {
        try {
          return await ai.models.generateContent({
            model: AI_CONFIG.model,
            contents: [
              prompt,
              {
                inlineData: {
                  data: cleanBase64,
                  mimeType: mimeType
                }
              }
            ],
            config: {
              responseMimeType: "application/json",
            }
          });
        } catch (error) {
          if (error.status === 503 && i < retries - 1) {
            console.warn(`Gemini 503 Error. Retrying in ${delay}ms... (Attempt ${i + 1}/${retries})`);
            await new Promise(resolve => setTimeout(resolve, delay));
            delay *= 2; // Exponential backoff
          } else {
            throw error;
          }
        }
      }
    };

    const response = await callGeminiWithRetry();

    const textResponse = response.text;
    const result = JSON.parse(textResponse);

    // Hàm escape regex để tránh lỗi khi keyword chứa ký tự đặc biệt (VD: dấu ngoặc)
    const escapeRegex = (text) => {
      return (text || '').replace(/[-[\]{}()*+?.,\\^$|#\s]/g, '\\$&');
    };

    // Add product search matching for each item
    for (let item of result) {
      if (item.name) {
        const keyword = item.searchKeyword || item.name;

        const safeKeyword = escapeRegex(keyword);
        const safeName = escapeRegex(item.name);

        // Search by keyword, limiting to 4 items as requested
        let matchedProducts = await Product.find({
          $or: [
            { name: { $regex: safeKeyword, $options: 'i' } },
            { product_name: { $regex: safeKeyword, $options: 'i' } },
            { name: { $regex: safeName, $options: 'i' } },
            { product_name: { $regex: safeName, $options: 'i' } }
          ],
          status: 'Active'
        }).lean();

        if (matchedProducts.length === 0 && item.generalCategory) {
          const safeCategory = escapeRegex(item.generalCategory);
          matchedProducts = await Product.find({
            $or: [
              { name: { $regex: safeCategory, $options: 'i' } },
              { product_name: { $regex: safeCategory, $options: 'i' } }
            ],
            status: 'Active'
          }).lean();
        }

        // Cải thiện độ chính xác: Ưu tiên tên trùng khớp hoàn toàn, sau đó ưu tiên tên ngắn hơn (gần với từ khóa nhất)
        const searchLower = keyword.toLowerCase();
        matchedProducts.sort((a, b) => {
          const nameA = (a.name || a.product_name || '').toLowerCase();
          const nameB = (b.name || b.product_name || '').toLowerCase();

          if (nameA === searchLower) return -1;
          if (nameB === searchLower) return 1;

          return nameA.length - nameB.length;
        });

        // Nếu món đầu tiên khớp chính xác 100% với từ khóa thì lấy duy nhất món đó để bật popup
        if (matchedProducts.length > 0) {
          const topName = (matchedProducts[0].name || matchedProducts[0].product_name || '').toLowerCase();
          if (topName === searchLower) {
            matchedProducts = [matchedProducts[0]];
          } else {
            matchedProducts = matchedProducts.slice(0, 10);
          }
        }

        item.matchedProducts = matchedProducts.map(p => {
          let imageUrl = p.imageUrl;
          if (!imageUrl && p.image) {
            imageUrl = Array.isArray(p.image) ? p.image[0] : p.image;
          }
          return {
            _id: String(p._id),
            name: p.name || p.product_name || '',
            price: p.price || 0,
            originalPrice: p.originalPrice || p.base_price || p.price || 0,
            imageUrl: imageUrl || '',
            unit: p.unit || '',
            sku: p.sku || ''
          };
        });
      } else {
        item.matchedProducts = [];
      }
    }

    res.json(result);
  } catch (error) {
    console.error("Gemini AI Error:", error);
    res.status(500).json({ message: 'Lỗi nhận diện AI', error: error.message });
  }
}));

// Thêm một nguyên liệu mới
router.post('/:userId', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const { name, quantity, purchaseDate, expiryDate, source, sku, orderId, image, images, unit, locationCode } = req.body;

  if (!name || quantity === undefined || !purchaseDate || !expiryDate || !source) {
    return res.status(400).json({ message: 'Missing required fields' });
  }

  const cleanUserId = userId && userId !== 'null' ? userId : '';

  if (orderId && sku) {
    const existing = await FridgeItem.findOne({ userId: cleanUserId, orderId, sku });
    if (existing) {
      return res.status(409).json({ message: 'Item from this order already added to fridge' });
    }
  }

  const newItem = new FridgeItem({
    userId: cleanUserId,
    name,
    quantity,
    purchaseDate,
    expiryDate,
    source,
    sku: sku || '',
    orderId: orderId || '',
    image: image || '',
    images: images || [],
    unit: unit || '',
    locationCode: locationCode || '',
  });

  const savedItem = await newItem.save();
  res.status(201).json(savedItem);
}));

// Thêm hàng loạt nguyên liệu từ lịch sử mua hàng
router.post('/:userId/batch', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const { items } = req.body;

  if (!items || !Array.isArray(items) || items.length === 0) {
    return res.status(400).json({ message: 'Items array is required' });
  }

  const cleanUserId = userId && userId !== 'null' ? userId : '';

  const newFridgeItems = [];
  for (const item of items) {
    if (item.orderId && item.sku) {
      const existing = await FridgeItem.findOne({ userId: cleanUserId, orderId: item.orderId, sku: item.sku });
      if (existing) {
        continue; // Bỏ qua item đã tồn tại trong tủ lạnh
      }
    }
    newFridgeItems.push({
      userId: cleanUserId,
      name: item.name,
      quantity: item.quantity,
      purchaseDate: item.purchaseDate,
      expiryDate: item.expiryDate,
      source: item.source || 'history',
      sku: item.sku || '',
      orderId: item.orderId || '',
      image: item.image || '',
      images: item.images || [],
      unit: item.unit || '',
      locationCode: item.locationCode || '',
    });
  }

  if (newFridgeItems.length === 0) {
    return res.status(200).json([]); // Không có gì mới để thêm
  }

  const savedItems = await FridgeItem.insertMany(newFridgeItems);
  res.status(201).json(savedItems);
}));

// Cập nhật nguyên liệu
router.put('/:userId/:itemId', asyncHandler(async (req, res) => {
  const { userId, itemId } = req.params;

  if (!mongoose.Types.ObjectId.isValid(itemId)) {
    return res.status(400).json({ message: 'Invalid Item ID' });
  }

  const updatedItem = await FridgeItem.findOneAndUpdate(
    { _id: itemId, userId },
    { $set: req.body },
    { new: true, runValidators: true }
  );

  if (!updatedItem) {
    return res.status(404).json({ message: 'Fridge item not found' });
  }

  res.json(updatedItem);
}));

// Xóa nguyên liệu
router.delete('/:userId/:itemId', asyncHandler(async (req, res) => {
  const { userId, itemId } = req.params;

  if (!mongoose.Types.ObjectId.isValid(itemId)) {
    return res.status(400).json({ message: 'Invalid Item ID' });
  }

  const deletedItem = await FridgeItem.findOneAndDelete({ _id: itemId, userId });

  if (!deletedItem) {
    return res.status(404).json({ message: 'Fridge item not found' });
  }

  res.json({ message: 'Item deleted successfully', itemId: deletedItem._id });
}));

// --- Locations CRUD ---

// Lấy danh sách location của user
router.get('/:userId/locations', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const locations = await FridgeLocation.find({ userId });
  res.json(locations);
}));

// Thêm location
router.post('/:userId/locations', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const { locationCode, name } = req.body;

  if (!locationCode || !name) {
    return res.status(400).json({ message: 'locationCode and name are required' });
  }

  const newLocation = new FridgeLocation({ userId, locationCode, name });
  const savedLocation = await newLocation.save();
  res.status(201).json(savedLocation);
}));

module.exports = router;
