# Book Store API - Spring Boot Backend

Đây là backend được chuyển đổi từ Node.js/Express sang Java Spring Boot.

## Cấu trúc dự án

```
bookstore/
├── src/main/java/com/hutech/bookstore/
│   ├── entity/          # JPA Entities (17 entities)
│   ├── repository/      # JPA Repositories (15 repositories)
│   ├── service/         # Service layer (business logic)
│   ├── controller/      # REST Controllers
│   ├── dto/            # Data Transfer Objects
│   ├── security/       # Spring Security configuration
│   ├── exception/      # Exception handling
│   └── util/          # Utility classes
└── src/main/resources/
    └── application.properties
```

## Đã hoàn thành

### 1. Entities (17 entities)
- ✅ Role
- ✅ User
- ✅ Category
- ✅ Book
- ✅ Cart & CartItem
- ✅ Order & OrderItem
- ✅ Address
- ✅ Favorite
- ✅ Voucher & VoucherUsage
- ✅ Payment
- ✅ ShippingProvider
- ✅ UserBook & DownloadHistory
- ✅ Message
- ✅ EmailVerification
- ✅ PasswordReset

### 2. Repositories
- ✅ Tất cả repository interfaces đã được tạo với các query methods cần thiết

### 3. Security & Authentication
- ✅ Spring Security configuration
- ✅ JWT authentication filter
- ✅ Password encoder (BCrypt)
- ✅ CORS configuration

### 4. Core Components
- ✅ ApiResponse utility class
- ✅ JwtUtil for token generation/validation
- ✅ GlobalExceptionHandler
- ✅ AppException custom exception
- ✅ Basic AuthService và AuthController

### 5. Configuration
- ✅ application.properties với đầy đủ cấu hình
- ✅ pom.xml với tất cả dependencies cần thiết

## Cần hoàn thiện

### Services (theo pattern của AuthService)
1. BookService - Quản lý sách
2. OrderService - Quản lý đơn hàng
3. CartService - Quản lý giỏ hàng
4. CategoryService - Quản lý danh mục
5. FavoriteService - Quản lý yêu thích
6. VoucherService - Quản lý voucher
7. PaymentService - Xử lý thanh toán
8. AddressService - Quản lý địa chỉ
9. UserService - Quản lý người dùng
10. MessageService - Quản lý tin nhắn
11. EmailService - Gửi email
12. FileUploadService - Upload file

### Controllers (theo pattern của AuthController)
1. BookController
2. OrderController
3. CartController
4. CategoryController
5. FavoriteController
6. VoucherController
7. PaymentController
8. AddressController
9. UserController
10. MessageController
11. UploadController
12. DownloadController
13. LibraryController
14. ReportController
15. ShippingProviderController

### DTOs
- Tạo các DTO classes cho request/response của từng controller

### Additional Features
- WebSocket configuration cho chat real-time
- Scheduled tasks cho cron jobs (order status updates)
- File storage service
- Email templates

## Cách chạy

1. Cấu hình database trong `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore
spring.datasource.username=root
spring.datasource.password=your_password
```

2. Chạy ứng dụng:
```bash
mvn spring-boot:run
```

3. API sẽ chạy tại: `http://localhost:5000`

## API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký
- `POST /api/auth/login` - Đăng nhập
- `GET /api/auth/me` - Lấy thông tin user hiện tại (cần authentication)

### Health Check
- `GET /api/health` - Kiểm tra server

## Pattern để tiếp tục phát triển

### 1. Tạo Service
```java
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    
    public Book createBook(Book book) {
        // Business logic
        return bookRepository.save(book);
    }
}
```

### 2. Tạo Controller
```java
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<Book>> createBook(@RequestBody Book book) {
        Book created = bookService.createBook(book);
        return ResponseEntity.ok(ApiResponse.success(created, "Book created"));
    }
}
```

### 3. Tạo DTO
```java
@Data
public class BookRequest {
    @NotBlank
    private String title;
    // ... other fields
}
```

## Lưu ý

1. Tất cả entities đều có `isDeleted` field để hỗ trợ soft delete
2. Sử dụng `@RequiredArgsConstructor` từ Lombok để inject dependencies
3. Sử dụng `ApiResponse` cho tất cả responses
4. Sử dụng `@Valid` để validate request bodies
5. JWT token được lưu trong header: `Authorization: Bearer <token>`

## Mapping từ Node.js sang Spring Boot

| Node.js | Spring Boot |
|---------|-------------|
| Express Router | @RestController + @RequestMapping |
| Mongoose Model | JPA Entity |
| Mongoose Schema | JPA Annotations |
| Model.find() | Repository methods |
| bcrypt | BCryptPasswordEncoder |
| jsonwebtoken | JwtUtil (jjwt) |
| express-validator | @Valid + Bean Validation |
| Middleware | Filter/Interceptor |
| async/await | @Transactional |

## Database

Ứng dụng sử dụng MySQL. Schema sẽ được tự động tạo từ entities khi chạy lần đầu (ddl-auto=update).

## Security

- JWT authentication
- Password encryption với BCrypt
- CORS enabled cho frontend
- Role-based access control (cần implement thêm)

