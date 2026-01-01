# BookStore API Documentation

Complete API documentation for BookStore application.

**Base URL:** `http://localhost:5000`

**Authentication:** Most endpoints require JWT token in Authorization header:
```
Authorization: Bearer <token>
```

---

## Table of Contents

1. [Health & Root](#health--root)
2. [Authentication](#authentication)
3. [Books](#books)
4. [Categories](#categories)
5. [Favorites](#favorites)
6. [Cart](#cart)
7. [Orders](#orders)
8. [Payments](#payments)
9. [Shipping Providers](#shipping-providers)
10. [Vouchers](#vouchers)

---

## Health & Root

### Health Check
```
GET /api/health
```
**Description:** Check if server is running

**Response:**
```json
{
  "statusCode": 200,
  "data": {
    "success": true,
    "message": "Server is running",
    "timestamp": "2025-12-23T10:00:00"
  },
  "message": "Server is running"
}
```

### Root - API Info
```
GET /
```
**Description:** Get API information and available endpoints

**Response:**
```json
{
  "statusCode": 200,
  "data": {
    "success": true,
    "message": "BookStore API Server",
    "version": "1.0.0",
    "timestamp": "2025-12-23T10:00:00",
    "endpoints": {
      "health": "/api/health",
      "auth": "/api/auth",
      "users": "/api/users",
      "books": "/api/books",
      "orders": "/api/orders",
      "cart": "/api/cart",
      "favorites": "/api/favorites"
    }
  }
}
```

---

## Authentication

### Register
```
POST /api/auth/register
```
**Description:** Register a new user

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "phone": "0123456789",
  "address": "123 Main St"
}
```

**Response:** `201 Created`

---

### Login
```
POST /api/auth/login
```
**Description:** Login and get JWT token

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "user": {
      "id": 1,
      "name": "John Doe",
      "email": "user@example.com",
      "role": "user",
      "avatar": null,
      "phone": "0123456789",
      "address": "123 Main St",
      "isEmailVerified": true
    },
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "message": "Login successful"
}
```

---

### Get Current User
```
GET /api/auth/me
```
**Description:** Get current authenticated user profile

**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Send Verification Code
```
POST /api/auth/send-verification-code
```
**Request Body:**
```json
{
  "email": "user@example.com",
  "name": "User Name"
}
```

---

### Verify Email
```
POST /api/auth/verify-email
```
**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

---

### Register with Verification
```
POST /api/auth/register-with-verification
```
**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "phone": "0123456789",
  "address": "123 Main St",
  "verificationCode": "123456"
}
```

---

### Forgot Password
```
POST /api/auth/forgot-password
```
**Request Body:**
```json
{
  "email": "user@example.com"
}
```

---

### Verify Reset OTP
```
POST /api/auth/verify-reset-otp
```
**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

---

### Reset Password
```
POST /api/auth/reset-password
```
**Request Body:**
```json
{
  "email": "user@example.com",
  "code": "123456",
  "password": "newpassword123"
}
```

---

### Change Password
```
PUT /api/auth/change-password
```
**Headers:**
- `Authorization: Bearer <token>`

**Request Body:**
```json
{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword123"
}
```

---

## Books

### Get All Books
```
GET /api/books
```
**Description:** Get paginated list of books with filters

**Query Parameters:**
- `page` (optional): Page number (default: 1)
- `limit` (optional): Items per page (default: 18)
- `search` (optional): Search by title/author
- `categoryId` (optional): Filter by category
- `minPrice` (optional): Minimum price
- `maxPrice` (optional): Maximum price
- `stock` (optional): Filter by stock status (`inStock`, `outOfStock`, `all`)
- `sortBy` (optional): Sort field (default: `createdAt`)
- `sortOrder` (optional): Sort order `asc` or `desc` (default: `desc`)

**Example:**
```
GET /api/books?page=1&limit=18&sortBy=createdAt&sortOrder=desc&stock=inStock
```

**Response:** `200 OK`

---

### Get Book by ID
```
GET /api/books/:id
```
**Description:** Get book details by ID

**Response:** `200 OK`

---

## Categories

### Get All Categories
```
GET /api/categories
```
**Query Parameters:**
- `sortBy` (optional): Sort field (default: `name`)
- `sortOrder` (optional): Sort order `asc` or `desc` (default: `asc`)

**Response:** `200 OK`

---

### Get Category by ID
```
GET /api/categories/:id
```
**Response:** `200 OK`

---

## Favorites

All favorites endpoints require authentication.

### Get Favorites
```
GET /api/favorites
```
**Headers:**
- `Authorization: Bearer <token>`

**Description:** Get all favorite books of current user

**Response:** `200 OK`

---

### Add to Favorites
```
POST /api/favorites/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Remove from Favorites
```
DELETE /api/favorites/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Check Favorite
```
GET /api/favorites/check/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "isFavorite": true
  },
  "message": "Favorite status retrieved successfully"
}
```

---

## Cart

All cart endpoints require authentication.

### Get Cart
```
GET /api/cart
```
**Headers:**
- `Authorization: Bearer <token>`

**Description:** Get current user's cart

**Response:** `200 OK`

---

### Add to Cart
```
POST /api/cart/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "quantity": 1
}
```

**Response:** `200 OK`

---

### Update Cart Item
```
PUT /api/cart/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "quantity": 2
}
```

**Response:** `200 OK`

---

### Remove from Cart
```
DELETE /api/cart/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Clear Cart
```
DELETE /api/cart
```
**Headers:**
- `Authorization: Bearer <token>`

**Description:** Remove all items from cart

**Response:** `200 OK`

---

### Check Cart Item
```
GET /api/cart/check/:bookId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "inCart": true,
    "quantity": 2
  },
  "message": "Cart item status retrieved successfully"
}
```

---

## Orders

All order endpoints require authentication.

### Create Order
```
POST /api/orders
```
**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "shippingAddressId": 1,
  "shippingProviderId": 1,
  "paymentMethod": "cod",
  "voucherCode": null,
  "note": "Giao hàng vào buổi sáng",
  "items": [
    {
      "bookId": 1,
      "quantity": 2
    },
    {
      "bookId": 2,
      "quantity": 1
    }
  ]
}
```

**Response:** `201 Created`

---

### Get My Orders
```
GET /api/orders/my-orders
```
**Headers:**
- `Authorization: Bearer <token>`

**Query Parameters:**
- `page` (optional): Page number
- `limit` (optional): Items per page
- `status` (optional): Filter by order status

**Response:** `200 OK`

---

### Get All Orders (Admin)
```
GET /api/orders
```
**Headers:**
- `Authorization: Bearer <token>`

**Query Parameters:**
- `page` (optional): Page number
- `limit` (optional): Items per page
- `status` (optional): Filter by order status
- `userId` (optional): Filter by user ID

**Response:** `200 OK`

---

### Get Order by ID
```
GET /api/orders/:orderId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Cancel Order
```
PATCH /api/orders/:orderId/cancel
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Update Order Status (Admin)
```
PATCH /api/orders/admin/:orderId/status
```
**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "status": "CONFIRMED"
}
```

**Response:** `200 OK`

---

## Payments

### Get Payment Methods
```
GET /api/payments/methods
```
**Description:** Get available payment methods (Public endpoint)

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": [
    {
      "id": "cod",
      "name": "Thanh toán khi nhận hàng (COD)",
      "description": "Thanh toán bằng tiền mặt khi nhận hàng",
      "icon": "cash",
      "enabled": true
    },
    {
      "id": "vnpay",
      "name": "VNPay",
      "description": "Thanh toán qua VNPay",
      "icon": "vnpay",
      "enabled": true
    }
  ],
  "message": "Payment methods retrieved successfully"
}
```

---

### Create COD Payment
```
POST /api/payments/cod
```
**Headers:**
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

**Request Body:**
```json
{
  "orderId": 1,
  "amount": 500000,
  "description": "Payment for order #1"
}
```

**Response:** `201 Created`

---

### Get All Payments (Admin)
```
GET /api/payments
```
**Headers:**
- `Authorization: Bearer <token>`

**Query Parameters:**
- `page` (optional): Page number
- `limit` (optional): Items per page
- `status` (optional): Filter by payment status
- `method` (optional): Filter by payment method

**Response:** `200 OK`

---

### Get Payment by ID
```
GET /api/payments/:paymentId
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

### Get Payment by Transaction Code
```
GET /api/payments/transaction/:transactionCode
```
**Headers:**
- `Authorization: Bearer <token>`

**Response:** `200 OK`

---

## Shipping Providers

### Get Active Shipping Providers
```
GET /api/shipping-providers/active
```
**Description:** Get all active shipping providers (Public endpoint)

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "providers": [
      {
        "_id": 1,
        "name": "Giao Hàng Nhanh",
        "code": "GHN",
        "baseFee": 25000.0,
        "estimatedTime": "2-3 ngày",
        "active": true,
        "description": "Dịch vụ giao hàng nhanh chóng và tin cậy",
        "contactInfo": {
          "phone": "1900 1234",
          "email": "support@ghn.vn",
          "website": "https://ghn.vn"
        },
        "isDeleted": false,
        "createdAt": "2025-12-23T09:00:00",
        "updatedAt": "2025-12-23T09:00:00"
      }
    ],
    "total": 4
  },
  "message": "Active shipping providers retrieved successfully"
}
```

---

### Get All Shipping Providers (Admin)
```
GET /api/shipping-providers
```
**Headers:**
- `Authorization: Bearer <token>`

**Description:** Get all shipping providers including inactive ones (Admin only)

**Response:** `200 OK`

---

### Get Shipping Provider by ID
```
GET /api/shipping-providers/:id
```
**Description:** Get shipping provider details by ID (Public endpoint)

**Response:** `200 OK`

---

### Get Shipping Provider by Code
```
GET /api/shipping-providers/code/:code
```
**Description:** Get shipping provider details by code (Public endpoint)

**Example:**
```
GET /api/shipping-providers/code/GHN
```

**Response:** `200 OK`

---

## Vouchers

### Get Available Vouchers

Lấy danh sách voucher có thể áp dụng cho đơn hàng. Endpoint này sẽ filter voucher dựa trên:
- Tổng tiền đơn hàng (minOrderAmount)
- Danh sách categories trong đơn hàng (applicableCategories)
- Danh sách books trong đơn hàng (applicableBooks)
- User ID (applicableUsers và kiểm tra đã dùng chưa)

**Endpoint:** `GET /api/vouchers/available`

**Authentication:** Optional (nếu có token sẽ kiểm tra user đã dùng voucher chưa)

**Query Parameters:**
- `orderAmount` (optional): Tổng tiền đơn hàng
- `categoryIds` (optional): Danh sách category IDs, phân cách bằng dấu phẩy (ví dụ: "1,2,3")
- `bookIds` (optional): Danh sách book IDs, phân cách bằng dấu phẩy (ví dụ: "1,2,3")

**Example:**
```
GET /api/vouchers/available?orderAmount=500000&categoryIds=1,2&bookIds=1,2,3
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "vouchers": [
      {
        "_id": 1,
        "code": "VOUCHER123",
        "name": "Giảm 10% cho đơn hàng trên 500k",
        "description": "Áp dụng cho đơn hàng từ 500,000đ",
        "type": "PERCENTAGE",
        "value": 10.0,
        "minOrderAmount": 500000.0,
        "maxDiscountAmount": 50000.0,
        "usageLimit": 100,
        "usedCount": 25,
        "validFrom": "2025-01-01T00:00:00",
        "validTo": "2025-12-31T23:59:59",
        "isActive": true,
        "applicableCategoryIds": [1, 2],
        "applicableBookIds": null,
        "applicableUserIds": null,
        "isDeleted": false,
        "createdAt": "2025-01-01T00:00:00",
        "updatedAt": "2025-01-01T00:00:00"
      }
    ],
    "total": 1
  },
  "message": "Available vouchers retrieved successfully",
  "timestamp": "2025-12-23T10:00:00"
}
```

---

### Get All Valid Vouchers

Lấy tất cả voucher hợp lệ (active, chưa hết hạn, chưa đạt usage limit). Không filter theo điều kiện đơn hàng.

**Endpoint:** `GET /api/vouchers`

**Authentication:** Not required (Public)

**Example:**
```
GET /api/vouchers
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "vouchers": [ ... ],
    "total": 5
  },
  "message": "Valid vouchers retrieved successfully",
  "timestamp": "2025-12-23T10:00:00"
}
```

---

### Get Voucher by Code

Lấy thông tin voucher theo code.

**Endpoint:** `GET /api/vouchers/code/:code`

**Authentication:** Not required (Public)

**Path Parameters:**
- `code`: Mã voucher

**Example:**
```
GET /api/vouchers/code/VOUCHER123
```

**Response:** `200 OK`
```json
{
  "statusCode": 200,
  "data": {
    "_id": 1,
    "code": "VOUCHER123",
    "name": "Giảm 10% cho đơn hàng trên 500k",
    "description": "Áp dụng cho đơn hàng từ 500,000đ",
    "type": "PERCENTAGE",
    "value": 10.0,
    "minOrderAmount": 500000.0,
    "maxDiscountAmount": 50000.0,
    "usageLimit": 100,
    "usedCount": 25,
    "validFrom": "2025-01-01T00:00:00",
    "validTo": "2025-12-31T23:59:59",
    "isActive": true,
    "applicableCategoryIds": [1, 2],
    "applicableBookIds": null,
    "applicableUserIds": null,
    "isDeleted": false,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "message": "Voucher retrieved successfully",
  "timestamp": "2025-12-23T10:00:00"
}
```

**Error Response:** `404 Not Found`
```json
{
  "statusCode": 404,
  "message": "Voucher not found",
  "timestamp": "2025-12-23T10:00:00"
}
```

---

## Response Format

All API responses follow this format:

```json
{
  "statusCode": 200,
  "data": { ... },
  "message": "Success message",
  "timestamp": "2025-12-23T10:00:00"
}
```

## Error Format

```json
{
  "statusCode": 400,
  "message": "Error message",
  "timestamp": "2025-12-23T10:00:00"
}
```

---

## Import Postman Collection

Import the `API_COLLECTION.json` file into Postman to test all endpoints easily.

1. Open Postman
2. Click "Import"
3. Select `API_COLLECTION.json`
4. Set the `baseUrl` variable to your server URL
5. Login first to get the token (it will be automatically saved)
6. Test all endpoints!

---

**Last Updated:** 2025-12-23

