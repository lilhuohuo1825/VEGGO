# VEGGO AI Chatbot – Trợ lý AI

Tài liệu mô tả chức năng **Trợ lý AI** trên app VEGGO: tư vấn sức khỏe, gợi ý món ăn, tìm sản phẩm, tính dinh dưỡng và chat chung.

---

## Tổng quan

Chatbot kết nối **Android app** với **backend Node.js**. Backend phân tích câu hỏi, lấy dữ liệu từ MongoDB qua các service domain, rồi trả câu trả lời kèm gợi ý sản phẩm / công thức.

**Nguyên tắc thiết kế:** Gemini **không** truy cập MongoDB trực tiếp. Mọi dữ liệu sản phẩm, recipe, dinh dưỡng đều đi qua service layer.

```
Android (ChatbotActivity)
    → POST /api/chat/message
        → ChatController
        → ChatService (orchestrator)
        → [Rules / Gemini] phân tích intent
        → ProductService | HealthService | RecipeService | NutritionService
        → MongoDB
        → [Template / Gemini] sinh câu trả lời
        → Lưu ChatConversation
    ← JSON (reply + suggestedProducts + suggestedRecipes)
```

---

## Các loại câu hỏi hỗ trợ

| Intent | Ví dụ câu hỏi | Dữ liệu trả về |
|--------|---------------|----------------|
| `HEALTH_ADVICE` | "Bổ sung vitamin C", "Giảm cân nên ăn gì" | Lời khuyên + sản phẩm theo mục tiêu sức khỏe |
| `RECIPE_SUGGESTION` | "Với ớt chuông nấu được món gì", "Nấu gì với giỏ hàng" | Công thức (logic giống Related Recipe ở Product Detail) |
| `PRODUCT_SEARCH` | "cà phê", "gợi ý sản phẩm cà phê", "tìm cà phê" | Sản phẩm khớp từ khóa |
| `NUTRITION_CALCULATION` | "Tính calories bữa ăn", "bao nhiêu calo" | Tổng dinh dưỡng ước tính |
| `GENERAL_CHAT` | "Xin chào", "Cảm ơn" | Lời chào / hướng dẫn sử dụng |

---

## API Backend

### Gửi tin nhắn

```http
POST /api/chat/message
Content-Type: application/json
```

**Request body:**

```json
{
  "customerId": "CUS000001",
  "message": "Với ớt chuông nấu được món gì",
  "conversationId": "optional-mongo-object-id"
}
```

**Response:**

```json
{
  "success": true,
  "data": {
    "conversationId": "...",
    "reply": "🍳 **Món có thể nấu với ớt chuông:**\n• ...",
    "intent": "RECIPE_SUGGESTION",
    "confidence": 0.92,
    "servicesUsed": ["RecipeService", "ProductService"],
    "suggestedProducts": [
      {
        "id": "...",
        "sku": "...",
        "name": "Ớt chuông đỏ Co.op Select",
        "price": 59900,
        "unit": "Kg",
        "image": "...",
        "origin": "..."
      }
    ],
    "suggestedRecipes": [
      {
        "instructionId": "...",
        "title": "Rau củ xào đậu hũ xốt mè",
        "image": "...",
        "cookingTime": "15 Phút",
        "description": "...",
        "matchScore": 0.75
      }
    ],
    "nutritionSummary": null
  }
}
```

### Quản lý hội thoại

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| `GET` | `/api/chat/conversations/:customerId` | Danh sách hội thoại |
| `GET` | `/api/chat/conversations/:customerId/:conversationId` | Chi tiết một hội thoại |
| `DELETE` | `/api/chat/conversations/:customerId/:conversationId` | Xóa hội thoại |

---

## Cấu hình môi trường (`backend/.env`)

```env
# Gemini AI
GEMINI_API_KEY=your-key-here
GEMINI_MODEL=gemini-2.0-flash-lite

# Chat – tối ưu free tier
CHAT_USE_GEMINI_RESPONSE=false   # false = dùng template (0 API call cho câu trả lời)
CHAT_MIN_INTERVAL_MS=4000        # tối thiểu 4 giây giữa 2 tin nhắn / user
CHAT_MAX_REQUESTS_PER_MIN=6        # tối đa 6 tin / phút / user
```

| Biến | Mặc định | Ý nghĩa |
|------|----------|---------|
| `CHAT_USE_GEMINI_RESPONSE` | `false` | `true` = Gemini viết câu trả lời; `false` = template cố định (tiết kiệm quota) |
| `CHAT_MIN_INTERVAL_MS` | `4000` | Rate limit khoảng cách giữa các request |
| `CHAT_MAX_REQUESTS_PER_MIN` | `6` | Rate limit số request / phút |
| `GEMINI_MODEL` | `gemini-2.0-flash-lite` | Model Gemini (tránh `gemini-2.5-flash` nếu project chưa được cấp quyền) |

---

## Kiến trúc Backend

### Luồng xử lý (`ChatService`)

1. Kiểm tra rate limit (`chatRateLimiter.js`)
2. Phân tích intent bằng **rules** trước (`intentRules.js`, confidence ≥ 0.75)
3. Chỉ gọi Gemini phân tích intent khi rules không chắc chắn
4. Gọi service tương ứng lấy context từ MongoDB
5. Sinh câu trả lời bằng **template** (mặc định) hoặc Gemini
6. Lưu `ChatConversation` và trả JSON

### File chính

```
backend/src/
├── config/
│   ├── aiConfig.js          # Intent constants, Gemini config, rate limit
│   └── healthGoals.js       # Mục tiêu sức khỏe + keyword groups
├── controllers/
│   └── chatController.js
├── routes/
│   └── chatRoutes.js        # Mount tại /api/chat
├── services/
│   ├── chatService.js       # Orchestrator
│   ├── productService.js
│   ├── healthService.js
│   ├── recipeService.js
│   ├── nutritionService.js
│   └── ai/
│       ├── geminiService.js
│       └── chatPrompts.js
├── models/
│   └── ChatConversation.js
└── utils/
    ├── intentRules.js           # Rule-based intent (không tốn API)
    ├── chatResponseTemplates.js # Template câu trả lời
    ├── chatRateLimiter.js
    ├── searchQueryExtractor.js  # Tách từ khóa sản phẩm khỏi câu chat
    ├── recipeQueryExtractor.js  # Tách nguyên liệu khỏi câu hỏi thực đơn
    ├── recipeHelpers.js         # rankRelatedRecipes (dùng chung Product Detail)
    └── recipeKeywords.js        # extractKeywords, scoreDishAgainstKeywords
```

---

## Logic nghiệp vụ quan trọng

### Tìm sản phẩm (`PRODUCT_SEARCH`)

- Tách từ khóa thật khỏi câu chat: `"gợi ý sản phẩm cà phê"` → `"cà phê"`
- Câu ngắn như `"cà phê"` được nhận diện đúng intent, không bị coi là chat chung
- Search có fallback: AND → OR → regex phrase

### Tư vấn sức khỏe (`HEALTH_ADVICE`)

- Mỗi mục tiêu có **nhóm từ khóa ưu tiên** (vd. vitamin C: cam quýt → ớt chuông → rau lá xanh)
- Sản phẩm gợi ý **xen kẽ theo nhóm**, khớp với lời khuyên trong text (không chỉ sort theo lượt bán)

### Gợi ý món ăn (`RECIPE_SUGGESTION`)

- Tách nguyên liệu: `"Với ớt chuông nấu được món gì"` → `"ớt chuông"`
- Dùng **`findRelatedRecipesByProductName`** – cùng logic với `GET /api/recipes/related?productName=...` trên Product Detail:
  - `extractKeywords(productName)`
  - `scoreDishAgainstKeywords` (khớp ingredients +3, tên món +1)
  - Sắp xếp theo `matchScore`

---

## Android App

### Màn hình & luồng

```
ChatbotActivity
  → ChatRepository (Retrofit)
  → POST /api/chat/message
  → ChatAdapter (RecyclerView)
      ├── Tin user / bot (bubble)
      ├── Gợi ý món ăn (RecyclerView ngang – RecipeAdapter, item_recipe)
      └── Gợi ý sản phẩm (RecyclerView ngang – ProductAdapter, item_product_grid)
```

**Thứ tự hiển thị sau câu trả lời bot:**

1. Text reply
2. **Recipe cards** (lướt ngang) → bấm mở `InstructionRecipeDetailActivity`
3. **Product cards** (lướt ngang, card giống trang chủ) → bấm mở `ProductDetailActivity`, nút **+** mở thêm giỏ hàng

### File Android chính

```
app/src/main/java/com/veggo/app/
├── presentation/chatbot/
│   └── ChatbotActivity.java
├── adapter/
│   ├── ChatAdapter.java
│   ├── RecipeAdapter.java
│   └── ProductAdapter.java      # horizontalScrollMode cho chat
├── data/
│   ├── remote/api/ChatApi.java
│   ├── remote/dto/
│   │   ├── ChatMessageRequestDto.java
│   │   ├── ChatMessageResponseDto.java
│   │   ├── ChatMessageDataDto.java
│   │   ├── ChatSuggestedProductDto.java
│   │   └── ChatSuggestedRecipeDto.java
│   └── repository/ChatRepository.java
└── domain/model/ChatMessage.java
```

### Layout

| File | Mô tả |
|------|--------|
| `activity_chatbot.xml` | Màn chat chính |
| `item_chat_bot.xml` / `item_chat_user.xml` | Bubble tin nhắn |
| `item_chat_recipe_list.xml` | Khối gợi ý recipe (scroll ngang) |
| `item_chat_product_list.xml` | Khối gợi ý sản phẩm (scroll ngang) |
| `item_recipe.xml` | Card recipe (dùng chung Home / Product Detail) |
| `item_product_grid.xml` | Card sản phẩm trang chủ |

### Cấu hình mạng

- Base URL API: `local.properties` → `api.base.url` (vd. `http://192.168.1.13:5001/api/`)
- Emulator: `10.0.2.2` qua `Constants.java`
- Thiết bị thật: cần `network_security_config.xml` cho IP LAN và `usesCleartextTraffic` ở debug manifest

### Throttle phía client

- Tối thiểu **4 giây** giữa hai lần gửi tin (đồng bộ với backend rate limit)

---

## Chạy thử

### Backend

```bash
cd backend
npm install
npm run dev
```

Server mặc định chạy port **5001** (xem `PORT` trong `.env`).

### Test API nhanh

```bash
curl -X POST http://localhost:5001/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUS000001","message":"Với ớt chuông nấu được món gì"}'
```

### Android

1. Cập nhật `local.properties` với IP máy dev
2. Rebuild app: `./gradlew :app:assembleDebug`
3. Mở **Trợ lý AI** trên app, thử các câu mẫu ở bảng intent phía trên

---

## Quick replies mặc định (UI)

- "Giảm cân nên ăn gì"
- "Nấu gì với giỏ hàng"
- "Tính calories bữa ăn"

---

## Lưu ý vận hành

- **Không commit** `backend/.env` (chứa `GEMINI_API_KEY`)
- Sau khi sửa `.env`, cần **restart backend**
- Sau khi đổi `local.properties` / network config, cần **rebuild Android**
- Key Gemini dạng `AQ.Ab8...` là format hợp lệ (Google AI Studio auth key mới)
- Nếu gặp lỗi **429 quota**: giữ `CHAT_USE_GEMINI_RESPONSE=false`, tăng `CHAT_MIN_INTERVAL_MS` nếu cần
- Nếu gặp lỗi **403 model**: đổi `GEMINI_MODEL=gemini-2.0-flash-lite`

---

## Mở rộng trong tương lai

- Lịch sử hội thoại trên app (API đã có, UI chưa bind)
- Gợi ý theo `tastePreferences` của user
- Hiển thị `nutritionSummary` dạng card trên Android
- Bật `CHAT_USE_GEMINI_RESPONSE=true` khi có quota cao hơn để câu trả lời tự nhiên hơn
