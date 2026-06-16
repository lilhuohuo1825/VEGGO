\# Cập nhật giao diện Home \& Chatbot - Feature Quyen UI



\## Các thay đổi đã thực hiện



\### Trang Home



\* Chỉnh để bấm vào thanh tìm kiếm sẽ mở giao diện Search.

\* Đổi tên các tiện ích ngắn gọn hơn:



&#x20; \* Trợ lý AI

&#x20; \* Tủ lạnh

&#x20; \* Khẩu vị

&#x20; \* Blog

&#x20; \* Carbon

\* Liên kết các tiện ích đến màn hình tương ứng.

\* Banner tự động chuyển trang sau 10 giây.

\* Chỉnh font của `item\_home\_category\_horizontal` đồng bộ với phần Tiện ích.

\* Thay đổi icon danh mục theo đúng tên danh mục.

\* Thay ảnh `ic\_leaf`.

\* Chỉnh dữ liệu danh mục trong `categories.json`:



&#x20; \* "Lương thực - ngũ cốc" → "Lương thực, ngũ cốc"

\* Xóa nền xám sản phẩm trong:



&#x20; \* `item\_home\_flash\_sale`

&#x20; \* `item\_home\_recipe`

\* Xóa file:



&#x20; \* `bg\_home\_image.xml`



\---



\### Onboarding



\* Cập nhật ảnh:



&#x20; \* `onboarding\_3.png`

&#x20; \* `onboarding\_4.png`



\---



\### Chatbot



Đã thêm giao diện Chatbot mới gồm:



\* Màn hình Chatbot.

\* Tin nhắn người dùng.

\* Tin nhắn bot.

\* Tin nhắn chào mừng.

\* Quick Reply.

\* Card sản phẩm trong hội thoại.

\* Giao diện nhập tin nhắn.

\* Icon lịch sử chat.



Các file mới:



\* ChatAdapter.java

\* ChatMessage.java

\* ChatbotActivity.java

\* activity\_chatbot.xml

\* item\_chat\_user.xml

\* item\_chat\_bot.xml

\* item\_chat\_welcome.xml

\* item\_chat\_product.xml

\* item\_quick\_reply.xml



\---



\## Ghi chú cho việc đồng bộ UI



\### Font



\* Tiêu đề section (Tiện ích, Danh mục, Công thức...):



&#x20; \* Inter

&#x20; \* Size 18sp



\* Nội dung card danh mục:



&#x20; \* Inter

&#x20; \* Size 13sp



\* Nội dung thông thường, tab được chọn, nút "Xem thêm":



&#x20; \* Inter

&#x20; \* Size 14sp

&#x20; \* Tab được chọn dùng SemiBold và màu Primary



\* Card sản phẩm dạng Grid, Feature trong Profile:



&#x20; \* Inter

&#x20; \* Size 16sp



\---



\### Header



Có 2 kiểu icon quay lại:



\#### Kiểu 1



Theo trang "Về Veggo"



\* Không có nền tròn.



\#### Kiểu 2



Theo trang Community



\* Có nền tròn phía sau icon.



Đề nghị các màn hình mới chọn một trong hai kiểu trên để đảm bảo đồng nhất giao diện.



