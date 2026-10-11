# Kiểm thử xác thực JWT bằng Postman

## Khởi chạy ứng dụng

Ứng dụng yêu cầu `JWT_SECRET_BASE64` được cấp qua môi trường. Không đặt khóa JWT trong file cấu hình được commit.
Thông tin kết nối PostgreSQL cũng được cấp qua `DB_URL`, `DB_USERNAME` và `DB_PASSWORD`; không lưu mật khẩu database trong source.

Trong PowerShell, tạo khóa ngẫu nhiên và chạy ứng dụng trong cùng cửa sổ:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/marketplace_db"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = Read-Host "PostgreSQL password"
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
.\gradlew.bat bootRun
```

Lệnh trên tạo khóa mới cho môi trường phát triển cục bộ; JWT cũ sẽ hết hiệu lực sau khi khởi động lại. Môi trường triển khai cần lưu một khóa ổn định trong secret manager hoặc biến môi trường được bảo vệ.

Tạo Postman Environment với `baseUrl` là `http://localhost:8080`.

## Đăng ký tài khoản khách

Gửi `POST {{baseUrl}}/api/users/register` với body JSON:

```json
{
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "password": "StrongPassword123!",
  "phone": "0900000000"
}
```

API luôn tạo role `CUSTOMER`; request không thể tự chỉ định role hoặc trạng thái tài khoản. Mật khẩu được lưu bằng BCrypt.

## Đăng nhập

Gửi `POST {{baseUrl}}/api/auth/login` với body JSON:

```json
{
  "email": "a@example.com",
  "password": "StrongPassword123!"
}
```

Response chứa `accessToken`, `tokenType` (`Bearer`), thời hạn và hồ sơ người dùng. Lưu token bằng tab **Tests** của Postman:

```javascript
pm.environment.set("accessToken", pm.response.json().accessToken);
```

Trong các request được bảo vệ, chọn **Authorization → Bearer Token** và đặt giá trị `{{accessToken}}`. Ví dụ `GET {{baseUrl}}/api/users/me`. Thiếu/sai/hết hạn token trả `401`; đăng nhập thành công nhưng thiếu quyền trả `403`.

## Endpoint công khai và endpoint cần xác thực

`POST /api/users/register`, `POST /api/auth/login` và các GET catalog (danh mục, sản phẩm, cửa hàng, đánh giá sản phẩm) là công khai. `GET /api/products/low-stock` và các thao tác quản lý danh mục chỉ dành cho ADMIN. Tạo shop dành cho CUSTOMER/SHOP_OWNER; sửa shop chỉ SHOP_OWNER. Thao tác nhân viên và tạo giao dịch shop chỉ SHOP_OWNER; nghiệp vụ shop khác dành cho SHOP_OWNER/EMPLOYEE. Hồ sơ, giỏ hàng, đơn, thanh toán và đánh giá cá nhân dành cho CUSTOMER. Service tiếp tục kiểm tra chủ shop, nhân viên, chủ đơn/giỏ hàng/đánh giá để chặn truy cập chéo dữ liệu.

Vai trò được đọc lại từ database khi xác minh mỗi JWT. Đăng ký chỉ cấp `CUSTOMER`; tạo shop sẽ chuyển khách hàng thành `SHOP_OWNER`, thêm nhân viên sẽ chuyển `CUSTOMER` thành `EMPLOYEE`. Không có API công khai để cấp `ADMIN`. Cấp ADMIN lần đầu bằng quy trình quản trị database được kiểm soát, ví dụ:

```sql
UPDATE nguoi_dung
SET vai_tro = 'ADMIN'
WHERE dia_chi_email = 'admin@example.com';
```

Sau khi đổi vai trò, token cũ sẽ sử dụng quyền mới ở request kế tiếp; token vẫn hết hạn theo thời gian đã cấu hình.

## Trạng thái đơn hàng nhiều shop

`Order.status` tổng hợp tiến độ fulfillment từ các `ShopOrder.status`, không biểu diễn thanh toán. Nếu mọi shop bị hủy, đơn tổng là `CANCELLED`; nếu chỉ một phần bị hủy, là `PARTIALLY_CANCELLED`; nếu chưa shop nào bị hủy, đơn tổng theo shop có tiến độ chậm nhất (`PENDING` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED`). Xem trạng thái chi tiết từng shop trong danh sách `shopOrders`.

`Payment.status` độc lập với trạng thái xử lý đơn. Backend hiện chỉ tạo payment ở trạng thái `PENDING`; chưa có tích hợp callback xác nhận/thất bại từ cổng thanh toán. Vì chưa có luồng hủy giao dịch/hoàn tiền, không thể hủy đơn đã có bản ghi thanh toán. Hủy đơn shop trước `SHIPPED` sẽ hoàn kho cho riêng shop đó.

Các truy vấn quản trị đã nối qua service và chỉ ADMIN truy cập dưới `/api/admin`: `GET /users?role=&enabled=`, `GET /orders?paymentStatus=`, `GET /orders/by-number/{orderNumber}`, `GET /payments?status=`, `GET /shops?status=`, `GET /categories/roots`, `GET /categories/{categoryId}/children` và `GET /categories/search?name=`. Chủ shop/nhân viên dùng `GET /api/shops/{shopId}/manage/products` để xem cả sản phẩm đã ngừng bán; `GET /api/shops/{shopId}/staff?active=false` dành cho chủ shop. Tài khoản khách có thể lọc lịch sử đơn bằng `GET /api/orders?status=` hoặc tìm theo mã bằng `/api/orders/by-number/{orderNumber}`. Truy vấn của shop được lọc theo shop trước khi trả dữ liệu.
