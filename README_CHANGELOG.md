# Hướng dẫn & Tài liệu các thay đổi Tính năng (Scan Search & VNPay Payment)

Tài liệu này mô tả chi tiết các luồng xử lý và thay đổi kỹ thuật từ khi tích hợp chức năng **tìm kiếm sản phẩm qua nút Scan** (Scan Search) và **cổng thanh toán trực tuyến VNPay** trên dự án VEGGO.

---

## 1. Luồng chức năng Tìm kiếm qua nút Scan (Scan Search)
Chức năng này cho phép người dùng sử dụng Camera để quét hóa đơn hoặc chụp ảnh một nguyên liệu bất kỳ. Hệ thống sẽ nhận diện nguyên liệu bằng AI (Gemini) và tự động tìm kiếm, đối khớp các sản phẩm tương ứng đang được bán trên cửa hàng VEGGO.

### 🔄 Luồng xử lý chi tiết (Flow):
```mermaid
sequenceGraph
    MainActivity ->> dialog_scan_options: Click nút Scan (Hiển thị 2 lựa chọn)
    alt Chọn Quét hóa đơn (Scan Receipt)
        dialog_scan_options ->> Camera: Mở camera chế độ Quét hóa đơn
    else Chọn Nhận diện nguyên liệu (Scan Ingredient)
        dialog_scan_options ->> Camera: Mở camera chế độ Nhận diện
    end
    Camera -->> MainActivity: Trả về URI ảnh chụp thành công
    MainActivity ->> AddFridgeIngredientActivity: Mở Activity thêm nguyên liệu kèm Image URI
    AddFridgeIngredientActivity ->> Backend API: Gọi API nhận diện nguyên liệu (Gemini AI)
    Backend API -->> AddFridgeIngredientActivity: Trả về tên nguyên liệu + Danh sách sản phẩm khớp trong Store (matchedProducts)
    
    alt Khớp chính xác 1 sản phẩm
        AddFridgeIngredientActivity ->> BottomSheet (dialog_matched_product_single): Hiển thị chi tiết sản phẩm đơn lẻ
    else Khớp nhiều sản phẩm
        AddFridgeIngredientActivity ->> BottomSheet (dialog_matched_products_list): Hiển thị Grid danh sách sản phẩm khớp
    else Không khớp sản phẩm nào
        AddFridgeIngredientActivity ->> AlertDialog (showNoMatchDialog): Thông báo không tìm thấy sản phẩm tương thích
    end
```

### 🛠️ Các thành phần thay đổi/tạo mới:
* **Layouts mới:**
  * `dialog_scan_options.xml`: Hộp thoại chọn chế độ quét (Hóa đơn / Nguyên liệu).
  * `dialog_matched_product_single.xml`: Bottom sheet hiển thị thông tin sản phẩm khớp đơn lẻ (ảnh, tên, giá bán, giá gốc, rating, lượt bán, nút Xem chi tiết, nút Thêm vào giỏ).
  * `dialog_matched_products_list.xml`: Bottom sheet hiển thị Grid danh sách các sản phẩm khớp kèm nút xem chi tiết và thêm nhanh vào giỏ hàng.
* **Android Code (`MainActivity.java` & `AddFridgeIngredientActivity.java`):**
  * `MainActivity.java`: Xử lý quyền camera và chuyển tiếp URI ảnh chụp kèm biến cờ hiệu `EXTRA_FROM_NAVBAR = true`.
  * `AddFridgeIngredientActivity.java`: Tích hợp các hàm kết nối API Gemini, xử lý logic hiển thị BottomSheet tương ứng với số lượng sản phẩm khớp (`showSingleMatchedProductDialog`, `showMultiMatchedProductsDialog`, `showNoMatchDialog`).
* **Backend API (`fridgeRoutes.js`):**
  * Route `POST /api/fridge/recognize`: Gọi model AI để bóc tách nguyên liệu từ ảnh và query chéo sang collection `products` để đối khớp sản phẩm theo tên/nhãn.

---

## 2. Luồng tích hợp cổng thanh toán trực tuyến VNPay
Tích hợp cổng thanh toán VNPay Sandbox giúp người dùng thanh toán hóa đơn trực tuyến an toàn. Luồng xử lý được tinh chỉnh tối đa giúp loại bỏ việc nhấp nháy giao diện cũ và chuyển hướng thẳng sang trang thông báo đặt hàng thành công.

### 🔄 Luồng xử lý chi tiết (Flow):
```mermaid
sequenceGraph
    CheckoutActivity ->> Backend: 1. Gọi API lấy link thanh toán (VNPAY Url)
    Note over CheckoutActivity: Hiển thị phủ mờ: "Đang tạo liên kết..."
    Backend -->> CheckoutActivity: Trả về Payment URL
    CheckoutActivity ->> VnpayWebViewActivity: 2. Mở WebView tải URL thanh toán
    VnpayWebViewActivity ->> VNPAY Gateway: Người dùng nhập OTP / Xác thực thẻ ngân hàng
    VNPAY Gateway -->> Backend (vnpay-return): Redirect kết quả thanh toán
    Backend -->> VnpayWebViewActivity: Redirect Deep Link: veggo://payment-result?success=true&orderId=...
    Note over VnpayWebViewActivity: Intercept deep link -> RESULT_OK
    VnpayWebViewActivity -->> CheckoutActivity: 3. Đóng WebView và trả RESULT_OK
    Note over CheckoutActivity: Hiển thị phủ mờ: "Đang đồng bộ đơn hàng..."
    CheckoutActivity ->> Backend (createOrder): 4. Gọi API tạo đơn hàng chính thức (status: paid)
    Backend -->> CheckoutActivity: Tạo đơn hàng thành công trong database
    CheckoutActivity ->> QrPaymentActivity: 5. Chuyển tiếp tới màn hình Success (EXTRA_SHOW_SUCCESS_IMMEDIATELY = true)
    Note over CheckoutActivity: Gọi finish() giải phóng CheckoutActivity
    QrPaymentActivity -->> Người dùng: Hiển thị giao diện "Chúc mừng đặt hàng thành công!"
```

### 🛠️ Các thành phần thay đổi/tạo mới:

#### 1. Backend NodeJS (`paymentRoutes.js` & `.env`):
* Cung cấp các Endpoint:
  * `POST /api/payment/create-payment-url`: Sắp xếp các tham số chữ cái (`sortObject`), mã hóa url thô, tạo chữ ký số HMAC-SHA512 (`vnp_SecureHash`) và trả về chuỗi URL thanh toán.
  * `GET /api/payment/vnpay-return`: Endpoint tiếp nhận phản hồi redirect từ VNPay. Thực hiện đối soát chữ ký, cập nhật trạng thái đơn hàng sang `paid` trong DB MongoDB, sau đó redirect sang Deep Link của ứng dụng `veggo://payment-result?success=...`.
  * `GET /api/payment/vnpay-ipn`: Endpoint xử lý Server-to-Server đảm bảo đồng bộ đơn hàng kể cả khi kết nối mạng Client bị gián đoạn.
* Cấu hình biến `.env`: Thiết lập các khóa bảo mật `VNP_TMN_CODE`, `VNP_HASH_SECRET`, `VNP_URL` và `VNP_RETURN_HOST`.

#### 2. Android App:
* **Mở rộng kết nối HTTP (Cleartext):**
  * Cập nhật `AndroidManifest.xml` (thêm thuộc tính `android:usesCleartextTraffic="true"`).
  * Cập nhật `network_security_config.xml` (thêm IP Wi-Fi máy tính phát triển `10.0.1.184` và khai báo `<base-config cleartextTrafficPermitted="true" />` để cho phép kết nối HTTP Sandbox không mã hóa).
* **WebView Thanh toán (`VnpayWebViewActivity.java` & `activity_vnpay_webview.xml`):**
  * Tải URL thanh toán.
  * Override hàm `shouldOverrideUrlLoading` (hỗ trợ cả kiểu String và WebResourceRequest) và `onNewIntent()` để bắt chính xác Deep Link `veggo://payment-result` từ server chuyển hướng về, trả lại kết quả `RESULT_OK` cho Checkout.
* **Hộp thoại tiến trình phủ mờ (Translucent Loading Overlay):**
  * Cài đặt hàm `showProgress(String message)` trong `CheckoutActivity.java` sử dụng một Dialog suốt và layout có màu nền mờ `#99000000` (60% đen translucent) để che phủ toàn bộ màn hình khi gọi API hoặc đồng bộ đơn hàng. Hộp thoại này tự giải phóng trong `onDestroy()` để tránh rò rỉ bộ nhớ.
* **Xử lý hoàn tất đặt hàng:**
  * Khi nhận được kết quả `RESULT_OK` từ WebView, `CheckoutActivity` ngay lập tức bật phủ mờ `"Đang đồng bộ và khởi tạo đơn hàng VNPay..."` che form cũ, gọi API lưu đơn hàng chính thức lên MongoDB Atlas, rồi điều hướng tới `QrPaymentActivity` kèm biến cờ hiệu `EXTRA_SHOW_SUCCESS_IMMEDIATELY = true`.
  * Gọi `finish()` ngay trên `CheckoutActivity` để đảm bảo người dùng không bị nút Back đưa quay lại trang nhập liệu Checkout cũ.
  
* **Thông tin truy cập Merchant Admin để quản lý giao dịch:**
  * Địa chỉ: https://sandbox.vnpayment.vn/merchantv2/
  * Tên đăng nhập: nttq820@gmail.com
  * Mật khẩu: Nttquyen820@

  * Thẻ test:
    * Ngân hàng: NCB
    * Số thẻ: 9704198526191432198
    * Tên chủ thẻ: NGUYEN VAN A
    * Ngày phát hành: 07/15
    * Mật khẩu OTP: 123456


---

## 3. Tinh chỉnh giao diện Onboarding (Ẩn nút Skip ở trang cuối)
Ở trang Onboarding thứ 4 (`onboarding_4`), do đã xuất hiện nút "Bắt đầu" (`btnNext` đổi text sang `onboarding_start`) để hoàn tất giới thiệu và đi vào trang chủ, nút "Bỏ qua" (`tvSkip`) đã được ẩn đi để tối ưu trải nghiệm người dùng và tránh dư thừa nút bấm.

### 🔄 Luồng xử lý chi tiết (Flow):
* Khi người dùng trượt qua lại giữa các trang Onboarding, `ViewPager2.OnPageChangeCallback` sẽ nhận sự kiện chọn trang (`onPageSelected(position)`).
* Hệ thống sẽ kiểm tra vị trí hiện tại:
  * Nếu là trang cuối cùng (`position == adapter.getItemCount() - 1`):
    * Nút điều hướng chuyển text thành **Bắt đầu**.
    * Nút Bỏ qua (`tvSkip`) được đặt trạng thái `View.INVISIBLE`.
  * Nếu là các trang trước đó:
    * Nút điều hướng chuyển text thành **Tiếp theo**.
    * Nút Bỏ qua (`tvSkip`) được đặt trạng thái `View.VISIBLE`.

### 🛠️ Các thành phần thay đổi/tạo mới:
* **Android Code ([OnboardingActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/onboarding/OnboardingActivity.java)):**
  * Cập nhật callback `registerOnPageChangeCallback` của `viewPager` để tự động bật/tắt hiển thị nút Skip dựa vào chỉ số slide hiện tại.


