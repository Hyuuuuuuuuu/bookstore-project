# Hướng dẫn sử dụng Postman Collection

## Import Collection vào Postman

### Bước 1: Import Collection
1. Mở Postman
2. Click **Import** button (góc trên bên trái)
3. Chọn file `BookStore_API.postman_collection.json`
4. Click **Import**

### Bước 2: Import Environment (Optional nhưng khuyến nghị)
1. Click **Import** button
2. Chọn file `BookStore_Environment.postman_environment.json`
3. Click **Import**
4. Chọn environment "Book Store - Development" từ dropdown ở góc trên bên phải

## Cấu trúc Collection

Collection được tổ chức thành các folders:

1. **Authentication** - Tất cả endpoints liên quan đến authentication
2. **Books** - Quản lý sách (CRUD, search, filter)
3. **Categories** - Quản lý danh mục
4. **Cart** - Giỏ hàng
5. **Orders** - Đơn hàng
6. **Favorites** - Sách yêu thích
7. **Addresses** - Địa chỉ giao hàng
8. **Vouchers** - Mã giảm giá
9. **Payments** - Thanh toán (VNPay, Momo)
10. **Library** - Thư viện sách đã mua
11. **Users** - Quản lý người dùng
12. **Health & Info** - Health check và thông tin API

## Environment Variables

Collection sử dụng các biến môi trường:

- `{{base_url}}` - Base URL của API (mặc định: http://localhost:5000)
- `{{auth_token}}` - JWT token (tự động set sau khi login)
- `{{user_id}}` - User ID (tự động set sau khi login)

## Cách sử dụng

### 1. Đăng ký và Đăng nhập

1. Chạy request **Register** trong folder Authentication
2. Sau khi đăng ký thành công, chạy request **Login**
3. Token sẽ tự động được lưu vào biến `{{auth_token}}` nhờ Test Script

### 2. Test các endpoints khác

Sau khi có token, bạn có thể test các endpoints khác:
- Tất cả endpoints trong folder **Books**, **Cart**, **Orders**, etc. sẽ tự động sử dụng token từ `{{auth_token}}`

### 3. Test với Admin role

Để test các endpoints yêu cầu admin:
1. Đăng ký một user mới
2. Cập nhật role của user đó thành "admin" trong database
3. Login với user đó để lấy admin token
4. Hoặc set `{{admin_token}}` thủ công trong environment

## Lưu ý

1. **Base URL**: Đảm bảo backend đang chạy tại `http://localhost:5000`
2. **Token**: Token sẽ tự động được lưu sau khi login thành công
3. **Variables**: Có thể thay đổi các giá trị trong environment variables
4. **Test Data**: Các request đã có sẵn test data, bạn có thể chỉnh sửa theo nhu cầu

## Troubleshooting

### Lỗi 401 Unauthorized
- Kiểm tra token có được set chưa
- Thử login lại để lấy token mới
- Kiểm tra token có hết hạn chưa

### Lỗi 403 Forbidden
- Endpoint yêu cầu admin role
- Kiểm tra user có đúng role không

### Lỗi Connection Refused
- Kiểm tra backend có đang chạy không
- Kiểm tra port có đúng không (mặc định 5000)
- Kiểm tra `{{base_url}}` trong environment

## Cập nhật Collection

Khi có thêm endpoints mới, bạn có thể:
1. Thêm request mới vào folder tương ứng
2. Sử dụng `{{base_url}}` và `{{auth_token}}` trong URL và headers
3. Export lại collection để chia sẻ

