# VEGGO Android - Nhật Ký Thay Đổi Chi Tiết (Changelog)

Tài liệu này ghi nhận toàn bộ các chỉnh sửa, cập nhật tính năng và cải tiến giao diện người dùng (UI/UX) đã thực hiện trong phiên làm việc vừa qua trên dự án **VEGGO**.

---

## Danh Sách Các Thay Đổi Chi Tiết

### 1. Quản lý Giỏ hàng (Cart)
* **Tính năng vuốt trái để xóa:** Khôi phục thành công tính năng vuốt sang trái (Swipe-to-Reveal) hiển thị nút **Xóa** nền đỏ cho từng sản phẩm trong giỏ hàng. Tính năng này hiện tại hoạt động độc lập, không còn bắt buộc phải bật chế độ chỉnh sửa (Edit Mode).
  * *Tệp ảnh hưởng:* [CartAdapter.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/adapter/CartAdapter.java)
* **Khắc phục xung đột Long Click:** Sửa lỗi xung đột khi người dùng vuốt nhanh để xóa sản phẩm bị dính hành vi Long Click (hiển thị popup thông tin sản phẩm). 
  * *Giải pháp:* Ngay khi người dùng chạm và di chuyển nhẹ ngón tay quá mức giới hạn độ nhạy (`touchSlop / 2`), hệ thống sẽ ngay lập tức hủy bỏ luồng đếm giờ của Long Click (`longPressRunnable`), giúp thao tác vuốt xóa nhạy, mượt mà và không kéo theo popup nổi lên ngoài ý muốn.
  * *Tệp ảnh hưởng:* [CartAdapter.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/adapter/CartAdapter.java)
* **Cập nhật realtime số lượng giỏ hàng tại Trang chủ:** Sửa lỗi icon giỏ hàng ở ngoài Trang chủ không tự động cập nhật số lượng khi người dùng quay lại từ màn hình giỏ hàng.
  * *Giải pháp:* Lắng nghe sự thay đổi của Back Stack trong `MainActivity` để gọi hàm `refreshCartBadge()` của `HomeFragment` cập nhật số lượng giỏ hàng tức thời.
  * *Tệp ảnh hưởng:* [MainActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/MainActivity.java), [HomeFragment.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/home/HomeFragment.java)

### 2. Tủ lạnh thông minh & Hạn sử dụng (Smart Fridge & Expiry Tags)
* **Bo tròn góc nút Xóa trong tủ lạnh:** Chỉnh sửa phần hiển thị nút **Xóa** màu đỏ khi vuốt trái nguyên liệu trong Tủ lạnh sang bo tròn cả **4 góc** thay vì chỉ bo 2 góc bên phải, giúp đồng bộ hoàn hảo với các thẻ (card item) được bo tròn xung quanh.
  * *Tệp ảnh hưởng:* [bg_swipe_delete_action.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/drawable/bg_swipe_delete_action.xml)
* **Đổi nhãn hết hạn hôm nay:** Rút ngắn nhãn hiển thị `"Hết hạn hôm nay"` thành chữ **`"Hết hạn"`** ngắn gọn, rõ ràng theo đúng yêu cầu mà không làm ảnh hưởng đến các logic tính toán ngày khác.
  * *Tệp ảnh hưởng:* [FridgeInventoryAdapter.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/profile/FridgeInventoryAdapter.java), [FridgeSuggestionsActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/profile/FridgeSuggestionsActivity.java)

### 3. Trợ lý Chatbot Veggo (AI Chatbot)
* **Cân đối bong bóng Chatbot đầu tiên:** Điều chỉnh khoảng cách hiển thị của ô chat xin chào đầu tiên của trợ lý Veggo. Bổ sung khoảng cách lề trái `layout_marginStart="8dp"` và lề phải `layout_marginEnd="64dp"` để bong bóng chat cân đối, cách đều Avatar Chatbot và đồng bộ 100% với các bong bóng trả lời chuẩn khác của chatbot.
  * *Tệp ảnh hưởng:* [item_chat_welcome.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/item_chat_welcome.xml)
* **Loại bỏ hiệu ứng bóng đổ (Shadow) khi nhấn nút Chatbot:** Xóa bỏ hiệu ứng shadow mặc định khi nhấp vào chatbot float nổi ngoài trang chủ và các image button trên navbar.
  * *Tệp ảnh hưởng:* [component_bottom_nav.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/component_bottom_nav.xml) (bổ sung `android:stateListAnimator="@null"`).

### 4. Giao diện Blog & Bài viết (Blog & Articles)
* **Bo tròn Popup bài viết ngẫu nhiên:** Thay đổi bo góc của Popup bài viết ngẫu nhiên (hiển thị lúc mới mở màn hình Blog) thành **Radius 12dp** đồng nhất.
  * *Tệp ảnh hưởng:* [bg_dialog_blog_random.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/drawable/bg_dialog_blog_random.xml) (Tệp mới tạo), [dialog_blog_random.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/dialog_blog_random.xml), [BlogUi.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/blog/BlogUi.java#L174) (chỉnh bo góc ảnh thành 12 để khít viền).
* **Đổi kiểu dáng cho tag "Bài viết hôm nay":** Chỉnh sửa tag có nền trắng viền xanh đậm mảnh (`primary_hover`), chữ xanh đậm và bo góc mềm mại.
  * *Tệp ảnh hưởng:* [bg_blog_random_tag.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/drawable/bg_blog_random_tag.xml) (Tệp mới tạo).
* **In đậm nút "Xem thêm" mục Blog:** Đồng bộ font chữ `"Xem thêm"` trên tiêu đề các mục bài viết Blog bằng font in đậm (`inter_semibold` và `textStyle="bold"`), đồng nhất với nút Xem thêm ngoài trang chủ.
  * *Tệp ảnh hưởng:* [layout_blog_section_header.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/layout_blog_section_header.xml)
* **Thêm thanh chỉ báo màu xanh chuyển Tab mượt mà:**
  * Tích hợp bộ công cụ chỉ báo đàn hồi `CurvedTabIndicatorHelper` chạy trượt dưới các danh mục Tab bài viết.
  * Căn chỉnh lề chữ và padding của tab cân đối (`paddingStart/End="8dp"` và `gravity="center"`) giúp thanh trượt màu xanh căn giữa hoàn hảo dưới tên Tab.
  * Chữ của Tab được chọn sẽ tự động đổi kiểu phông thành `inter_semibold` (in đậm) và các tab khác là `inter_regular` (chữ thường).
  * **Tự động cuộn Tab được chọn vào giữa màn hình:** Khi người dùng click chọn tab, thanh cuộn tab sẽ tự động di chuyển mượt mà đưa tab đó vào giữa vùng nhìn thấy (`smoothScrollTo`). Giao diện cũng chuyển sang cơ chế nạp bài viết thông minh thay vì gọi lại `render()` vẽ lại cả trang, ngăn tình trạng thanh trượt bị giật nhảy về đầu.
  * *Tệp ảnh hưởng:* [item_blog_category_tab.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/item_blog_category_tab.xml), [BlogUi.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/blog/BlogUi.java), [BlogHomeActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/blog/BlogHomeActivity.java)
* **Cải tiến trải nghiệm bình luận & Chống che bàn phím (Blog Detail):**
  * Tự động đẩy vùng nhập bình luận và danh sách bình luận lên sát mép trên của bàn phím ảo bằng cách thêm đệm chân `paddingBottom="200dp"` khi người dùng bấm vào ô nhập liệu, kết hợp với công thức tính tọa độ cuộn tuyệt đối `relativeTop - dp(200)`.
  * Hỗ trợ tự động ẩn bàn phím ảo, giải phóng tiêu điểm (clear focus) và cuộn bài viết chi tiết trở về vị trí đọc ban đầu khi người dùng chạm vào bất kỳ vùng trống nào ngoài ô nhập liệu.
  * Cấu hình ứng dụng tự động co giãn màn hình (`adjustResize`) khi bàn phím ảo xuất hiện.
  * *Tệp ảnh hưởng:* [BlogDetailActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/blog/BlogDetailActivity.java), [AndroidManifest.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/AndroidManifest.xml)
* **Đồng bộ nút Back tròn xanh nhạt trong chi tiết bài viết:** Thêm nền hình tròn màu xanh nhạt (`@drawable/bg_community_circle`) cho nút back nổi ở góc trên bài viết chi tiết, co nhỏ mũi tên back bên trong lại 1 xíu (bằng cách đặt thêm `padding` 10dp) giúp thiết kế đồng điệu hoàn toàn với trang Chi tiết sản phẩm.
  * *Tệp ảnh hưởng:* [BlogDetailActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/blog/BlogDetailActivity.java)
* **Cập nhật yêu thích thời gian thực (Realtime Favorite):** Đưa hàm tải danh sách bài viết từ `onCreate` sang `onResume()` trong trang danh sách Blog chính để khi người dùng nhấn tim ở bên trong chi tiết bài viết và quay lại trang danh sách Blog, biểu tượng tim sẽ lập tức được cập nhật mà không làm thay đổi hay xáo trộn thứ tự các bài đăng đang đọc.
  * *Tệp ảnh hưởng:* [BlogHomeActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/blog/BlogHomeActivity.java)
* **Xóa bỏ tab danh mục sai chính tả "Chẹo dinh dưỡng":** Sửa lỗi gõ sai chính tả danh mục `"categoryTag": "Chẹo dinh dưỡng"` thành `"Mẹo dinh dưỡng"` trong nguồn dữ liệu bài viết, giúp gộp danh mục bài viết chuẩn và loại bỏ tab thừa này khỏi màn hình Blog.
  * *Tệp ảnh hưởng:* [blogs.json](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/assets/blogs.json)

### 5. Chi tiết Đơn hàng (Order Detail)
* **Tính năng sao chép mã đơn hàng:** Bổ sung một icon copy (`20dp x 20dp`) màu xanh đậm (`primary_hover`) có hiệu ứng click nhẹ đứng ngay trước chuỗi mã đơn hàng tại trang chi tiết. Khi nhấn vào, ứng dụng sẽ tự động sao chép mã đơn hàng vào Clipboard của thiết bị và hiển thị Toast thông báo thành công cho người dùng.
  * *Tệp ảnh hưởng:* [activity_order_detail.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/activity_order_detail.xml), [OrderDetailActivity.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/presentation/order/OrderDetailActivity.java)

### 6. Ví điện tử VeggoPay & Màn hình cá nhân
* **Tối ưu độ nổi khối (Elevation) của các thẻ VeggoPay:** Giảm độ bóng đổ `app:cardElevation` từ `2dp` xuống `1dp`, đồng thời thêm lề nhỏ `2dp` ở tất cả các hướng xung quanh các thẻ chức năng (Chuyển tiền, Nạp tiền, Rút tiền, Quét mã, v.v...) để giao diện trông phẳng hơn, sắc nét và cao cấp hơn mà không bị cắt cạnh trên các thiết bị Android mới.
  * *Tệp ảnh hưởng:* [activity_veggopay.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/activity_veggopay.xml)
* **Tăng kích thước icon "Về Veggo" và đổi ảnh:** Chỉnh sửa ảnh đại diện cho mục "Về Veggo" thành tệp to hơn, sắc nét hơn và tăng kích thước hiển thị của icon trong danh mục thông tin cá nhân lên `30dp x 30dp` để cân đối với các biểu tượng menu khác.
  * *Tệp ảnh hưởng:* [profile_menu_info_rows.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/profile_menu_info_rows.xml), [ic_profile_menu_about.png](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/drawable/ic_profile_menu_about.png)

### 7. Xác thực & Đăng nhập (Auth & Input)
* **Tự động mở bàn phím khi nhận OTP:** Cấu hình trình hỗ trợ OTP tự động kích hoạt bàn phím ảo ngay khi người dùng chọn gửi mã OTP nhằm tăng tốc trải nghiệm xác minh.
  * *Tệp ảnh hưởng:* [OtpAutoFillHelper.java](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/java/com/veggo/app/core/otp/OtpAutoFillHelper.java)
* **Màn hình "Cần đăng nhập" nền trắng:** Chuyển đổi màu nền của màn hình thông báo yêu cầu đăng nhập khi truy cập các tiện ích (như Tủ lạnh thông minh, ví VeggoPay, carbon point...) từ màu xám nhạt (`veggo_surface`) thành màu trắng tinh khiết (`@color/white`) để trang trông sáng và tinh giản hơn.
  * *Tệp ảnh hưởng:* [activity_login_required.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/layout/activity_login_required.xml)

### 8. Cấu hình bảo mật mạng (Network Configuration)
* **Cập nhật IP mạng LAN:** Bổ sung địa chỉ IP nội bộ mới của máy chủ phát triển phát sinh từ mạng LAN vào cấu hình bảo mật mạng để thiết bị thử nghiệm/máy ảo Android có thể kết nối thông suốt đến backend API mà không bị chặn lỗi bảo mật cleartext (HTTP).
  * *Tệp ảnh hưởng:* [network_security_config.xml](file:///d:/NAM3/Ky3/M_Commerce/VEGGO/app/src/main/res/xml/network_security_config.xml)

---
*Mọi thay đổi đã được xác thực, kiểm thử và biên dịch thành công mà không làm ảnh hưởng đến các tính năng cốt lõi khác của hệ thống.*
