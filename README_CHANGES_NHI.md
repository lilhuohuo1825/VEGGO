# README_CHANGES_NHI

Tài liệu tổng hợp toàn bộ thay đổi trên nhánh **`feature/product`** (Nhi).

| Thông tin | Giá trị |
|-----------|---------|
| Nhánh | `feature/product` |
| Commit mới nhất | `41534cc` — Fix loi show data ben category |
| Trạng thái | ~117 file WIP chưa commit |


# PHẦN B — Nhánh `feature/product`

## B.1 Commits đã push/local

| Commit | Message | Files |
|--------|---------|-------|
| `41534cc` | Fix loi show data ben category | `MainActivity.java`, `CategoryAdapter.java`, `CategoryDetailFragment.java` |

- Sửa lỗi hiển thị dữ liệu danh mục
- Cập nhật `CategoryAdapter` bind/icon
- Điều chỉnh `CategoryDetailFragment` load subcategory/product

---

## B.2 Feature WIP (chưa commit)

### 1. Product Detail — Reviews

**API:**
```
GET /api/reviews/sku/:sku
GET /api/reviews
```

**Luồng:** `ProductDetailActivity` → `ProductViewModel.triggerReviewFetch()` → `ReviewRepository` → MongoDB

**Files:** `Reviews.js`, `reviewRoutes.js`, `ReviewApi.java`, `ReviewDto.java`, `ProductReviewsDto.java`, `ReviewRepositoryImpl.java`, `ReviewMapper.java`, `ReviewDetailActivity.java`, `ReviewAdapter.java`

---

### 2. Product Detail — Consultations (Hỏi đáp)

**API:**
```
GET  /api/consultations/:sku
POST /api/consultations/:sku/questions
     Body: { question, customerName?, productName? }
```

**Files:** `Consultation.js`, `consultationRoutes.js`, `ConsultationApi.java`, `ConsultationDto.java`, `ConsultationAskRequest.java`, `ConsultationRepositoryImpl.java`, `ConsultationMapper.java`, `ConsultationDetailActivity.java`, `ConsultationAdapter.java`, `ApiHttpException.java`

---

### 3. Product Detail — Related Recipes (Công thức liên quan)

Gợi ý công thức từ **tên sản phẩm**, match `dishes.ingredients`, group theo `instructionId`.

**API:**
```
GET /api/recipes/related?productName=...
GET /api/recipes/:instructionId
```

**Luồng:**
```text
ProductDetailActivity → triggerRelatedRecipesFetch(product.name)
  → RecipeRepository → card (CookingTime)
  → tap → CommunityRecipeDetailActivity (EXTRA_INSTRUCTION_ID)
```

**Files:** `Instruction.js`, `Dish.js`, `recipeKeywords.js`, `recipeRoutes.js`, `RecipeApi.java`, `RelatedRecipeDto.java`, `RecipeDetailDto.java`, `RecipeRepositoryImpl.java`, `CommunityRecipeDetailActivity.java`, `RecipeAdapter.java`, `item_recipe.xml`

*(Chi tiết đầy đủ ở mục C bên dưới)*

---

### 4. Profile — Address Book

**API:**
```
GET    /api/addresses/user/:userId
POST   /api/addresses
PUT    /api/addresses/:id
DELETE /api/addresses/:id
PATCH  /api/addresses/:id/default
GET    /api/tree_complete
```

**Files:** `SavedAddress.js`, `addressRoutes.js`, `treeCompleteRoutes.js`, `AddressApi.java`, `AddressDto.java`, `AddressRequestDto.java`, `AddressMapper.java`, `AddressRepositoryImpl.java`, `AddressBookActivity.java`, `AddressFormActivity.java`, `AddressBookViewModel.java`, `AddressFormViewModel.java`, `AddressTreeLoader.java`, `VietnamAddressTree.java`

---

### 5. Profile — Personal Info + Avatar

**API:**
```
PUT /api/users/profile
Header: X-Current-Phone
Body: multipart (name, phone, email, avatar)
```

**Files:** `userRoutes.js`, `avatarStorage.js`, `profileValidation.js`, `UserProfileDto.java`, `UserApi.java`, `UserRepositoryImpl.java`, `PersonalInfoActivity.java`, `PersonalInfoViewModel.java`, `ImageCompressor.java`, `ProfileLoggedInFragment.java`

---

### 6. Global Search

Tìm sản phẩm + feature shortcuts (Tủ lạnh, Trợ lý AI, …).

**Files:** `SearchActivity.java`, `SearchViewModel.java`, `GlobalSearchAdapter.java`, `ProductDao.java`, `activity_search.xml`, `layout_search_bar.xml`, `item_search_*.xml`

---

### 7. Category & Product listing

**Files:** `CategoryApi.java`, `CategoryRepositoryImpl.java`, `CategoryViewModel.java`, `CategoryDetailFragment.java`, `HomeFragment.java`, `ProductAdapter.java`, `RelatedProductAdapter.java`, `FlashSaleAdapter.java`

---

### 8. Auth / Login (session cho API)

**Files:** `LoginActivity.java`, `AppPreferences.java`, `VeggoApplication.java`

---

### 9. Database & DI

**Files:** `VeggoDatabase.java`, `DatabaseManager.java`, `ViewModelFactory.java`, `AppModule.java`, `backend/server.js`, `backend/.env.example`

---

## B.3 Backend routes (WIP)

| Prefix | Mô tả |
|--------|--------|
| `/api/reviews` | Reviews theo SKU |
| `/api/consultations` | Hỏi đáp sản phẩm |
| `/api/recipes` | Công thức liên quan + chi tiết |
| `/api/addresses` | Sổ địa chỉ |
| `/api/tree_complete` | Cây Tỉnh/Huyện/Xã VN |
| `/api/users/profile` | Cập nhật profile + avatar |

---

## B.4 Android screens (WIP)

| Màn hình | Feature |
|----------|---------|
| `ProductDetailActivity` | Reviews, Consultations, Related Recipes, Related Products |
| `ReviewDetailActivity` | Toàn bộ reviews |
| `ConsultationDetailActivity` | Toàn bộ Q&A |
| `CommunityRecipeDetailActivity` | Chi tiết công thức (API) |
| `SearchActivity` | Global search |
| `AddressBookActivity` / `AddressFormActivity` | CRUD địa chỉ |
| `PersonalInfoActivity` | Profile + avatar |
| `ProfileLoggedInFragment` | User đã login |
| `CategoryDetailFragment` | Danh mục + sản phẩm |
| `HomeFragment` | Home sections |

---

# PHẦN C — Related Recipes (chi tiết)

## C.1 Data model MongoDB

### `instructions` (overview — chỉ hiển thị)

| Field | Mô tả |
|-------|--------|
| `_id`, `ID` | ObjectId / legacy M001… |
| `DishName` / `title` | Tên công thức |
| `Image`, `Description` | Ảnh, mô tả |
| `CookingTime`, `Difficulty`, `Servings` | Meta |

### `dishes` (match chính)

| Field | Mô tả |
|-------|--------|
| `Ingredients` / `ingredients` | **Match chính** (+3 điểm) |
| `dishName` / `DishName` | Match phụ (+1) |
| `instructionId` / `ID` | Link instructions |
| `Preparation`, `Cooking`, `Serving` | Ghép thành `steps` |

Seed: `app/src/main/assets/instructions.json`, `dishes.json` → `node import-mongo.js`

---

## C.2 Matching logic

**Input:** product name only (vd: `gạo lứt ST25 lúa tôm 1kg`)

1. Extract keywords — lowercase, bỏ packaging, stop words, chuẩn hóa dấu VN
2. Match `dishes.ingredients` (+3) và tên món (+1)
3. Group theo `instructionId`
4. Rank `matchScore` DESC, top 10
5. Không match → ẩn section

---

## C.3 API Recipes

### GET `/api/recipes/related?productName=...`

```json
[
  {
    "instructionId": "...",
    "title": "Cơm gạo lứt hạt sen",
    "image": "https://...",
    "description": "...",
    "cookingTime": "30 Phút",
    "matchScore": 0.5
  }
]
```

### GET `/api/recipes/:instructionId`

```json
{
  "instruction": { "title", "image", "description", "cookingTime", "difficulty", "servings", "video" },
  "dishes": [{ "dishName", "ingredients": [], "steps": "..." }]
}
```

---

## C.4 UI Recipes

- Section **"Công thức liên quan"** trên Product Detail (`llRelatedRecipes`, `rvRecipes`)
- Card: ảnh, tên, **Thời gian: 30 Phút** (`item_recipe.xml`, `recipe_cooking_time_format`)
- Tap → `CommunityRecipeDetailActivity` + `EXTRA_INSTRUCTION_ID`

### Domain mapping

| API | `Recipe` | UI |
|-----|----------|-----|
| `instructionId` | `id` | Intent |
| `title` | `name` | `tvRecipeName` |
| `image` | `imageUrl` | `ivRecipeImage` |
| `cookingTime` | `cookingTime` | `tvCookingTime` |

---

## C.5 Bug fixes (Recipes)

| Vấn đề | Fix |
|--------|-----|
| `Callback` ambiguous compile error | Dùng `retrofit2.Callback` trong `RecipeRepositoryImpl` |
| Card hiện description thay cooking time | API trả `cookingTime`, adapter format string |
| `tvRecipePrice` không tồn tại | Xóa reference trong `RecipeAdapter` |

---

# PHẦN D — Chạy & test

```bash
cd backend
npm install
npm run dev
node import-mongo.js
```

**Checklist:**

- [ ] Auth: login, session, profile
- [ ] Product Detail: reviews theo SKU
- [ ] Product Detail: gửi câu hỏi consultation
- [ ] Product Detail: công thức liên quan + tap chi tiết
- [ ] Search: tìm sản phẩm
- [ ] Sổ địa chỉ: CRUD + mặc định
- [ ] Thông tin cá nhân: sửa + upload avatar
- [ ] Category: hiển thị đúng (`41534cc`)

**Test Recipes:**
```bash
curl "http://localhost:5001/api/recipes/related?productName=g%E1%BA%A1o%20l%E1%BB%A9t%201kg"
curl "http://localhost:5001/api/recipes/{instructionId}"
```

---

*Cập nhật: README_CHANGES_NHI — gộp Auth/Checkout/Profile + feature/product WIP + Related Recipes.*
