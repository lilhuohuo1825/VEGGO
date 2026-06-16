# README_CHANGES

## Tổng quan

Nhánh hiện tại tập trung vào 3 nhóm thay đổi chính:

1. Migrate Authentication từ SQLite sang MongoDB Atlas thông qua Node.js API.
2. Cập nhật Profile để hiển thị đúng thông tin người dùng sau đăng nhập.
3. Cải tiến giao diện và luồng Checkout/Cart.

---

## Authentication

### Đã hoàn thành

* Login bằng MongoDB API.
* Register bằng MongoDB API.
* Forgot Password.
* Reset Password.
* Lưu session bằng AppPreferences.
* Tắt app mở lại vẫn giữ đăng nhập.
* Logout hoạt động bình thường.

### Kiến trúc mới

```text
Android
→ AuthViewModel
→ AuthRepository
→ Retrofit API
→ Node.js Backend
→ MongoDB Atlas
```

### File chính ảnh hưởng

* LoginActivity.java
* RegisterActivity.java
* ForgotPasswordActivity.java
* AuthViewModel.java
* AuthRepository.java
* AuthRepositoryImpl.java
* AuthApi.java
* UserDto.java
* AppPreferences.java
* backend/src/routes/userRoutes.js
* backend/src/models/User.js

---

## Profile

### Đã cập nhật

* Hiển thị đúng user sau Login/Register.
* Không còn lấy user mặc định từ SQLite khi đã có session.
* Đồng bộ thông tin từ MongoDB.

### File chính

* ProfileFragment.java
* ProfileLoggedInFragment.java
* PersonalInfoActivity.java
* AssetScreenData.java

---

## Checkout & Cart

### Đã cập nhật

* Giao diện Checkout.
* Chọn Voucher.
* Chọn Địa chỉ.
* Phương thức thanh toán.
* Cập nhật UI Giỏ hàng.

### File chính

* CheckoutActivity.java
* CheckoutGuestActivity.java
* CartFragment.java
* CartAdapter.java

---

## Lưu ý khi merge

Ưu tiên giữ phiên bản hiện tại của các file Auth:

* LoginActivity.java
* RegisterActivity.java
* ForgotPasswordActivity.java
* AuthViewModel.java
* AuthApi.java
* userRoutes.js
* User.js

Các file Checkout và Profile nên review conflict thủ công trước khi merge.

---

## Đã test

✅ Login

✅ Register

✅ Forgot Password

✅ Reset Password

✅ Logout

✅ Session Persistence

✅ Profile hiển thị đúng user

✅ User được lưu trên MongoDB Atlas
