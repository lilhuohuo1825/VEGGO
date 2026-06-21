# Tổng hợp các thay đổi Giao diện & Chức năng chưa Commit

Dưới đây là danh sách toàn bộ các tính năng, giao diện và logic vừa được thêm/sửa đổi trên nhánh `feature/quyen-ui` nhưng chưa được commit lên Git.

## 1. Trang Chủ (HOME - `fragment_home.xml` & `HomeFragment.java`)
- **Đồng bộ UI Nút "Xem thêm"**: 
  - Đã thêm icon mũi tên xổ xuống (`ic_chevron_down`) cho tất cả các nút Xem thêm (Danh mục, Flash sale, Công thức phổ biến, và phần Phân trang dưới cùng).
  - Áp dụng màu xanh lá chủ đạo (`@color/primary_main`) cho toàn bộ text và icon của các nút này bằng `app:drawableTint` và `app:tint`.
- **Thanh điều hướng sản phẩm (Sticky Tabs)**: 
  - Tạo layout mới `layout_home_sticky_tabs.xml` để hiển thị thanh tab danh mục sản phẩm.

## 2. Chi tiết khuyến mãi (PROMOTION - `PromotionDetailActivity.java`)
- **Tạo mới toàn bộ Màn hình Chi tiết Khuyến mãi**:
  - Giao diện mới hoàn toàn: `activity_promotion_detail.xml`.
  - Box hiển thị mã giảm giá với viền nét đứt (`bg_promotion_code_dashed.xml`) và nút Copy (`ic_copy.png`).
  - Gắn tag khuyến mãi màu xanh (`ic_promotiontag.png`).
  - Nút "Xem thêm" phân trang sản phẩm áp dụng khuyến mãi cũng được đồng bộ màu và icon giống trang Home.

## 3. Quản lý Tủ lạnh (Smart Fridge)
- **Tích hợp AI Quét hóa đơn / Nhận diện nguyên liệu**:
  - Thêm tính năng chọn chụp ảnh (Camera) hoặc tải ảnh từ thư viện (Gallery) qua các popup `dialog_scan_options.xml` & `dialog_image_source.xml`.
  - Cập nhật `AddFridgeIngredientActivity` để nhận dữ liệu ảnh, thu nhỏ ảnh, mã hóa Base64 và gửi lên backend.
- **Backend AI Integration**:
  - Viết logic gọi Google Gemini API (`gemini-2.5-flash`) trên Node.js backend (`fridgeRoutes.js`) để phân tích JSON nguyên liệu từ hóa đơn. Nếu dùng hết sl 20l/ngày thì tự đổi API test
- **Đồng bộ Dialog Vị trí**:
  - Chỉnh lại giao diện `dialog_fridge_add_location.xml` và `dialog_location.xml` (đồng bộ độ bo góc, padding, typography).
- **Hệ thống Cảnh báo hết hạn (Notification)**:
  - Bổ sung `FridgeExpiryScheduler` và `FridgeExpiryNotificationReceiver`.
  - Lên lịch bằng `AlarmManager.setExactAndAllowWhileIdle()` chạy tự động lúc **8:00 sáng hàng ngày**. Có thể thay đổi thời gian để test
  - Logic cảnh báo linh hoạt: Báo trước các nguyên liệu sẽ hết hạn trong vòng **0, 1, 2, và 3 ngày tới** (thay vì chỉ cứng nhắc đúng ngày mai).
  - Tự động popup xin quyền `POST_NOTIFICATIONS` trên Android 13+ ở `MainActivity`.
  - Cấp đầy đủ quyền `SCHEDULE_EXACT_ALARM` trong `AndroidManifest.xml` để tránh văng app.
- **Chi tiết nguyên liệu**:
  - Tạo màn hình `FridgeIngredientDetailActivity` (`activity_fridge_ingredient_detail.xml`) để xem chi tiết item trong tủ lạnh.

## 4. Đơn hàng (Orders)
- Cập nhật luồng giao diện cho Lịch sử đơn hàng và Chi tiết đơn hàng:
  - `OrderHistoryFragment.java` & `fragment_order_history.xml`.
  - `OrderDetailActivity.java` & `activity_order_detail.xml`.
  - Thêm item layout mới cho sản phẩm trong chi tiết đơn hàng: `item_order_detail_product_kale.xml`.
  - Thêm logic trong đơn hàng là có thể chọn 1 hoặc nhiều sp trong đơn hàng có status "Completed" để thêm vào tủ lạnh
## 5. Backend & Database
- Khởi tạo các Collection mới trên MongoDB: `FridgeItem`, `FridgeLocation`, `Promotion`, `PromotionTarget`, `PromoBannerImage`.
- Viết API đầy đủ cho: Nhận diện AI, Thêm vị trí tủ lạnh, Thêm nguyên liệu thủ công và từ lịch sử đơn hàng, API Khuyến mãi.

---
*Lưu ý: Nhớ khởi động lại (restart) Backend Node.js nếu bạn đang chạy local để cập nhật API Key và model AI mới nhất (`gemini-2.5-flash`).*


