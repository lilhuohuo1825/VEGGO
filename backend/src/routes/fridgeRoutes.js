const express = require('express');
const mongoose = require('mongoose');
const { GoogleGenAI } = require('@google/genai');
const FridgeItem = require('../models/FridgeItem');
const FridgeLocation = require('../models/FridgeLocation');
const Product = require('../models/Product');
const asyncHandler = require('../middleware/asyncHandler');
const { VISION_MODEL_FALLBACK_CHAIN } = require('../config/aiConfig');
const {
  ACTIVE_PRODUCT_FILTER,
  escapeRegex,
  formatProductSummary,
} = require('../utils/productHelpers');

const router = express.Router();

const FOOD_RECOGNITION_PROMPT = `Bạn là hệ thống nhận diện thực phẩm/nguyên liệu thông minh. Hãy phân tích ảnh này (có thể là hóa đơn, bao bì sản phẩm, ảnh thực phẩm thực tế, hoặc ảnh cửa hàng) và trích xuất TOÀN BỘ các mặt hàng thực phẩm/đồ uống.

Nhận diện TẤT CẢ các loại bao gồm: rau củ quả, thịt cá hải sản, mì tôm/mì gói, đồ ăn liền, bánh kẹo, sữa và chế phẩm, đồ uống (cà phê, trà, nước ngọt), gia vị, dầu ăn, ngũ cốc, đồ khô, đồ đông lạnh, snack, và mọi loại thực phẩm khác.

Trả về JSON với định dạng CHÍNH XÁC là một MẢNG các object (không có text khác ngoài JSON):
[{"name": "Tên hiển thị chi tiết (vd: Quả dưa leo, Sữa chua Vinamilk 100g)", "searchKeyword": "Danh từ gốc siêu ngắn gọn (chỉ 1-2 từ, không tính từ/trạng thái) để tìm kiếm (vd: Rong biển, Dưa leo, Cà phê)", "generalCategory": "Danh mục chung tiếng Việt để dự phòng (vd: Đồ khô, Rau xanh, Gia vị)", "quantity": số_lượng_hoặc_null, "unit": "đơn vị (cái/gói/kg/l/hộp...)", "purchaseDate": "YYYY-MM-DD nếu có trên hóa đơn, nếu không thì null"}]

Lưu ý:
- Nếu là hóa đơn: trích xuất tất cả mặt hàng thực phẩm trong đó
- Nếu là ảnh sản phẩm/bao bì: nhận diện sản phẩm đó
- Nếu là ảnh thực phẩm thực tế: nhận diện loại thực phẩm
- Trả về mảng JSON hợp lệ, không thêm markdown hay text giải thích`;

function createGeminiClient() {
  const apiKey = process.env.GEMINI_API_KEY?.trim();
  if (!apiKey) {
    const error = new Error('GEMINI_API_KEY chưa được cấu hình');
    error.status = 503;
    throw error;
  }
  return new GoogleGenAI({ apiKey });
}

function parseRecognitionResult(textResponse) {
  const raw = String(textResponse || '').trim();
  if (!raw) {
    return [];
  }

  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed;
    }
    if (parsed && Array.isArray(parsed.items)) {
      return parsed.items;
    }
    if (parsed && typeof parsed === 'object' && parsed.name) {
      return [parsed];
    }
  } catch (_error) {
    const arrayStart = raw.indexOf('[');
    const arrayEnd = raw.lastIndexOf(']');
    if (arrayStart >= 0 && arrayEnd > arrayStart) {
      try {
        const parsed = JSON.parse(raw.slice(arrayStart, arrayEnd + 1));
        if (Array.isArray(parsed)) {
          return parsed;
        }
      } catch (_inner) {
        // fall through
      }
    }
  }

  return [];
}

function shouldTryNextVisionModel(error) {
  const status = error?.status || error?.statusCode;
  const message = String(error?.message || '').toLowerCase();
  return status === 404
    || status === 403
    || message.includes('not found')
    || message.includes('not supported')
    || message.includes('permission_denied')
    || message.includes('denied access')
    || message.includes('leaked');
}

async function recognizeFoodItems(imageBase64) {
  let cleanBase64 = imageBase64;
  let mimeType = 'image/jpeg';
  if (String(imageBase64).startsWith('data:')) {
    const parts = String(imageBase64).split(';');
    mimeType = parts[0].split(':')[1] || mimeType;
    cleanBase64 = parts[1].split(',')[1];
  }

  const ai = createGeminiClient();
  const models = VISION_MODEL_FALLBACK_CHAIN.length
    ? VISION_MODEL_FALLBACK_CHAIN
    : ['gemini-2.5-flash'];

  let lastError = null;

  for (const model of models) {
    for (let attempt = 0; attempt < 3; attempt += 1) {
      try {
        const response = await ai.models.generateContent({
          model,
          contents: [
            FOOD_RECOGNITION_PROMPT,
            {
              inlineData: {
                data: cleanBase64,
                mimeType,
              },
            },
          ],
          config: {
            responseMimeType: 'application/json',
          },
        });

        const result = parseRecognitionResult(response.text);
        if (!result.length) {
          console.warn(`[Scan] Model ${model} trả về mảng rỗng`);
          lastError = new Error('AI không nhận diện được nguyên liệu trong ảnh');
          lastError.status = 422;
          break;
        }

        console.log(`[Scan] Nhận diện thành công bằng ${model}, ${result.length} mục`);
        return result;
      } catch (error) {
        lastError = error;
        if (error.status === 422) {
          break;
        }
        if (shouldTryNextVisionModel(error)) {
          console.warn(`[Scan] Model ${model} không khả dụng, thử model khác...`);
          break;
        }
        if (error.status === 503 && attempt < 2) {
          const delay = 2000 * (attempt + 1);
          console.warn(`[Scan] Gemini 503, retry in ${delay}ms...`);
          await new Promise((resolve) => setTimeout(resolve, delay));
          continue;
        }
        if (error.status === 429 && attempt < 2) {
          const delay = 3000 * (attempt + 1);
          console.warn(`[Scan] Gemini 429, retry in ${delay}ms...`);
          await new Promise((resolve) => setTimeout(resolve, delay));
          continue;
        }
        console.warn(`[Scan] Model ${model} lỗi:`, error.message);
        break;
      }
    }
  }

  if (lastError?.status === 422) {
    throw lastError;
  }

  const wrapped = new Error(lastError?.message || 'Lỗi nhận diện AI');
  wrapped.status = lastError?.status || 500;
  throw wrapped;
}

function mapMatchedProduct(product) {
  const summary = formatProductSummary(product);
  if (!summary) {
    return null;
  }

  return {
    _id: summary.id,
    id: summary.id,
    name: summary.name,
    price: summary.price,
    originalPrice: summary.originalPrice,
    imageUrl: summary.image || '',
    image: summary.image || '',
    unit: summary.unit || '',
    sku: summary.sku || '',
    rating: summary.rating,
    soldCount: summary.soldCount,
    weight: summary.weight || '',
  };
}

async function findMatchedProducts(item) {
  const keyword = item.searchKeyword || item.name;
  const safeKeyword = escapeRegex(keyword);
  const safeName = escapeRegex(item.name);

  const nameFilters = [
    { name: { $regex: safeKeyword, $options: 'i' } },
    { product_name: { $regex: safeKeyword, $options: 'i' } },
    { name: { $regex: safeName, $options: 'i' } },
    { product_name: { $regex: safeName, $options: 'i' } },
    { ingredients: { $regex: safeKeyword, $options: 'i' } },
    { brand: { $regex: safeKeyword, $options: 'i' } },
  ];

  let matchedProducts = await Product.find({
    $and: [
      ACTIVE_PRODUCT_FILTER,
      { $or: nameFilters },
    ],
  }).lean();

  if (matchedProducts.length === 0 && item.generalCategory) {
    const safeCategory = escapeRegex(item.generalCategory);
    matchedProducts = await Product.find({
      $and: [
        ACTIVE_PRODUCT_FILTER,
        {
          $or: [
            { name: { $regex: safeCategory, $options: 'i' } },
            { product_name: { $regex: safeCategory, $options: 'i' } },
          ],
        },
      ],
    }).lean();
  }

  const searchLower = String(keyword || '').toLowerCase();
  matchedProducts.sort((a, b) => {
    const nameA = String(a.name || a.product_name || '').toLowerCase();
    const nameB = String(b.name || b.product_name || '').toLowerCase();

    if (nameA === searchLower) return -1;
    if (nameB === searchLower) return 1;

    return nameA.length - nameB.length;
  });

  if (matchedProducts.length > 0) {
    const topName = String(matchedProducts[0].name || matchedProducts[0].product_name || '').toLowerCase();
    if (topName === searchLower) {
      matchedProducts = [matchedProducts[0]];
    } else {
      matchedProducts = matchedProducts.slice(0, 10);
    }
  }

  return matchedProducts
    .map(mapMatchedProduct)
    .filter(Boolean);
}

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
    const result = await recognizeFoodItems(imageBase64);

    for (const item of result) {
      if (item.name) {
        item.matchedProducts = await findMatchedProducts(item);
      } else {
        item.matchedProducts = [];
      }
    }

    res.json(result);
  } catch (error) {
    console.error('Gemini AI Error:', error);
    const status = error.status || 500;
    res.status(status).json({
      message: error.message || 'Lỗi nhận diện AI',
      error: error.message,
    });
  }
}));

// Thêm một nguyên liệu mới
router.post('/:userId', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const { name, quantity, purchaseDate, expiryDate, source, sku, orderId, image, images, unit, locationCode, remindBeforeExpiry } = req.body;

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
    remindBeforeExpiry: remindBeforeExpiry !== false,
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
        continue;
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
      remindBeforeExpiry: item.remindBeforeExpiry !== false,
    });
  }

  if (newFridgeItems.length === 0) {
    return res.status(200).json([]);
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

router.get('/:userId/locations', asyncHandler(async (req, res) => {
  const { userId } = req.params;
  const locations = await FridgeLocation.find({ userId });
  res.json(locations);
}));

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
