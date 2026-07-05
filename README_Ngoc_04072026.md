## Cập nhật hệ thống xác thực mạng xã hội

### Đăng nhập Facebook

- Bổ sung cơ chế lấy ảnh đại diện chất lượng cao từ Facebook Graph API.
- Đồng bộ tên hiển thị và ảnh đại diện của người dùng vào MongoDB mỗi khi đăng nhập.
- Khắc phục tình trạng hiển thị ảnh đại diện mặc định khi đăng nhập bằng Facebook.

### Liên kết tài khoản Google và Facebook

- Bổ sung cơ chế tự động liên kết tài khoản Google và Facebook khi cùng sử dụng một địa chỉ email.
- Xử lý trường hợp `FirebaseAuthUserCollisionException` để tránh tạo nhiều tài khoản cho cùng một người dùng.
- Cho phép người dùng đăng nhập bằng Google hoặc Facebook nhưng vẫn truy cập cùng một tài khoản và toàn bộ dữ liệu đã có.

### Đồng bộ dữ liệu người dùng

- Cập nhật thông tin hồ sơ người dùng từ nhà cung cấp đăng nhập khi đăng nhập thành công.
- Đồng bộ ảnh đại diện và tên hiển thị lên cơ sở dữ liệu.
- Giữ nguyên dữ liệu người dùng như đơn hàng, địa chỉ, điểm Carbon và lịch sử hoạt động sau khi liên kết tài khoản.

### Cải thiện quy trình xác thực

- Bổ sung xử lý các trường hợp trùng email giữa các nhà cung cấp đăng nhập.
- Hoàn thiện luồng xác thực giữa Android, Firebase Authentication và Backend.
- Tăng cường log phục vụ quá trình kiểm thử và bảo trì hệ thống.