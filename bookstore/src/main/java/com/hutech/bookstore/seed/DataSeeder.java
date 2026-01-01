package com.hutech.bookstore.seed;

import com.hutech.bookstore.model.*;
import com.hutech.bookstore.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Data Seeder - Tạo dữ liệu mẫu cho database
 * Chạy tự động khi ứng dụng khởi động nếu app.seed.enabled=true
 * 
 * Để chạy seed: set app.seed.enabled=true trong application.properties
 * Để tắt seed: set app.seed.enabled=false
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final ShippingProviderRepository shippingProviderRepository;
    private final VoucherRepository voucherRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.clear-existing:false}")
    private boolean clearExisting;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        log.info("🌱 Starting database seeding...");

        try {
            // Clear existing data (optional - chỉ khi cần reset)
            // clearExistingData();

            // Seed roles first
            seedRoles();

            // Seed categories
            seedCategories();

            // Seed shipping providers
            seedShippingProviders();

            // Seed users
            seedUsers();

            // Seed books
            seedBooks();

            // Seed addresses
            seedAddresses();

            // Seed vouchers
            seedVouchers();

            log.info("✅ Database seeding completed successfully!");
            log.info("\n📋 Summary:");
            log.info("👑 Admin user: admin@bookstore.com / admin123");
            log.info("👤 Staff user: staff@bookstore.com / staff123");
            log.info("👤 Regular user: user@bookstore.com / user123");
            log.info("👤 Test user: test@bookstore.com / test123");

        } catch (Exception e) {
            log.error("❌ Seeding error: {}", e.getMessage(), e);
        }
    }

    private void seedRoles() {
        // Trong reset mode (clearExisting=true), luôn tạo lại tất cả roles
        // Trong seed mode (clearExisting=false), chỉ tạo roles chưa tồn tại
        if (clearExisting) {
            // Reset mode: Tạo lại tất cả roles (đã xóa trong clearExistingData)
            List<Role> roles = Arrays.asList(
                new Role(null, "admin", "Administrator role - Full system access", false, null, null),
                new Role(null, "user", "Regular user role - Basic user access", false, null, null),
                new Role(null, "staff", "Staff role - Book management, order management, and customer support", false, null, null)
            );
            roleRepository.saveAll(roles);
            log.info("✅ Created {} roles (reset mode)", roles.size());
        } else {
            // Seed mode: Chỉ tạo role nếu chưa tồn tại
            List<Role> rolesToCreate = new ArrayList<>();
            
            if (roleRepository.findByName("admin").isEmpty()) {
                rolesToCreate.add(new Role(null, "admin", "Administrator role - Full system access", false, null, null));
            }
            if (roleRepository.findByName("user").isEmpty()) {
                rolesToCreate.add(new Role(null, "user", "Regular user role - Basic user access", false, null, null));
            }
            if (roleRepository.findByName("staff").isEmpty()) {
                rolesToCreate.add(new Role(null, "staff", "Staff role - Book management, order management, and customer support", false, null, null));
            }
            
            if (!rolesToCreate.isEmpty()) {
                roleRepository.saveAll(rolesToCreate);
                log.info("✅ Created {} new roles", rolesToCreate.size());
            } else {
                log.info("All roles already exist, skipping...");
            }
        }
    }

    private void seedCategories() {
        // Trong reset mode, luôn tạo lại tất cả (đã xóa trong clearExistingData)
        // Trong seed mode, chỉ tạo những gì chưa tồn tại
        if (clearExisting) {
            // Reset mode: Tạo lại tất cả categories
            List<Category> categories = Arrays.asList(
                new Category(null, "Tiểu thuyết", "Các tác phẩm văn học mang tính hư cấu, cảm xúc và chiều sâu tâm lý.", false, null, null),
                new Category(null, "Lịch sử - Văn hóa", "Sách ghi lại các sự kiện, văn hóa và truyền thống dân tộc.", false, null, null),
                new Category(null, "Khoa học", "Kiến thức về tự nhiên, vật lý, sinh học, vũ trụ và nghiên cứu khoa học.", false, null, null),
                new Category(null, "Công nghệ thông tin", "Sách về lập trình, AI, mạng, và công nghệ số.", false, null, null)
            );
            categoryRepository.saveAll(categories);
            log.info("✅ Created {} categories (reset mode)", categories.size());
        } else {
            // Seed mode: Chỉ tạo category nếu chưa tồn tại
            List<Category> categoriesToCreate = new ArrayList<>();
            
            String[] categoryNames = {"Tiểu thuyết", "Lịch sử - Văn hóa", "Khoa học", "Công nghệ thông tin"};
            String[] categoryDescriptions = {
                "Các tác phẩm văn học mang tính hư cấu, cảm xúc và chiều sâu tâm lý.",
                "Sách ghi lại các sự kiện, văn hóa và truyền thống dân tộc.",
                "Kiến thức về tự nhiên, vật lý, sinh học, vũ trụ và nghiên cứu khoa học.",
                "Sách về lập trình, AI, mạng, và công nghệ số."
            };
            
            for (int i = 0; i < categoryNames.length; i++) {
                if (categoryRepository.findByNameAndIsDeletedFalse(categoryNames[i]).isEmpty()) {
                    categoriesToCreate.add(new Category(null, categoryNames[i], categoryDescriptions[i], false, null, null));
                }
            }
            
            if (!categoriesToCreate.isEmpty()) {
                categoryRepository.saveAll(categoriesToCreate);
                log.info("✅ Created {} new categories", categoriesToCreate.size());
            } else {
                log.info("All categories already exist, skipping...");
            }
        }
    }

    private void seedShippingProviders() {
        // Trong reset mode, luôn tạo lại (đã xóa trong clearExistingData)
        // Trong seed mode, chỉ tạo nếu chưa tồn tại
        if (!clearExisting && shippingProviderRepository.count() > 0) {
            log.info("Shipping providers already exist, skipping...");
            return;
        }

        List<ShippingProvider> providers = new ArrayList<>();
        
        ShippingProvider.ContactInfo ghnContact = new ShippingProvider.ContactInfo(
            "1900 1234", "support@ghn.vn", "https://ghn.vn"
        );
        providers.add(new ShippingProvider(null, "Giao Hàng Nhanh", "GHN", 25000.0, 
            "2-3 ngày", true, "Dịch vụ giao hàng nhanh chóng và tin cậy", ghnContact, false, null, null));

        ShippingProvider.ContactInfo ghtkContact = new ShippingProvider.ContactInfo(
            "1900 5678", "support@ghtk.vn", "https://ghtk.vn"
        );
        providers.add(new ShippingProvider(null, "Giao Hàng Tiết Kiệm", "GHTK", 20000.0, 
            "3-5 ngày", true, "Dịch vụ giao hàng tiết kiệm chi phí", ghtkContact, false, null, null));

        ShippingProvider.ContactInfo vnpostContact = new ShippingProvider.ContactInfo(
            "1900 9012", "support@vnpost.vn", "https://vnpost.vn"
        );
        providers.add(new ShippingProvider(null, "Vietnam Post", "VNPOST", 15000.0, 
            "5-7 ngày", true, "Dịch vụ bưu điện quốc gia", vnpostContact, false, null, null));

        ShippingProvider.ContactInfo jntContact = new ShippingProvider.ContactInfo(
            "1900 3456", "support@jtexpress.vn", "https://jtexpress.vn"
        );
        providers.add(new ShippingProvider(null, "J&T Express", "JNT", 22000.0, 
            "2-4 ngày", true, "Dịch vụ giao hàng express", jntContact, false, null, null));

        shippingProviderRepository.saveAll(providers);
        log.info("✅ Created {} shipping providers", providers.size());
    }

    private void seedUsers() {
        Role adminRole = roleRepository.findByName("admin")
            .orElseThrow(() -> new RuntimeException("Admin role not found"));
        Role userRole = roleRepository.findByName("user")
            .orElseThrow(() -> new RuntimeException("User role not found"));
        Role staffRole = roleRepository.findByName("staff")
            .orElseThrow(() -> new RuntimeException("Staff role not found"));

        // Trong reset mode, luôn tạo lại tất cả users (đã xóa trong clearExistingData)
        // Trong seed mode, chỉ tạo users chưa tồn tại
        if (clearExisting) {
            // Reset mode: Tạo lại tất cả users
            List<User> users = Arrays.asList(
                createUser("Admin User", "Nguyễn Văn Admin", "admin@bookstore.com", 
                    "admin123", "0323456789", "123 Admin Street, Ho Chi Minh City", 
                    adminRole, true, User.UserStatus.ACTIVE),
                createUser("Staff User", "Lê Văn Staff", "staff@bookstore.com", 
                    "staff123", "0123456789", "789 Staff Road, Ho Chi Minh City", 
                    staffRole, true, User.UserStatus.ACTIVE),
                createUser("Regular User", "Trần Thị User", "user@bookstore.com", 
                    "user123", "0987654321", "456 User Avenue, Ho Chi Minh City", 
                    userRole, true, User.UserStatus.ACTIVE),
                createUser("Test User", "Lê Văn Test", "test@bookstore.com", 
                    "test123", "0369852147", "789 Test Road, Ho Chi Minh City", 
                    userRole, false, User.UserStatus.PENDING)
            );
            userRepository.saveAll(users);
            log.info("✅ Created {} users (reset mode)", users.size());
        } else {
            // Seed mode: Chỉ tạo user nếu chưa tồn tại
            List<User> usersToCreate = new ArrayList<>();
            
            if (userRepository.findByEmailAndIsDeletedFalse("admin@bookstore.com").isEmpty()) {
                usersToCreate.add(createUser("Admin User", "Nguyễn Văn Admin", "admin@bookstore.com", 
                    "admin123", "0323456789", "123 Admin Street, Ho Chi Minh City", 
                    adminRole, true, User.UserStatus.ACTIVE));
            }
            if (userRepository.findByEmailAndIsDeletedFalse("staff@bookstore.com").isEmpty()) {
                usersToCreate.add(createUser("Staff User", "Lê Văn Staff", "staff@bookstore.com", 
                    "staff123", "0123456789", "789 Staff Road, Ho Chi Minh City", 
                    staffRole, true, User.UserStatus.ACTIVE));
            }
            if (userRepository.findByEmailAndIsDeletedFalse("user@bookstore.com").isEmpty()) {
                usersToCreate.add(createUser("Regular User", "Trần Thị User", "user@bookstore.com", 
                    "user123", "0987654321", "456 User Avenue, Ho Chi Minh City", 
                    userRole, true, User.UserStatus.ACTIVE));
            }
            if (userRepository.findByEmailAndIsDeletedFalse("test@bookstore.com").isEmpty()) {
                usersToCreate.add(createUser("Test User", "Lê Văn Test", "test@bookstore.com", 
                    "test123", "0369852147", "789 Test Road, Ho Chi Minh City", 
                    userRole, false, User.UserStatus.PENDING));
            }

            if (!usersToCreate.isEmpty()) {
                userRepository.saveAll(usersToCreate);
                log.info("✅ Created {} new users", usersToCreate.size());
            } else {
                log.info("All sample users already exist, skipping...");
            }
        }
    }

    private User createUser(String name, String fullName, String email, String password,
                           String phone, String address, Role role, boolean emailVerified, 
                           User.UserStatus status) {
        User user = new User();
        user.setName(name);
        user.setFullName(fullName);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setAddress(address);
        user.setRole(role);
        user.setIsEmailVerified(emailVerified);
        user.setStatus(status);
        user.setIsActive(status == User.UserStatus.ACTIVE);
        return user;
    }

    private void seedBooks() {
        // Trong reset mode, luôn tạo lại (đã xóa trong clearExistingData)
        // Trong seed mode, chỉ tạo nếu chưa tồn tại
        if (!clearExisting && bookRepository.count() > 0) {
            log.info("Books already exist, skipping...");
            return;
        }

        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            log.warn("No categories found, cannot create books");
            return;
        }

        List<Book> books = new ArrayList<>();
        String[] publishers = {
            "NXB Kim Đồng", "NXB Trẻ", "NXB Văn Học", "NXB Giáo Dục",
            "NXB Tổng Hợp", "NXB Thế Giới", "NXB Hội Nhà Văn", "NXB Đại Học Quốc Gia"
        };

        // Tạo 8 sách cho mỗi category
        for (Category category : categories) {
            for (int j = 1; j <= 8; j++) {
                Book.BookFormat format = (j % 2 == 0) ? Book.BookFormat.PAPERBACK : Book.BookFormat.HARDCOVER;
                
                int year = 2020 + random.nextInt(4);
                int month = 1 + random.nextInt(8);
                int day = 1 + random.nextInt(28);
                LocalDate publicationDate = LocalDate.of(year, month, day);

                Book book = new Book();
                book.setTitle(category.getName() + " Tập " + j);
                book.setAuthor("Tác giả " + category.getName() + " " + j);
                book.setDescription("Cuốn sách " + category.getName().toLowerCase() + 
                    " tập " + j + " mang đến nội dung hấp dẫn, phù hợp với độc giả yêu thích thể loại này.");
                book.setPrice(50000.0 + random.nextDouble() * 150000);
                book.setStock(10 + random.nextInt(90));
                book.setImageUrl("https://placehold.co/400x600?text=" + 
                    category.getName().replace(" ", "+") + "+" + j);
                book.setCategory(category);
                book.setIsbn("978-" + category.getId() + j + String.format("%06d", random.nextInt(1000000)));
                book.setPublisher(publishers[random.nextInt(publishers.length)]);
                book.setPublicationDate(publicationDate);
                book.setPages(150 + random.nextInt(400));
                book.setFormat(format);
                book.setDimensions("20x15x3");
                book.setWeight(300.0 + random.nextDouble() * 500);
                book.setViewCount(random.nextInt(1000));
                book.setIsActive(true);
                book.setStatus(Book.BookStatus.AVAILABLE);
                book.setIsDeleted(false);
                // DigitalFile không cần set vì đây là sách vật lý

                books.add(book);
            }
        }

        bookRepository.saveAll(books);
        log.info("✅ Created {} books", books.size());
    }

    private void seedAddresses() {
        // Trong reset mode, luôn tạo lại (đã xóa trong clearExistingData)
        // Trong seed mode, chỉ tạo nếu chưa tồn tại
        if (!clearExisting && addressRepository.count() > 0) {
            log.info("Addresses already exist, skipping...");
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.warn("No users found, cannot create addresses");
            return;
        }

        List<Address> addresses = new ArrayList<>();
        String[][] addressData = {
            {"Nguyễn Văn Admin", "0323456789", "123 Đường Admin", "TP. Hồ Chí Minh", "Quận 1", "Phường Bến Nghé"},
            {"Trần Thị User", "0987654321", "456 Đường User", "TP. Hồ Chí Minh", "Quận 2", "Phường Thủ Thiêm"},
            {"Lê Văn Test", "0369852147", "789 Đường Test", "TP. Hồ Chí Minh", "Quận 3", "Phường Võ Thị Sáu"},
            {"Phạm Thị D", "0912345678", "321 Đường GHI", "TP. Hồ Chí Minh", "Quận 7", "Phường Tân Phú"},
            {"Hoàng Văn E", "0987654321", "654 Đường JKL", "TP. Hồ Chí Minh", "Quận 10", "Phường 15"}
        };

        for (int i = 0; i < addressData.length && i < users.size(); i++) {
            Address address = new Address();
            address.setUser(users.get(i % users.size()));
            address.setName(addressData[i][0]);
            address.setPhone(addressData[i][1]);
            address.setAddress(addressData[i][2]);
            address.setCity(addressData[i][3]);
            address.setDistrict(addressData[i][4]);
            address.setWard(addressData[i][5]);
            address.setIsDefault(i == 0); // First address is default
            address.setIsDeleted(false);

            addresses.add(address);
        }

        addressRepository.saveAll(addresses);
        log.info("✅ Created {} addresses", addresses.size());
    }

    private void seedVouchers() {
        // Trong reset mode, luôn tạo lại (đã xóa trong clearExistingData)
        // Trong seed mode, chỉ tạo nếu chưa tồn tại
        if (!clearExisting && voucherRepository.count() > 0) {
            log.info("Vouchers already exist, skipping...");
            return;
        }

        List<User> users = userRepository.findAll();
        User adminUser = users.stream()
            .filter(u -> u.getRole().getName().equals("admin"))
            .findFirst()
            .orElse(users.get(0));

        List<Voucher> vouchers = Arrays.asList(
            createVoucher("WELCOME10", "Giảm 10% cho khách hàng mới", 
                "Giảm 10% cho khách hàng mới", Voucher.VoucherType.PERCENTAGE, 10.0, 
                100000.0, 50000.0, 100, 
                LocalDateTime.of(2025, 1, 1, 0, 0), 
                LocalDateTime.of(2025, 12, 31, 23, 59), adminUser),
            createVoucher("FREESHIP50", "Giảm 50.000 cho đơn từ 200.000", 
                "Giảm 50.000 cho đơn từ 200.000", Voucher.VoucherType.FIXED_AMOUNT, 50000.0, 
                200000.0, 50000.0, 300, 
                LocalDateTime.of(2025, 2, 1, 0, 0), 
                LocalDateTime.of(2025, 12, 31, 23, 59), adminUser),
            createVoucher("READMORE20", "Ưu đãi 20% cho sách kỹ năng sống", 
                "Ưu đãi 20% cho sách kỹ năng sống", Voucher.VoucherType.PERCENTAGE, 20.0, 
                150000.0, 80000.0, 200, 
                LocalDateTime.of(2025, 3, 1, 0, 0), 
                LocalDateTime.of(2025, 9, 1, 23, 59), adminUser),
            createVoucher("TECH30K", "Giảm 30.000 cho sách công nghệ", 
                "Giảm 30.000 cho sách công nghệ", Voucher.VoucherType.FIXED_AMOUNT, 30000.0, 
                120000.0, 30000.0, 150, 
                LocalDateTime.of(2025, 4, 1, 0, 0), 
                LocalDateTime.of(2025, 10, 1, 23, 59), adminUser),
            createVoucher("SUMMER15", "Giảm 15% cho đơn mùa hè", 
                "Giảm 15% cho đơn mùa hè", Voucher.VoucherType.PERCENTAGE, 15.0, 
                100000.0, 70000.0, 500, 
                LocalDateTime.of(2025, 6, 1, 0, 0), 
                LocalDateTime.of(2025, 8, 31, 23, 59), adminUser)
        );

        voucherRepository.saveAll(vouchers);
        log.info("✅ Created {} vouchers", vouchers.size());
    }

    private Voucher createVoucher(String code, String name, String description, 
                                  Voucher.VoucherType type, Double value, 
                                  Double minOrderAmount, Double maxDiscountAmount, 
                                  Integer usageLimit, LocalDateTime validFrom, 
                                  LocalDateTime validTo, User createdBy) {
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setName(name);
        voucher.setDescription(description);
        voucher.setType(type);
        voucher.setValue(value);
        voucher.setMinOrderAmount(minOrderAmount);
        voucher.setMaxDiscountAmount(maxDiscountAmount);
        voucher.setUsageLimit(usageLimit);
        voucher.setUsedCount(0);
        voucher.setValidFrom(validFrom);
        voucher.setValidTo(validTo);
        voucher.setIsActive(true);
        voucher.setCreatedBy(createdBy);
        voucher.setIsDeleted(false);
        return voucher;
    }

    /**
     * Xóa toàn bộ dữ liệu trong database (chỉ dữ liệu seed, không xóa schema)
     * CẢNH BÁO: Chỉ sử dụng trong môi trường development!
     * Reset mode sẽ xóa tất cả kể cả roles để đảm bảo data giống hệt seed mode
     */
    private void clearExistingData() {
        log.warn("🧹 Clearing existing data...");
        log.warn("⚠️  WARNING: This will delete all data in the database!");
        
        try {
            // Xóa theo thứ tự để tránh lỗi foreign key constraint
            // Thứ tự: Xóa các bảng phụ thuộc trước, bảng chính sau
            
            addressRepository.deleteAll();
            log.info("   ✓ Cleared addresses");
            
            voucherRepository.deleteAll();
            log.info("   ✓ Cleared vouchers");
            
            bookRepository.deleteAll();
            log.info("   ✓ Cleared books");
            
            categoryRepository.deleteAll();
            log.info("   ✓ Cleared categories");
            
            userRepository.deleteAll();
            log.info("   ✓ Cleared users");
            
            shippingProviderRepository.deleteAll();
            log.info("   ✓ Cleared shipping providers");
            
            // Xóa roles để đảm bảo reset hoàn toàn giống seed
            roleRepository.deleteAll();
            log.info("   ✓ Cleared roles");
            
            log.info("✅ All data cleared successfully!");
        } catch (Exception e) {
            log.error("❌ Error clearing data: {}", e.getMessage(), e);
            throw e;
        }
    }
}

