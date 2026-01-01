import mongoose from 'mongoose'
import dotenv from 'dotenv'
import bcrypt from 'bcrypt'

// Load environment variables
dotenv.config()

// Import models
import Role from '~/models/roleModel'
import User from '~/models/userModel'
import Category from '~/models/categoryModel'
import Book from '~/models/bookModel'
import Order from '~/models/orderModel'
import OrderItem from '~/models/orderItemModel'
import Favorite from '~/models/favoriteModel'
import Voucher from '~/models/voucherModel'
import VoucherUsage from '~/models/voucherUsageModel'
import Message from '~/models/messageModel'
import Address from '~/models/addressModel'
import Cart from '~/models/cartModel'
import roleService from '~/services/roleService'
import UserBook from '~/models/userBookModel'
import EmailVerification from '~/models/emailVerificationModel'
import PasswordReset from '~/models/passwordResetModel'
import ShippingProvider from '~/models/shippingProviderModel'
import Payment from '~/models/paymentModel'

// Connect to database
const connectDB = async () => {
  try {
    // Use MONGO_URI with authentication if available, otherwise use default
    const mongoUri = process.env.MONGO_URI || 'mongodb://admin:password123@localhost:27017/bookstore?authSource=admin'
    console.log('🔗 Connecting to MongoDB with URI:', mongoUri)
    await mongoose.connect(mongoUri)
    console.log('✅ Connected to MongoDB for seeding')
  } catch (error) {
    console.error('❌ Database connection error:', error.message)
    process.exit(1)
  }
}

// Sample data - Categories, Books, và Vouchers bằng tiếng Việt
const sampleCategories = [
  { name: "Tiểu thuyết", description: "Các tác phẩm văn học mang tính hư cấu, cảm xúc và chiều sâu tâm lý." },
  { name: "Lịch sử - Văn hóa", description: "Sách ghi lại các sự kiện, văn hóa và truyền thống dân tộc." },
  { name: "Khoa học", description: "Kiến thức về tự nhiên, vật lý, sinh học, vũ trụ và nghiên cứu khoa học." },
  { name: "Công nghệ thông tin", description: "Sách về lập trình, AI, mạng, và công nghệ số." },
]

// Generate books for each category (8 books per category = 32 total)
const generateBooksForCategory = (category, categoryIndex) => {
  const books = []
  const formats = ['paperback', 'hardcover'] // Physical books only for seed
  const publishers = [
    'NXB Kim Đồng',
    'NXB Trẻ',
    'NXB Văn Học',
    'NXB Giáo Dục',
    'NXB Tổng Hợp',
    'NXB Thế Giới',
    'NXB Hội Nhà Văn',
    'NXB Đại Học Quốc Gia'
  ]
  
  for (let j = 1; j <= 8; j++) {
    const format = formats[Math.floor(Math.random() * formats.length)]
    const isDigital = false // All physical books for this seed
    
    // Generate publication date
    const year = 2020 + Math.floor(Math.random() * 4)
    const month = Math.floor(1 + Math.random() * 8)
    const day = Math.floor(1 + Math.random() * 28)
    
    const book = {
      title: `${category.name} Tập ${j}`,
      author: `Tác giả ${category.name} ${j}`,
      description: `Cuốn sách ${category.name.toLowerCase()} tập ${j} mang đến nội dung hấp dẫn, phù hợp với độc giả yêu thích thể loại này.`,
      price: 50000 + Math.floor(Math.random() * 150000),
      stock: 10 + Math.floor(Math.random() * 90),
      imageUrl: `https://placehold.co/400x600?text=${encodeURIComponent(category.name + ' ' + j)}`,
      isbn: `978-${categoryIndex}${j}${Math.floor(Math.random() * 1000000)}`,
      publisher: publishers[Math.floor(Math.random() * publishers.length)],
      publicationDate: new Date(year, month - 1, day),
      pages: 150 + Math.floor(Math.random() * 400),
      format: format,
      dimensions: "20x15x3",
      weight: 300 + Math.floor(Math.random() * 500),
      fileUrl: '',
      viewCount: Math.floor(Math.random() * 1000),
      isActive: true,
      status: 'available'
    }
    
    books.push(book)
  }
  return books
}

// Generate all books (8 per category = 32 total)
const sampleBooks = []
sampleCategories.forEach((category, index) => {
  sampleBooks.push(...generateBooksForCategory(category, index))
})

// Thêm dữ liệu mới cho các model khác
const sampleUsers = [
  {
    name: 'Admin User',
    fullName: 'Nguyễn Văn Admin',
    email: 'admin@bookstore.com',
    password: 'admin123',
    phone: '0323456789',
    address: '123 Admin Street, Ho Chi Minh City',
    isEmailVerified: true,
    status: 'active',
    isActive: true
  },
  {
    name: 'Staff User',
    fullName: 'Lê Văn Staff',
    email: 'staff@bookstore.com',
    password: 'staff123',
    phone: '0123456789',
    address: '789 Staff Road, Ho Chi Minh City',
    isEmailVerified: true,
    status: 'active',
    isActive: true
  },
  {
    name: 'Regular User',
    fullName: 'Trần Thị User',
    email: 'user@bookstore.com',
    password: 'user123',
    phone: '0987654321',
    address: '456 User Avenue, Ho Chi Minh City',
    isEmailVerified: true,
    status: 'active',
    isActive: true
  },
  {
    name: 'Test User',
    fullName: 'Lê Văn Test',
    email: 'test@bookstore.com',
    password: 'test123',
    phone: '0369852147',
    address: '789 Test Road, Ho Chi Minh City',
    isEmailVerified: false,
    status: 'pending',
    isActive: false
  }
]

// Address seed data
const sampleAddresses = [
  {
    name: 'Nguyễn Văn Admin',
    phone: '0323456789',
    address: '123 Đường Admin',
    city: 'TP. Hồ Chí Minh',
    district: 'Quận 1',
    ward: 'Phường Bến Nghé',
    isDefault: true
  },
  {
    name: 'Trần Thị User',
    phone: '0987654321',
    address: '456 Đường User',
    city: 'TP. Hồ Chí Minh',
    district: 'Quận 2',
    ward: 'Phường Thủ Thiêm',
    isDefault: true
  },
  {
    name: 'Lê Văn Test',
    phone: '0369852147',
    address: '789 Đường Test',
    city: 'TP. Hồ Chí Minh',
    district: 'Quận 3',
    ward: 'Phường Võ Thị Sáu',
    isDefault: true
  },
  {
    name: 'Phạm Thị D',
    phone: '0912345678',
    address: '321 Đường GHI',
    city: 'TP. Hồ Chí Minh',
    district: 'Quận 7',
    ward: 'Phường Tân Phú',
    isDefault: false
  },
  {
    name: 'Hoàng Văn E',
    phone: '0987654321',
    address: '654 Đường JKL',
    city: 'TP. Hồ Chí Minh',
    district: 'Quận 10',
    ward: 'Phường 15',
    isDefault: false
  }
]

const sampleOrders = [
  {
    totalPrice: 125000,
    originalAmount: 125000,
    discountAmount: 0,
    paymentMethod: 'cod',
    status: 'pending',
    paymentStatus: 'pending',
    shippingAddressId: null // Will be set to first address ID
  },
  {
    totalPrice: 200000,
    originalAmount: 210000,
    discountAmount: 10000,
    paymentMethod: 'momo',
    status: 'shipped',
    paymentStatus: 'completed',
    transactionId: 'TXN001',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to second address ID
  },
  {
    totalPrice: 350000,
    originalAmount: 375000,
    discountAmount: 25000,
    paymentMethod: 'bank_transfer',
    status: 'delivered',
    paymentStatus: 'completed',
    transactionId: 'TXN002',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to third address ID
  },
  {
    totalPrice: 180000,
    originalAmount: 180000,
    discountAmount: 0,
    paymentMethod: 'zalopay',
    status: 'confirmed',
    paymentStatus: 'completed',
    transactionId: 'TXN003',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to fourth address ID
  },
  {
    totalPrice: 95000,
    originalAmount: 100000,
    discountAmount: 5000,
    paymentMethod: 'cod',
    status: 'cancelled',
    paymentStatus: 'refunded',
    shippingAddressId: null // Will be set to fifth address ID
  },
  {
    totalPrice: 420000,
    originalAmount: 450000,
    discountAmount: 30000,
    paymentMethod: 'momo',
    status: 'digital_delivered',
    paymentStatus: 'completed',
    transactionId: 'TXN004',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to sixth address ID
  },
  {
    totalPrice: 275000,
    originalAmount: 290000,
    discountAmount: 15000,
    paymentMethod: 'bank_transfer',
    status: 'pending',
    paymentStatus: 'pending',
    shippingAddressId: null // Will be set to seventh address ID
  },
  {
    totalPrice: 165000,
    originalAmount: 165000,
    discountAmount: 0,
    paymentMethod: 'zalopay',
    status: 'shipped',
    paymentStatus: 'completed',
    transactionId: 'TXN005',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to eighth address ID
  },
  {
    totalPrice: 320000,
    originalAmount: 340000,
    discountAmount: 20000,
    paymentMethod: 'bank_transfer',
    status: 'confirmed',
    paymentStatus: 'completed',
    transactionId: 'TXN006',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to ninth address ID
  },
  {
    totalPrice: 75000,
    originalAmount: 75000,
    discountAmount: 0,
    paymentMethod: 'cod',
    status: 'pending',
    paymentStatus: 'pending',
    shippingAddressId: null // Will be set to tenth address ID
  },
  {
    totalPrice: 480000,
    originalAmount: 520000,
    discountAmount: 40000,
    paymentMethod: 'momo',
    status: 'delivered',
    paymentStatus: 'completed',
    transactionId: 'TXN007',
    paidAt: new Date(),
    shippingAddressId: null // Will be set to eleventh address ID
  },
  {
    totalPrice: 195000,
    originalAmount: 200000,
    discountAmount: 5000,
    paymentMethod: 'zalopay',
    status: 'cancelled',
    paymentStatus: 'refunded',
    shippingAddressId: null // Will be set to twelfth address ID
  }
]

const sampleVouchers = [
  {
    code: "WELCOME10",
    name: "Giảm 10% cho khách hàng mới",
    description: "Giảm 10% cho khách hàng mới",
    type: "percentage",
    value: 10,
    minOrderAmount: 100000,
    maxDiscountAmount: 50000,
    usageLimit: 100,
    usedCount: 0,
    validFrom: new Date("2025-01-01"),
    validTo: new Date("2025-12-31"),
    isActive: true
  },
  {
    code: "FREESHIP50",
    name: "Giảm 50.000 cho đơn từ 200.000",
    description: "Giảm 50.000 cho đơn từ 200.000",
    type: "fixed_amount",
    value: 50000,
    minOrderAmount: 200000,
    maxDiscountAmount: 50000,
    usageLimit: 300,
    usedCount: 0,
    validFrom: new Date("2025-02-01"),
    validTo: new Date("2025-12-31"),
    isActive: true
  },
  {
    code: "READMORE20",
    name: "Ưu đãi 20% cho sách kỹ năng sống",
    description: "Ưu đãi 20% cho sách kỹ năng sống",
    type: "percentage",
    value: 20,
    minOrderAmount: 150000,
    maxDiscountAmount: 80000,
    usageLimit: 200,
    usedCount: 0,
    validFrom: new Date("2025-03-01"),
    validTo: new Date("2025-09-01"),
    isActive: true
  },
  {
    code: "TECH30K",
    name: "Giảm 30.000 cho sách công nghệ",
    description: "Giảm 30.000 cho sách công nghệ",
    type: "fixed_amount",
    value: 30000,
    minOrderAmount: 120000,
    maxDiscountAmount: 30000,
    usageLimit: 150,
    usedCount: 0,
    validFrom: new Date("2025-04-01"),
    validTo: new Date("2025-10-01"),
    isActive: true
  },
  {
    code: "SUMMER15",
    name: "Giảm 15% cho đơn mùa hè",
    description: "Giảm 15% cho đơn mùa hè",
    type: "percentage",
    value: 15,
    minOrderAmount: 100000,
    maxDiscountAmount: 70000,
    usageLimit: 500,
    usedCount: 0,
    validFrom: new Date("2025-06-01"),
    validTo: new Date("2025-08-31"),
    isActive: true
  },
]

// Cart seed data
const sampleCarts = [
  {
    items: [
      {
        quantity: 2,
        addedAt: new Date()
      },
      {
        quantity: 1,
        addedAt: new Date()
      }
    ]
  },
  {
    items: [
      {
        quantity: 3,
        addedAt: new Date()
      }
    ]
  }
]

// UserBook seed data for digital books
const sampleUserBooks = [
  {
    bookType: 'ebook',
    filePath: '/storage/books/ebooks/book-1.pdf',
    fileSize: 2500000,
    mimeType: 'application/pdf',
    downloadCount: 1,
    lastDownloadAt: new Date(),
    isActive: true
  },
  {
    bookType: 'audiobook',
    filePath: '/storage/books/audiobooks/book-2.mp3',
    fileSize: 15000000,
    mimeType: 'audio/mpeg',
    downloadCount: 2,
    lastDownloadAt: new Date(),
    isActive: true
  }
]

// Shipping Provider seed data
const sampleShippingProviders = [
  {
    name: 'Giao Hàng Nhanh',
    code: 'GHN',
    baseFee: 25000,
    estimatedTime: '2-3 ngày',
    description: 'Dịch vụ giao hàng nhanh chóng và tin cậy',
    contactInfo: {
      phone: '1900 1234',
      email: 'support@ghn.vn',
      website: 'https://ghn.vn'
    },
    active: true
  },
  {
    name: 'Giao Hàng Tiết Kiệm',
    code: 'GHTK',
    baseFee: 20000,
    estimatedTime: '3-5 ngày',
    description: 'Dịch vụ giao hàng tiết kiệm chi phí',
    contactInfo: {
      phone: '1900 5678',
      email: 'support@ghtk.vn',
      website: 'https://ghtk.vn'
    },
    active: true
  },
  {
    name: 'Vietnam Post',
    code: 'VNPOST',
    baseFee: 15000,
    estimatedTime: '5-7 ngày',
    description: 'Dịch vụ bưu điện quốc gia',
    contactInfo: {
      phone: '1900 9012',
      email: 'support@vnpost.vn',
      website: 'https://vnpost.vn'
    },
    active: true
  },
  {
    name: 'J&T Express',
    code: 'JNT',
    baseFee: 22000,
    estimatedTime: '2-4 ngày',
    description: 'Dịch vụ giao hàng express',
    contactInfo: {
      phone: '1900 3456',
      email: 'support@jtexpress.vn',
      website: 'https://jtexpress.vn'
    },
    active: true
  },
  {
    name: 'Ninja Van',
    code: 'NINJA',
    baseFee: 30000,
    estimatedTime: '1-2 ngày',
    description: 'Dịch vụ giao hàng siêu tốc',
    contactInfo: {
      phone: '1900 7890',
      email: 'support@ninjavan.vn',
      website: 'https://ninjavan.vn'
    },
    active: false
  }
]

// Sample payments data - chỉ 1 payment mẫu
const samplePayments = [
  {
    amount: 250000,
    method: 'vnpay',
    status: 'completed',
    transactionId: 'TXN001',
    description: 'Thanh toán VNPay cho đơn hàng #ORD001',
    paymentUrl: 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html',
    customerInfo: {
      ipAddress: '192.168.1.100',
      userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
    },
    gatewayResponse: {
      responseCode: '00',
      message: 'Giao dịch thành công',
      transactionNo: 'TXN001'
    }
  }
]

// Sample messages for chat system
const sampleMessages = [
  {
    conversationId: 'conv_001',
    content: 'Xin chào! Tôi cần hỗ trợ về đơn hàng của mình.',
    messageType: 'text',
    isRead: false
  },
  {
    conversationId: 'conv_001',
    content: 'Chào bạn! Tôi có thể giúp gì cho bạn? Vui lòng cho tôi biết mã đơn hàng.',
    messageType: 'text',
    isRead: true
  },
  {
    conversationId: 'conv_001',
    content: 'Đơn hàng của tôi có mã là #ORD001. Tôi muốn hủy đơn hàng này.',
    messageType: 'text',
    isRead: false
  },
  {
    conversationId: 'conv_001',
    content: 'Tôi đã kiểm tra đơn hàng #ORD001 của bạn. Đơn hàng đang trong quá trình xử lý. Bạn có thể hủy đơn hàng trong vòng 24h kể từ khi đặt.',
    messageType: 'text',
    isRead: true
  },
  {
    conversationId: 'conv_001',
    content: 'Cảm ơn bạn! Tôi muốn hủy đơn hàng này.',
    messageType: 'text',
    isRead: false
  },
  {
    conversationId: 'conv_001',
    content: 'Tôi đã hủy đơn hàng #ORD001 cho bạn. Tiền sẽ được hoàn lại trong vòng 3-5 ngày làm việc.',
    messageType: 'text',
    isRead: true
  },
  {
    conversationId: 'conv_002',
    content: 'Xin chào admin! Tôi có câu hỏi về sản phẩm.',
    messageType: 'text',
    isRead: false
  },
  {
    conversationId: 'conv_002',
    content: 'Chào bạn! Tôi sẵn sàng hỗ trợ bạn. Bạn muốn hỏi về sản phẩm nào?',
    messageType: 'text',
    isRead: true
  },
  {
    conversationId: 'conv_002',
    content: 'Tôi muốn hỏi về cuốn sách "JavaScript: The Good Parts". Còn hàng không?',
    messageType: 'text',
    isRead: false
  },
  {
    conversationId: 'conv_002',
    content: 'Cuốn "JavaScript: The Good Parts" hiện tại còn hàng. Giá là 150,000 VND. Bạn có muốn đặt hàng không?',
    messageType: 'text',
    isRead: true
  }
]




// Seed function
const seedDatabase = async () => {
  try {
    console.log('🌱 Starting database seeding...')

    // Clear existing data
    await Role.deleteMany({})
    await User.deleteMany({})
    await Category.deleteMany({})
    await Book.deleteMany({})
    await Order.deleteMany({})
    await OrderItem.deleteMany({})
    await Favorite.deleteMany({})
    await Voucher.deleteMany({})
    await VoucherUsage.deleteMany({})
    await Message.deleteMany({})
    await Address.deleteMany({})
    await Cart.deleteMany({})
    await UserBook.deleteMany({})
    await EmailVerification.deleteMany({})
    await PasswordReset.deleteMany({})
    await ShippingProvider.deleteMany({})
    await Payment.deleteMany({})
    console.log('🧹 Cleared existing data')
    
    // Wait a bit to ensure deletion is complete
    await new Promise(resolve => setTimeout(resolve, 1000))

    // Ensure basic roles exist
    await roleService.ensureBasicRoles()
    
    // Get role references for user creation
    const adminRole = await Role.findOne({ name: 'admin' })
    const userRole = await Role.findOne({ name: 'user' })
    const staffRole = await Role.findOne({ name: 'staff' })
    
    console.log('👥 Roles available:', adminRole.name, userRole.name, staffRole.name)

    // Create users
    const users = []
    for (const userData of sampleUsers) {
      let roleId = userRole._id // Default role
      
      if (userData.email === 'admin@bookstore.com') {
        roleId = adminRole._id
      } else if (userData.email === 'staff@bookstore.com') {
        roleId = staffRole._id
      }
      
      const user = new User({
        ...userData,
        roleId: roleId
      })
      await user.save()
      users.push(user)
    }
    console.log('👤 Created users:', users.length)

    // Create addresses for users
    const addresses = []
    for (let i = 0; i < sampleAddresses.length; i++) {
      const address = await Address.create({
        ...sampleAddresses[i],
        userId: users[i % users.length]._id
      })
      addresses.push(address)
    }
    console.log('🏠 Created addresses:', addresses.length)

    // Create categories
    const categories = await Category.insertMany(sampleCategories)
    console.log('📚 Created categories:', categories.length)

    // Create shipping providers
    const shippingProviders = await ShippingProvider.insertMany(sampleShippingProviders)
    console.log('🚚 Created shipping providers:', shippingProviders.length)

    // Create books with categories (8 books per category)
    const books = []
    for (let i = 0; i < sampleBooks.length; i++) {
      const categoryIndex = Math.floor(i / 8) // 8 books per category
      const book = await Book.create({
        ...sampleBooks[i],
        categoryId: categories[categoryIndex]._id
      })
      books.push(book)
    }
    console.log('📖 Created books:', books.length)

    // Create orders
    const orders = []
    const defaultProvider = shippingProviders.find(p => p.active) // Lấy provider đầu tiên đang active
    for (let i = 0; i < sampleOrders.length; i++) {
      const order = await Order.create({
        ...sampleOrders[i],
        userId: users[1]._id, // Regular user
        shippingAddressId: addresses[i % addresses.length]._id, // Assign address ID
        shippingProvider: defaultProvider ? defaultProvider._id : null,
        shippingFee: defaultProvider ? defaultProvider.baseFee : 0,
        totalPrice: sampleOrders[i].totalPrice + (defaultProvider ? defaultProvider.baseFee : 0) // Cộng phí ship vào tổng tiền
      })
      orders.push(order)
      console.log(`🛒 Created order ${i + 1}: ${order.orderCode} - ${order.status} - ${order.totalPrice.toLocaleString('vi-VN')} ₫`)
    }
    console.log('🛒 Total orders created:', orders.length)

    // Create order items
    const orderItems = []
    for (let i = 0; i < orders.length; i++) {
      const book = books[i % books.length]
      const quantity = Math.floor(Math.random() * 3) + 1
      const orderItem = await OrderItem.create({
        orderId: orders[i]._id,
        bookId: book._id,
        quantity: quantity,
        priceAtPurchase: book.price
      })
      orderItems.push(orderItem)
    }
    console.log('📦 Created order items:', orderItems.length)

    // Create payments
    const payments = []
    for (let i = 0; i < samplePayments.length; i++) {
      const payment = await Payment.create({
        ...samplePayments[i],
        orderId: orders[i % orders.length]._id // Link to existing orders
      })
      payments.push(payment)
      console.log(`💳 Created payment ${i + 1}: ${payment.transactionCode} - ${payment.method} - ${payment.status} - ${payment.amount.toLocaleString('vi-VN')} ₫`)
    }
    console.log('💳 Total payments created:', payments.length)

    // Create favorites
    const favorites = []
    for (let i = 0; i < Math.min(2, books.length); i++) {
      const favorite = await Favorite.create({
        userId: users[1]._id, // Regular user
        bookId: books[i]._id,
        isFavourite: true
      })
      favorites.push(favorite)
    }
    console.log('❤️ Created favorites:', favorites.length)

    // Create vouchers
    const vouchers = []
    for (let i = 0; i < sampleVouchers.length; i++) {
      const voucher = await Voucher.create({
        ...sampleVouchers[i],
        createdBy: users[0]._id // Admin user
      })
      vouchers.push(voucher)
    }
    console.log('🎫 Created vouchers:', vouchers.length)

    // Create voucher usages
    const voucherUsages = []
    for (let i = 0; i < 2; i++) {
      const voucherUsage = await VoucherUsage.create({
        voucherId: vouchers[i]._id,
        userId: users[1]._id, // Regular user
        orderId: orders[i]._id,
        voucherCode: vouchers[i].code,
        discountAmount: vouchers[i].value,
        orderAmount: orders[i].totalPrice
      })
      voucherUsages.push(voucherUsage)
    }
    console.log('🎫 Created voucher usages:', voucherUsages.length)

    // Create carts
    const carts = []
    for (let i = 0; i < sampleCarts.length; i++) {
      const cart = await Cart.create({
        ...sampleCarts[i],
        userId: users[i % users.length]._id,
        items: sampleCarts[i].items.map((item, itemIndex) => ({
          ...item,
          bookId: books[itemIndex % books.length]._id
        }))
      })
      carts.push(cart)
    }
    console.log('🛒 Created carts:', carts.length)

    // Create user books (digital book ownership)
    const userBooks = []
    for (let i = 0; i < sampleUserBooks.length; i++) {
      const userBook = await UserBook.create({
        ...sampleUserBooks[i],
        userId: users[1]._id, // Regular user
        bookId: books[i % books.length]._id,
        orderId: orders[i % orders.length]._id
      })
      userBooks.push(userBook)
    }
    console.log('📚 Created user books:', userBooks.length)

    // Create messages
    const messages = []
    for (let i = 0; i < sampleMessages.length; i++) {
      const message = await Message.create({
        ...sampleMessages[i],
        fromId: i % 2 === 0 ? users[1]._id : users[0]._id, // Alternate between regular user and admin
        toId: i % 2 === 0 ? users[0]._id : users[1]._id
      })
      messages.push(message)
    }
    console.log('💬 Created messages:', messages.length)

    // Create email verifications
    const emailVerifications = []
    for (let i = 0; i < 3; i++) {
      const emailVerification = await EmailVerification.create({
        email: users[i].email,
        code: Math.floor(100000 + Math.random() * 900000).toString(),
        expiresAt: new Date(Date.now() + 5 * 60 * 1000), // 5 minutes from now
        attempts: 0,
        isUsed: i === 0 // First one is used
      })
      emailVerifications.push(emailVerification)
    }
    console.log('📧 Created email verifications:', emailVerifications.length)

    // Create password resets
    const passwordResets = []
    for (let i = 0; i < 2; i++) {
      const passwordReset = await PasswordReset.create({
        email: users[i].email,
        token: require('crypto').randomBytes(32).toString('hex'),
        expiresAt: new Date(Date.now() + 15 * 60 * 1000), // 15 minutes from now
        attempts: 0,
        isUsed: false
      })
      passwordResets.push(passwordReset)
    }
    console.log('🔐 Created password resets:', passwordResets.length)





    console.log('✅ Database seeding completed successfully!')
    console.log('\n📋 Summary:')
    console.log(`👑 Admin user: admin@bookstore.com / admin123`)
    console.log(`👤 Regular user: user@bookstore.com / user123`)
    console.log(`👤 Test user: test@bookstore.com / test123`)
    console.log(`📚 Categories: ${categories.length}`)
    console.log(`📖 Books: ${books.length}`)
    console.log(`🏠 Addresses: ${addresses.length}`)
    console.log(`🛒 Orders: ${orders.length}`)
    console.log(`📦 Order items: ${orderItems.length}`)
    console.log(`💳 Payments: ${payments.length}`)
    console.log(`❤️ Favorites: ${favorites.length}`)
    console.log(`🎫 Vouchers: ${vouchers.length}`)
    console.log(`🎫 Voucher usages: ${voucherUsages.length}`)
    console.log(`🛒 Carts: ${carts.length}`)
    console.log(`📚 User books: ${userBooks.length}`)
    console.log(`💬 Messages: ${messages.length}`)
    console.log(`📧 Email verifications: ${emailVerifications.length}`)
    console.log(`🔐 Password resets: ${passwordResets.length}`)
    console.log(`🚚 Shipping providers: ${shippingProviders.length}`)
    
    console.log('\n🛒 Order Details:')
    orders.forEach((order, index) => {
      console.log(`${index + 1}. ${order.orderCode} - ${order.status} - ${order.totalPrice.toLocaleString('vi-VN')} ₫`)
    })

    console.log('\n💳 Payment Details:')
    payments.forEach((payment, index) => {
      console.log(`${index + 1}. ${payment.transactionCode} - ${payment.method} - ${payment.status} - ${payment.amount.toLocaleString('vi-VN')} ₫`)
    })

  } catch (error) {
    console.error('❌ Seeding error:', error.message)
    console.error(error)
  } finally {
    await mongoose.connection.close()
    console.log('🔌 Database connection closed')
    process.exit(0)
  }
}

// Run seeding
const runSeed = async () => {
  await connectDB()
  await seedDatabase()
}

runSeed()