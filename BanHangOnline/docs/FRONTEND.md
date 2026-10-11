# Chạy giao diện Marketplace

## Chạy ứng dụng

Giao diện được viết bằng HTML, CSS và JavaScript thuần, đặt trong
`src/main/resources/static` và được Spring Boot phục vụ cùng origin với API.
Không cần cài Node.js hoặc chạy máy chủ frontend riêng.

Thiết lập biến môi trường cho Backend trước khi chạy:

- `DB_URL`: URL PostgreSQL của cơ sở dữ liệu ứng dụng.
- `DB_USERNAME`: tài khoản PostgreSQL.
- `DB_PASSWORD`: mật khẩu PostgreSQL.
- `JWT_SECRET_BASE64`: khóa HMAC ở dạng Base64, giải mã thành ít nhất 32 byte.
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`,
  `SMTP_STARTTLS` và `SMTP_FROM`: cấu hình email khôi phục mật khẩu.
- `FRONTEND_BASE_URL`: URL gốc dùng trong liên kết đặt lại mật khẩu; mặc định
  là `http://localhost:8080`.

Không đưa mật khẩu database hoặc JWT secret vào mã nguồn hay frontend.
Có thể tạo khóa dùng riêng cho môi trường phát triển bằng PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($bytes)
$rng.Dispose()
```

Tại thư mục dự án, chạy:

```powershell
.\gradlew.bat bootRun
```

Mở `http://localhost:8080/`. Nếu giao diện/API được triển khai ở origin khác,
đặt giá trị `<meta name="api-base">` trong `index.html` thành URL gốc của API
và cấu hình CORS tại Backend cho đúng origin.

## Kiểm thử luồng chính

1. Đăng ký tài khoản khách hàng, sau đó đăng nhập.
2. Tìm sản phẩm theo tên, danh mục hoặc khoảng giá; sắp xếp và phân trang kết quả,
   rồi mở cửa hàng và chi tiết sản phẩm.
3. Thêm sản phẩm vào giỏ, cập nhật số lượng, đặt hàng và xem lịch sử đơn.
4. Tạo yêu cầu thanh toán để kiểm tra trạng thái chờ xử lý.
5. Đăng nhập bằng chủ cửa hàng để quản lý sản phẩm, đơn hàng, nhân viên và giao dịch.
6. Đăng nhập bằng nhân viên được phân công để xem sản phẩm và các đơn được giao.
7. Đăng nhập bằng ADMIN để lọc tài khoản, cửa hàng, đơn, thanh toán, quản lý
   danh mục nhiều cấp và mở khóa tài khoản.
8. Đăng xuất rồi đăng nhập tài khoản khác; dữ liệu riêng tư được tải lại từ API và token chỉ được giữ trong `sessionStorage`.
9. Cấu hình SMTP, yêu cầu khôi phục mật khẩu bằng email và dùng liên kết trước
   khi hết hạn sau 30 phút; sau đó thử đổi mật khẩu từ hồ sơ cá nhân.

Backend chưa có API dashboard thống kê, đổi email, upload ảnh, xác nhận trạng
thái thanh toán, cổng thanh toán thật, API quản trị mọi sản phẩm hoặc API quản
trị đánh giá. Giao diện không tạo số liệu hoặc giả lập các chức năng này.

Yêu cầu khôi phục trả cùng thông báo cho tài khoản không tồn tại, bị vô hiệu
hóa hoặc đã vượt giới hạn; email được gửi bất đồng bộ sau khi token đã lưu.
Nếu SMTP chưa cấu hình, Backend báo dịch vụ chưa khả dụng. Tạo cửa hàng làm
thay đổi quyền trong Backend; tài khoản khách hàng cần đăng nhập lại để nhận
JWT có vai trò SHOP_OWNER mới.

## Khóa tài khoản

Sau 5 lần đăng nhập sai liên tiếp, Backend khóa tài khoản. ADMIN đăng nhập,
mở **Khu vực quản lý → Tài khoản bị khóa**, rồi chọn **Mở khóa**. Backend đặt
lại bộ đếm và lưu người mở khóa cùng thời điểm trong `lich_su_mo_khoa_tai_khoan`.

Các trường khóa tài khoản, bảng lịch sử mở khóa và bảng token đặt lại mật khẩu
được khai báo bằng mapping JPA. Cấu hình hiện tại dùng
`spring.jpa.hibernate.ddl-auto=update`, vì vậy Hibernate sẽ thực hiện thay đổi
schema bổ sung khi ứng dụng khởi động. Hãy sao lưu database và kiểm tra schema
trước khi áp dụng ở môi trường có dữ liệu quan trọng.
