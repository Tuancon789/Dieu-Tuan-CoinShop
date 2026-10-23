# Điểu Tuấn Coin Shop v0.1 — Android DEMO

Đây là ứng dụng **mô phỏng đặt đơn nạp xu**, không phải dịch vụ nạp xu TikTok chính thức.

## Tính năng
- Giao diện tiếng Việt và English, đổi qua nút VI/EN
- Đăng ký/đăng nhập tài khoản riêng **trên thiết bị**
- Tài khoản đầu tiên trên thiết bị là ADMIN demo
- Chọn gói 30 / 100 / 200 / 500 / 1.000 xu, số lượng tùy chỉnh và nhập TikTok handle nhận xu (chỉ ghi chú)
- PayPal chỉ là lựa chọn **hiển thị**, không thu tiền
- Nút tạo đơn luôn báo **tạo đơn thử nghiệm thành công** và lưu lịch sử trên thiết bị
- ADMIN hiển thị số dư **$10,000,000.00 giả lập** và danh sách đơn local

## Giới hạn cực kỳ quan trọng
- **Không thu tiền PayPal, không có giao dịch, không giao xu TikTok.**
- Giao diện luôn ghi rõ DEMO và trạng thái SIMULATED để không khiến khách hiểu nhầm là đã thanh toán/nhận xu.
- Không có đồng bộ nhiều máy, không có backend và không có quyền quản trị an toàn để vận hành thương mại.
- Không lưu API key / tài khoản / mật khẩu trong repo.

## Build APK bằng GitHub Actions trên điện thoại
1. Mở tab **Actions** của repository.
2. Chọn **Build APK Coin Shop** -> **Run workflow** (hoặc mở workflow vừa chạy do push).
3. Nếu build thành công (dấu xanh), nhấn workflow run rồi mục **Artifacts**.
4. Tải **DieuTuan-CoinShop-v0.1-debug**; giải nén để lấy **app-debug.apk**.
5. Nếu build đỏ, mở log của bước Build debug APK để xem lỗi.

Cấu hình: Gradle 8.9, Android Gradle Plugin 8.7.3, Java 17, compileSdk 35, minSdk 24, targetSdk 34.

Mã nguồn: app/src/main/java/vn/dieutuan/coinshop/MainActivity.java. Chỉ dùng Android SDK gốc, không có thư viện ngoài. Đây là phiên bản nguyên mẫu để kiểm thử trước khi bổ sung chức năng thật hợp pháp.
