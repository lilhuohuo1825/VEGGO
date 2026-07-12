# VEGGO Image Recognition

Tài liệu mô tả phần nhận diện hình ảnh trong VEGGO.

Trong codebase hiện tại có 2 nhánh liên quan:

- nhận diện ảnh nguyên liệu / hóa đơn / bao bì bằng Gemini Vision
- quét QR/barcode cho VeggoPay

Tài liệu này tập trung vào nhánh nhận diện ảnh bằng AI.

---

## Mục Tiêu

Người dùng có thể chụp hoặc tải ảnh lên để:

- nhận diện nguyên liệu
- tách mặt hàng từ hóa đơn
- gợi ý sản phẩm phù hợp trên VEGGO
- đổ dữ liệu vào tủ lạnh thông minh

---

## Luồng Hiện Tại Trong Code

### Android

Hai màn hình chính:

- [app/src/main/java/com/veggo/app/presentation/search/SearchImageSuggestionsActivity.java](../app/src/main/java/com/veggo/app/presentation/search/SearchImageSuggestionsActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/AddFridgeIngredientActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/AddFridgeIngredientActivity.java)

Luồng chung:

1. Người dùng chọn ảnh từ camera hoặc thư viện.
2. Ảnh được resize và encode Base64.
3. App gọi backend `POST /api/fridge/ai/recognize`.
4. Backend trả danh sách item nhận diện được.
5. App map item đó sang sản phẩm gợi ý hoặc nguyên liệu trong tủ lạnh.

### Backend

Endpoint:

- [backend/src/routes/fridgeRoutes.js](../backend/src/routes/fridgeRoutes.js)

Backend dùng Gemini Vision để:

- đọc ảnh thực phẩm
- đọc ảnh bao bì
- đọc hóa đơn
- trích xuất danh sách item

Sau đó backend tìm sản phẩm khớp trong MongoDB và trả kết quả cho Android.

---

## Prompt Hiện Tại

Prompt trong backend yêu cầu model:

- trích xuất toàn bộ mặt hàng thực phẩm/đồ uống trong ảnh
- trả về JSON hợp lệ
- với mỗi item, cung cấp:
  - `name`
  - `searchKeyword`
  - `generalCategory`
  - `quantity`
  - `unit`
  - `purchaseDate`

Đây là thiết kế phù hợp cho:

- ảnh hóa đơn
- ảnh sản phẩm đóng gói
- ảnh nguyên liệu thực tế

---

## Kết Quả Trả Về

Mỗi item sau khi nhận diện có thể mang theo:

- tên hiển thị
- từ khóa gốc để tìm kiếm
- danh mục dự phòng
- số lượng
- đơn vị
- ngày mua
- danh sách sản phẩm khớp

Android sẽ:

- hiển thị sản phẩm gợi ý
- hoặc đổ dữ liệu vào block nhập tay của tủ lạnh

---

## Cách Hoạt Động Với Tủ Lạnh Thông Minh

`AddFridgeIngredientActivity` có 2 chế độ:

- quét hóa đơn để tạo nhiều item cùng lúc
- quét 1 ảnh nguyên liệu để nhận diện và gợi ý sản phẩm

Nếu nhận diện thành công, app có thể:

- prefill tên nguyên liệu
- gợi ý số lượng/đơn vị
- gợi ý sản phẩm khớp
- lưu ngày mua và ngày hết hạn

---

## Phạm Vi Của Nhận Diện Ảnh

Phần này **không** phải object detection thời gian thực trên camera.

Đây là luồng:

- chọn ảnh
- gửi ảnh lên backend
- backend dùng Gemini Vision
- trả JSON item

Nếu sau này muốn làm real-time recognition trên camera, nên tách sang pipeline riêng.

---

## File Liên Quan

- [backend/src/routes/fridgeRoutes.js](../backend/src/routes/fridgeRoutes.js)
- [backend/src/config/aiConfig.js](../backend/src/config/aiConfig.js)
- [app/src/main/java/com/veggo/app/data/remote/api/FridgeApi.java](../app/src/main/java/com/veggo/app/data/remote/api/FridgeApi.java)
- [app/src/main/java/com/veggo/app/presentation/search/SearchImageSuggestionsActivity.java](../app/src/main/java/com/veggo/app/presentation/search/SearchImageSuggestionsActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/AddFridgeIngredientActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/AddFridgeIngredientActivity.java)
