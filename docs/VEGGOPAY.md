# VEGGO Pay

Tài liệu mô tả ví điện tử VeggoPay trong hệ thống VEGGO.

---

## Mục Tiêu

VeggoPay là lớp thanh toán nội bộ của VEGGO để:

- nạp tiền
- chuyển tiền cho người dùng khác
- nạp/rút theo luồng wallet
- liên kết ngân hàng
- quét QR để giao dịch
- ghi nhận biến động số dư
- gắn điểm carbon / cây xanh với hành vi tiêu dùng

---

## Luồng Hệ Thống

### Backend

Backend xử lý qua:

- [backend/src/routes/paymentRoutes.js](../backend/src/routes/paymentRoutes.js)
- [backend/src/controllers/walletController.js](../backend/src/controllers/walletController.js)
- [backend/src/models/Wallet.js](../backend/src/models/Wallet.js)
- [backend/src/models/WalletTransaction.js](../backend/src/models/WalletTransaction.js)

### Android

Màn hình chính:

- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayActivity.java)

Các màn hình liên quan:

- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayTransferActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayTransferActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayScanActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayScanActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayDonateActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayDonateActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayHistoryActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayHistoryActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayMyQrActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayMyQrActivity.java)

---

## Luồng Chức Năng

### 1. Mở ví

`VeggoPayActivity`:

- kiểm tra người dùng đã đăng nhập chưa
- kiểm tra trạng thái ví
- nếu ví chưa active thì điều hướng sang policy screen
- nếu ví active thì mở khóa bằng mật khẩu hoặc sinh trắc học

### 2. Nạp tiền

Luồng nạp tiền dùng API payment để sinh URL VNPay.

Hợp đồng backend:

- tạo payment URL
- trả về deep link kết quả
- cập nhật trạng thái đơn / giao dịch sau khi thanh toán xong

### 3. Chuyển tiền

Luồng chuyển tiền trong ví:

- tìm người nhận bằng số điện thoại
- kiểm tra người nhận có ví active
- xác thực mật khẩu ví
- trừ tiền người gửi, cộng tiền người nhận
- ghi transaction cho cả hai bên

### 4. Liên kết ngân hàng

Wallet hỗ trợ:

- thêm ngân hàng
- đặt ngân hàng mặc định
- sửa thông tin ngân hàng liên kết
- hủy liên kết

### 5. Quét QR

VeggoPay có màn quét QR/barcode riêng để:

- nhận diện mã thanh toán
- mở luồng giao dịch nhanh

---

## Dữ Liệu Chính

### Wallet

Thường chứa:

- `customerId`
- `balance`
- `status`
- `linkedBanks`
- cấu hình bảo mật ví

### WalletTransaction

Mỗi giao dịch nên có:

- `transactionId`
- `customerId`
- `amount`
- `type`
- `status`
- `referenceId`
- `description`
- `carbonPoints`

---

## Điểm Cần Chú Ý

- Không nên để app tự tính logic tiền tệ quan trọng nếu backend đã có nguồn dữ liệu chuẩn.
- Các thao tác trừ/cộng tiền cần idempotent.
- Tất cả callback thanh toán nên được ghi nhận trên backend trước, sau đó app chỉ sync lại.
- Trạng thái ví, số dư, và lịch sử giao dịch nên refresh mỗi lần `onResume()`.

---

## File Liên Quan

- [backend/src/routes/paymentRoutes.js](../backend/src/routes/paymentRoutes.js)
- [backend/src/controllers/walletController.js](../backend/src/controllers/walletController.js)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayTransferActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayTransferActivity.java)
- [app/src/main/java/com/veggo/app/presentation/profile/VeggoPayScanActivity.java](../app/src/main/java/com/veggo/app/presentation/profile/VeggoPayScanActivity.java)
