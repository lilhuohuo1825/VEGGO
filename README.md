# VEGGO

VEGGO là một hệ thống bán hàng và chăm sóc người dùng gồm 3 phần:

- Ứng dụng Android cho khách hàng.
- Backend Node.js/Express kết nối MongoDB Atlas, Firebase và Socket.IO.
- Trang quản trị web Angular.

## Tổng Quan Kiến Trúc

Ứng dụng Android được xây theo các lớp:

```text
presentation -> domain -> data -> core
```

- `presentation/`: Activity, Fragment, ViewModel, UI state.
- `domain/`: model nghiệp vụ, repository contract, use case.
- `data/`: API, Firebase, Room, mapper, repository implementation.
- `core/`: network, database, firebase, notification, preference, utility.
- `adapter/`: RecyclerView adapters.

Backend đóng vai trò lớp trung gian:

- Không kết nối MongoDB trực tiếp từ Android.
- Cung cấp REST API cho app và admin web.
- Hỗ trợ Socket.IO cho realtime chat/notification.
- Tích hợp Firebase Admin, AI chatbot, payment, reminder job và một số tác vụ đồng bộ dữ liệu.

## Cấu Trúc Dự Án

- `app/`: mã nguồn Android.
- `backend/`: API server Node.js.
- `admin-web/`: dashboard quản trị Angular.
- `docs/`: tài liệu cho chatbot và speech-to-text.
- `vitacare-reference/`: thư mục tham chiếu/migration cũ, không thuộc luồng chạy chính của VEGGO.

## Luồng Chạy Chính

1. Android mở từ `OnboardingActivity`, sau đó vào `MainActivity`.
2. `MainActivity` điều phối các tab chính như Home, Orders, Account, Community, Scan.
3. ViewModel gọi repository, repository gọi API backend hoặc Room/Firebase.
4. Backend xử lý nghiệp vụ, truy cập MongoDB Atlas, Firebase Admin, Socket.IO và các service phụ trợ.
5. Admin web gọi cùng backend API để quản trị dữ liệu.

## Tính Năng Chính

- Đăng nhập, đăng ký, quên mật khẩu, Google/Facebook login.
- Trang chủ, danh mục, tìm kiếm, chi tiết sản phẩm.
- Giỏ hàng, checkout, thanh toán, đơn hàng, hoàn trả, đánh giá.
- Chat hỗ trợ realtime.
- Blog, cộng đồng, bài viết, bình luận.
- Tủ lạnh thông minh, gợi ý món ăn, barcode scan, camera scan.
- VeggoPay, ví điện tử, chuyển tiền, quét QR.
- Điểm carbon, chứng nhận carbon.
- Quản lý đơn lặp lại và nhắc lịch giao hàng.

## Thiết Lập Môi Trường

### Android

- Android package: `com.veggo.app`
- API URL được lấy từ `local.properties` hoặc mặc định theo chế độ emulator/physical device.

Ví dụ:

```properties
dev.api.mode=emulator
```

Hoặc khi chạy trên máy thật cùng mạng LAN:

```properties
dev.api.mode=physical
dev.api.host=<LAN-IP-máy-chạy-backend>
```

### Backend

Tạo file môi trường từ mẫu:

```bash
cp backend/.env.example backend/.env
```

Cần cấu hình tối thiểu:

- `MONGODB_URI`
- `FIREBASE_SERVICE_ACCOUNT_PATH`
- các biến AI/payment nếu có dùng

Khởi động backend:

```bash
cd backend
npm install
npm run dev
```

Health check:

```text
GET http://localhost:5001/api/health
```

### Firebase

- `app/google-services.json` cho Android.
- `backend/firebase-service-account.json` cho Firebase Admin ở backend.

## Chạy Dự Án

### Android

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

### Backend

```bash
cd backend
npm run dev
```

### Admin Web

```bash
cd admin-web
npm install
npm start
```

## Quy Ước Làm Việc

- Không commit file nhạy cảm: `.env`, service account, keystore, APK/AAB, `node_modules`, file IDE local.
- Không để Android truy cập MongoDB Atlas trực tiếp.
- Giữ logic nghiệp vụ trong ViewModel/UseCase/Repository, không gọi API trực tiếp từ Activity/Fragment.

## Tài Liệu Liên Quan

- [docs/CHATBOT.md](docs/CHATBOT.md)
- [docs/SPEECH.md](docs/SPEECH.md)
- [docs/PRICE_FORECAST.md](docs/PRICE_FORECAST.md)
- [docs/VEGGOPAY.md](docs/VEGGOPAY.md)
- [docs/IMAGE_RECOGNITION.md](docs/IMAGE_RECOGNITION.md)
