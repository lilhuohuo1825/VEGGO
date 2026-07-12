# VEGGO Price Forecast

Tài liệu mô tả luồng dự báo giá rau củ quả trong VEGGO. Mục tiêu của phần này là cung cấp dự báo ngắn hạn để hỗ trợ người dùng ra quyết định mua hàng và để hiển thị cảnh báo giá trong app.

---

## Phạm Vi

Hiện tại trong codebase, VEGGO đã có:

- API lấy dự báo giá theo sản phẩm: `GET /api/forecast/:productId`
- DTO Android để nhận dữ liệu dự báo
- UI cảnh báo giá trong trang chi tiết sản phẩm và helper hiển thị dialog cảnh báo
- Schema lưu cache dự báo trong MongoDB: `PriceForecast`

Tài liệu này mô tả hợp đồng dữ liệu và cách tích hợp. Nếu bạn có một pipeline ML riêng để tạo forecast, pipeline đó có thể ghi kết quả vào collection `PriceForecast`.

---

## Ý Tưởng Mô Hình

Mô hình dự báo nên là mô hình hồi quy hoặc chuỗi thời gian ngắn hạn cho từng sản phẩm.

### Dữ liệu đầu vào gợi ý

- Giá lịch sử theo ngày hoặc theo tuần.
- Biến động thời tiết.
  - lượng mưa
  - nhiệt độ
  - độ ẩm
  - cảnh báo thiên tai
- Yếu tố mùa vụ.
  - mùa thu hoạch
  - tháng cao điểm tiêu dùng
  - dịp lễ/tết
- Tín hiệu cung cầu.
  - lượng hàng nhập kho
  - sold count
  - tồn kho
  - khu vực / kho phân phối
- Metadata sản phẩm.
  - category
  - origin
  - unit
  - brand

### Đầu ra mong muốn

Mỗi forecast nên trả về:

- `currentPrice`
- `predictedPrice7Days`
- `changePercent`
- `trend` = `up | down | stable`
- `reason`

`reason` nên là diễn giải ngắn gọn theo ngôn ngữ người dùng, ví dụ:

- giá tăng do mưa lớn kéo dài ở vùng cung ứng
- giá giảm do vào mùa vụ thu hoạch
- giá ổn định do nguồn cung và nhu cầu cân bằng

---

## Hợp Đồng Dữ Liệu

### Backend schema

File mô hình:

- [backend/src/models/PriceForecast.js](../backend/src/models/PriceForecast.js)

Các field chính:

- `productId`
- `sku`
- `currentPrice`
- `predictedPrice7Days`
- `changePercent`
- `trend`
- `reason`

### API backend

File route:

- [backend/src/routes/forecastRoutes.js](../backend/src/routes/forecastRoutes.js)

Luồng hiện tại:

1. Kiểm tra sản phẩm có tồn tại không.
2. Tìm forecast đã lưu trong MongoDB.
3. Nếu chưa có forecast, trả baseline ổn định.
4. Nếu có forecast, trả lại dữ liệu đã cache.

### Android DTO

File DTO:

- [app/src/main/java/com/veggo/app/data/remote/dto/ForecastResponseDto.java](../app/src/main/java/com/veggo/app/data/remote/dto/ForecastResponseDto.java)

UI đọc forecast ở:

- [app/src/main/java/com/veggo/app/presentation/product/ProductDetailActivity.java](../app/src/main/java/com/veggo/app/presentation/product/ProductDetailActivity.java)
- [app/src/main/java/com/veggo/app/presentation/dialog/PriceAlertHelper.java](../app/src/main/java/com/veggo/app/presentation/dialog/PriceAlertHelper.java)

---

## Gợi Ý Cách Triển Khai ML

Nếu bạn muốn xây mô hình thật sự, kiến trúc hợp lý là:

1. ETL tổng hợp dữ liệu lịch sử.
2. Tạo feature theo ngày/tuần.
3. Huấn luyện model.
4. Chạy batch inference theo lịch.
5. Ghi kết quả vào `PriceForecast`.
6. Android chỉ đọc kết quả, không tự tính forecast cục bộ.

### Mô hình phù hợp

- Baseline: linear regression / XGBoost / Random Forest
- Chuỗi thời gian: ARIMA, Prophet, LSTM, Temporal Fusion Transformer
- Thực tế sản phẩm bán lẻ: thường nên bắt đầu từ model dễ giải thích trước, rồi mới nâng cấp sang model phức tạp hơn

---

## Cảnh Báo Giá Trong App

`PriceAlertHelper` chỉ hiển thị cảnh báo khi:

- `changePercent >= 10%` hoặc `changePercent <= -10%`
- mỗi sản phẩm chỉ cảnh báo 1 lần/ngày
- toàn app cũng có giới hạn spam theo ngày

Nếu `trend = down`, dialog sẽ ưu tiên thông điệp mua hời. Nếu `trend = up`, dialog sẽ cảnh báo tăng giá.

---

## File Liên Quan

- [backend/src/models/PriceForecast.js](../backend/src/models/PriceForecast.js)
- [backend/src/routes/forecastRoutes.js](../backend/src/routes/forecastRoutes.js)
- [app/src/main/java/com/veggo/app/data/remote/api/ForecastApi.java](../app/src/main/java/com/veggo/app/data/remote/api/ForecastApi.java)
- [app/src/main/java/com/veggo/app/presentation/dialog/PriceAlertHelper.java](../app/src/main/java/com/veggo/app/presentation/dialog/PriceAlertHelper.java)
