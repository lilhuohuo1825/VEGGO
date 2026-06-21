# Ghi chú thay đổi code VEGGO

Tài liệu này ghi lại các thay đổi chính đã thêm vào app, luồng màn hình liên quan và API/data dùng cho các chức năng mới. Nội dung tập trung vào các phần Community Cooking, nháp công thức, bình luận, chi tiết công thức và Taste Preferences.

## 1. Taste Preferences

### Mục tiêu

Taste Preferences cho phép người dùng cấu hình khẩu vị và hạn chế ăn uống. User có thể:

- Chọn chế độ ăn.
- Tạo tag cần tránh, ví dụ `Không ăn: trái cây nhiệt đới`, `Dị ứng: đậu phộng`, `Cảnh báo: cà chua`.
- Chọn một trong hai cách app xử lý catalog:
  - `Ẩn khỏi catalog`: product khớp tag chặn sẽ không xuất hiện khi duyệt hàng.
  - `Gắn badge cảnh báo`: product vẫn xuất hiện nhưng có nhãn cảnh báo.
- Xem nguyên liệu cần lưu ý.
- Xem gợi ý sản phẩm thay thế và món ăn phù hợp.

### Luồng màn hình

1. `TastePreferencesActivity`
   - Màn tổng quan `Khẩu vị của tôi`.
   - Hiển thị tổng số tag đang bật và số tag chặn.
   - Hiển thị các chip tag khẩu vị/hạn chế đang bật.
   - Hiển thị danh sách `Nguyên liệu cần lưu ý` dạng card động từ toàn bộ tag đang bật, không hard-code Đậu phộng/Cà chua nữa.
   - Button `Chỉnh sửa` mở màn setup.
   - Button `Xem thực đơn` mở màn gợi ý thực đơn phù hợp.

2. `TasteSetupActivity`
   - Cho user chỉnh chế độ ăn và danh sách tag cần tránh.
   - User chỉ được chọn một logic catalog: ẩn product hoặc gắn badge cảnh báo.
   - Tag bật có màu primary, tag tắt có màu xám.

3. `TasteTagDialog`
   - Dialog thêm/sửa tag cần tránh theo style Veggo.
   - User nhập tên tag, ví dụ `ca cao`, `đậu phộng`.
   - Keyword không cho user nhập thủ công; app tự sinh keyword từ label.

4. `TasteAlertActivity`
   - Hiển thị tất cả nguyên liệu cần lưu ý mà user đã thiết lập.
   - Hiển thị gợi ý thay thế dựa trên tag của user, product và recipe hiện có.
   - Sau khi render, app lưu dữ liệu đã sinh vào Mongo qua `saveGeneratedTasteData`.

5. `TasteMenuSuggestionsActivity`
   - Hiển thị `Sản phẩm thay thế`.
   - Hiển thị `Món không chứa sản phẩm cần tránh`.
   - Product thay thế và recipe dùng chung layout item `item_taste_menu_card.xml` để UI thống nhất.

### Logic xử lý

Logic chính nằm trong:

- `app/src/main/java/com/veggo/app/presentation/profile/TastePreferenceStore.java`

Các hàm quan trọng:

- `tags()`: lấy danh sách tag local.
- `enabledTags()`: lấy tag đang bật.
- `enabledBlockingTags()`: lấy tag đang bật nhưng không phải loại `Cảnh báo`.
- `setHideCatalog(boolean)`: bật chế độ ẩn catalog, đồng thời tắt chế độ badge.
- `setBadgeCatalog(boolean)`: bật chế độ badge, đồng thời tắt chế độ ẩn catalog.
- `matchingBlockingTag(...)`: kiểm tra product/recipe có khớp tag chặn không.
- `shouldHide(Product)`: product có nên bị ẩn khỏi catalog không.
- `productWarning(Product)`: lấy text badge cảnh báo nếu user chọn chế độ badge.
- `alternativesForUserTags(...)`: lấy danh sách product thay thế cho các product bị chặn.
- `safeInstructions(...)`: lấy recipe không chứa nguyên liệu cần tránh.
- `replacementNotes(...)`: sinh text gợi ý thay thế.
- `saveGeneratedTasteData(...)`: lưu `alertIngredients` và `replacementSuggestions` lên Mongo.

### Data lưu ở Mongo

Taste data được lưu trong collection `users`, field:

```json
{
  "tastePreferences": {
    "dietMode": "vegan",
    "catalogBehavior": "hide",
    "suggestMenu": true,
    "tags": [
      {
        "label": "trái cây nhiệt đới",
        "action": "Không ăn",
        "keywords": ["xoài", "dưa hấu", "đu đủ"],
        "enabled": true
      }
    ],
    "alertIngredients": [
      {
        "label": "đậu phộng",
        "action": "Dị ứng",
        "status": "Không phù hợp",
        "description": "..."
      }
    ],
    "replacementSuggestions": [
      "Dùng Chanh không hạt Co.op Select 400g thay cho nguyên liệu cần tránh"
    ]
  }
}
```

### API liên quan

Android khai báo trong:

- `app/src/main/java/com/veggo/app/data/remote/api/UserApi.java`

Backend khai báo trong:

- `backend/src/routes/userRoutes.js`

API:

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| `GET` | `/api/users/{customerId}/taste-preferences` | Lấy taste preferences của user. Nếu user chưa có data thì backend tạo default. |
| `PUT` | `/api/users/{customerId}/taste-preferences` | Lưu taste preferences, tag, cách xử lý catalog, alertIngredients và replacementSuggestions. |

Backend default taste được tạo ở `defaultTastePreferences()` trong `userRoutes.js`.

## 2. Product Catalog và lọc theo Taste

### Mục tiêu

Product catalog cần phản ứng theo Taste Preferences:

- Nếu user chọn `Ẩn khỏi catalog`, product khớp tag chặn sẽ bị loại khỏi danh sách.
- Nếu user chọn `Gắn badge cảnh báo`, product vẫn hiển thị nhưng có badge cảnh báo.
- Danh sách product khi chọn nguyên liệu recipe cần lọc theo trạng thái active.

### Data liên quan

Product lấy từ collection/products API hiện có. Logic khớp tag dùng các trường:

- Tên sản phẩm.
- Mô tả.
- Category.
- Subcategory.
- Nhóm/ingredients nếu có trong asset model.

Khi chọn nguyên liệu cho công thức, danh sách product cần chỉ lấy product đang active theo `isActive` hoặc `status` tùy data backend trả về.

## 3. Community Cooking - tạo/sửa/xóa công thức

### Mục tiêu

Người dùng có thể tạo công thức, chỉnh sửa công thức của mình, xóa công thức của mình và lưu nhiều bản nháp.

### Luồng màn hình

1. `CommunityPostActivity`
   - Màn tạo công thức.
   - User nhập title, category, ảnh/video, nguyên liệu, bước làm, dinh dưỡng.
   - Button lưu nháp gọi API lưu draft.
   - Button xem nháp hiển thị danh sách tất cả draft của user.
   - Mỗi draft có thể được chọn để fill lại form hoặc xóa.
   - Nếu mở từ flow edit recipe thì button submit đổi thành `Lưu thay đổi`, ẩn lưu nháp.

2. `CommunityRecipeDetailActivity`
   - Màn chi tiết công thức.
   - Icon xóa chỉ xuất hiện khi user đang xem công thức do chính mình tạo.
   - Xóa công thức phải có dialog xác nhận.
   - Nút chỉnh sửa mở `CommunityPostActivity` ở chế độ edit.

3. `CommunityIngredientsActivity`
   - Màn xem/check nguyên liệu của recipe.
   - Dữ liệu nguyên liệu lấy từ recipe detail và product liên quan.

### Data Mongo

Community data lưu trong collection:

- `community_cooking`

Các mảng chính liên quan:

- `recipeDrafts`: danh sách bản nháp công thức.
- `recipes`: danh sách công thức đã publish.
- `recipeDetails`: thông tin chi tiết recipe.
- `recipeIngredients`: nguyên liệu của recipe.
- `recipeGalleries`: ảnh/gallery của recipe.
- `recipeComments`: bình luận recipe.

Draft có các field quan trọng:

```json
{
  "DraftID": "DRAFT000001",
  "CustomerID": "CUS000005",
  "Title": "Tên công thức",
  "CategoryID": "CAT001",
  "ImageUrls": [],
  "VideoUrl": "",
  "IngredientsText": "",
  "IngredientItems": [],
  "Steps": "",
  "Calories": 110,
  "SaltLevel": "50",
  "SugarLevel": "150",
  "CreatedAt": "...",
  "UpdatedAt": "..."
}
```

## 4. API Community Cooking

Android khai báo trong:

- `app/src/main/java/com/veggo/app/data/remote/api/CommunityApi.java`

Repository gọi API ở:

- `app/src/main/java/com/veggo/app/presentation/community/CommunityRepository.java`

Backend khai báo trong:

- `backend/src/routes/communityRoutes.js`

### API công thức và nháp

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| `GET` | `/api/community/home` | Lấy dữ liệu trang community home. |
| `GET` | `/api/community/categories` | Lấy category công thức. |
| `GET` | `/api/community/chefs` | Lấy danh sách chef/user community. |
| `GET` | `/api/community/users/{customerId}` | Lấy profile community của user. |
| `GET` | `/api/community/recipes` | Lấy danh sách recipe, có thể lọc `categoryId`, `chefId`, `limit`. |
| `GET` | `/api/community/recipes/{recipeId}/detail?viewerId={customerId}` | Lấy chi tiết recipe, nguyên liệu, ảnh, bình luận, trạng thái theo viewer. |
| `POST` | `/api/community/recipes/publish` | Publish công thức từ form/draft. |
| `PUT` | `/api/community/recipes/{recipeId}` | Cập nhật công thức đã publish. |
| `DELETE` | `/api/community/recipes/{recipeId}?customerId={customerId}` | Xóa công thức. Backend kiểm tra owner. |
| `GET` | `/api/community/recipes/drafts/{customerId}` | Lấy tất cả bản nháp của user. |
| `POST` | `/api/community/recipes/drafts` | Lưu thêm một bản nháp mới. |
| `DELETE` | `/api/community/recipes/drafts/{draftId}?customerId={customerId}` | Xóa một bản nháp. Backend kiểm tra owner. |
| `POST` | `/api/community/uploads/images` | Upload ảnh công thức, trả về URL public. |

### API cookbook, follow, comment

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| `GET` | `/api/community/cookbooks?customerId={customerId}` | Lấy cookbook của user. |
| `GET` | `/api/community/cookbooks/{cookbookId}` | Lấy chi tiết cookbook và recipe trong đó. |
| `POST` | `/api/community/cookbooks` | Tạo cookbook. |
| `POST` | `/api/community/cookbooks/{cookbookId}/recipes` | Thêm recipe vào cookbook. |
| `GET` | `/api/community/follows` | Lấy danh sách follow/follower. |
| `GET` | `/api/community/follows/counts` | Lấy số follow và trạng thái follow của viewer. |
| `POST` | `/api/community/follows/toggle` | Follow/unfollow chef. |
| `POST` | `/api/community/comments` | Tạo bình luận mới cho recipe. |
| `POST` | `/api/community/comments/{commentId}/like` | Thả tim hoặc bỏ tim bình luận. |

## 5. Bình luận recipe

### Mục tiêu

Phần bình luận bỏ `Reply` và thêm chức năng thả tim bình luận.

### Luồng

1. User mở chi tiết recipe.
2. App gọi `GET /api/community/recipes/{recipeId}/detail?viewerId=...`.
3. Backend trả về `comments`, mỗi comment có:
   - `likeCount`
   - `likedByCurrentUser`
4. User bấm icon tim.
5. App gọi `POST /api/community/comments/{commentId}/like`.
6. Backend toggle customerId trong danh sách liked của comment, cập nhật count và trả comment mới.
7. UI đổi tim sang màu đỏ nếu `likedByCurrentUser = true`.

### Data Mongo

Trong `community_cooking.recipeComments`:

```json
{
  "CommentID": "COM000001",
  "RecipeID": "REC000001",
  "CustomerID": "CUS000005",
  "Content": "Món này ngon quá",
  "LikeCount": 1,
  "CommentLikedCustomerIDs": ["CUS000005"],
  "CreatedAt": "...",
  "SortOrder": 1
}
```

## 6. UI chính đã chỉnh

### Taste

- `activity_taste_preferences.xml`
  - Section tag khẩu vị giữ dạng chip.
  - Section nguyên liệu cần lưu ý giữ dạng card dọc.
  - List nguyên liệu render động từ tag user, không còn card tĩnh.

- `activity_taste_menu_suggestions.xml`
  - Sửa text tiếng Việt có dấu.
  - Product thay thế và recipe dùng item layout thống nhất.

- `item_taste_menu_card.xml`
  - Layout chung cho item trong trang thực đơn phù hợp.

### Community

- Detail recipe:
  - Icon trash chỉ hiện với owner.
  - Xóa recipe có dialog xác nhận.
  - Nút edit mở form edit.

- Post recipe:
  - Xem được nhiều bản nháp.
  - Xóa từng bản nháp có xác nhận.
  - Chọn nguyên liệu không hiển thị SKU.
  - Product picker lọc theo active/status.

- Comment:
  - Bỏ reply.
  - Thêm like comment, tim đỏ khi đã like.

## 7. Kiểm tra sau thay đổi

Các lệnh nên chạy sau khi sửa code:

```bash
.\gradlew.bat :app:compileDebugJavaWithJavac
```

```bash
cd backend
node --check src/routes/userRoutes.js
node --check src/routes/communityRoutes.js
```

Nếu thay đổi API/backend, cần chạy app và test lại các luồng:

- Mở `Khẩu vị của tôi`, thêm tag, đổi chế độ xử lý catalog.
- Mở `Nguyên liệu cần lưu ý`, kiểm tra đủ tag user đã tạo.
- Mở `Thực đơn phù hợp`, kiểm tra sản phẩm thay thế và món ăn an toàn.
- Tạo recipe, lưu nhiều nháp, xem nháp, xóa nháp.
- Publish recipe, mở chi tiết bằng owner và non-owner để kiểm tra edit/delete.
- Bình luận và thả tim bình luận.
