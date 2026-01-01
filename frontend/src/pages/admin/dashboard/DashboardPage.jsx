import React, { useEffect, useState } from 'react';
import { bookAPI, userAPI, orderAPI } from '../../../services/apiService'; // Dùng các API cơ bản
import { useNavigate } from 'react-router-dom';

const DashboardPage = () => {
  const navigate = useNavigate();
  const [stats, setStats] = useState({
    totalBooks: 0,
    totalUsers: 0,
    totalOrders: 0,
    totalRevenue: 0
  });
  const [recentOrders, setRecentOrders] = useState([]);
  const [topBooks, setTopBooks] = useState([]);
  const [loading, setLoading] = useState(true);

  // Hàm format tiền tệ an toàn
  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount || 0);
  };

  // Hàm lấy tên khách hàng an toàn (Xử lý cho cả Backend cũ và mới)
  const getUserName = (order) => {
    // 1. Backend cũ: user là object
    if (order.user && order.user.name) return order.user.name;
    // 2. Backend mới: userName là string phẳng
    if (order.userName) return order.userName;
    // 3. Lấy từ địa chỉ giao hàng
    if (order.shippingAddress && order.shippingAddress.name) return order.shippingAddress.name;
    // 4. Trường hợp khác
    if (order.shippingName) return order.shippingName;
    return 'Khách vãng lai';
  };

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        
        // Gọi song song 3 API cơ bản để lấy dữ liệu thô
        // Lưu ý: Backend cũ có thể không hỗ trợ phân trang chuẩn hoặc tham số limit khác nhau
        // Ta cố gắng lấy số lượng đủ lớn để tính toán
        const [booksRes, usersRes, ordersRes] = await Promise.all([
            bookAPI.getBooks({ limit: 100 }), 
            userAPI.getUsers({ limit: 100 }),
            orderAPI.getOrders({ limit: 20 }) // Lấy 20 đơn mới nhất
        ]);

        // Xử lý dữ liệu trả về (cần check kỹ cấu trúc response của bạn)
        const books = booksRes?.data?.data?.books || booksRes?.data?.books || [];
        const users = usersRes?.data?.data?.users || usersRes?.data?.users || [];
        // API order cũ có thể trả về mảng trực tiếp hoặc nằm trong data.orders
        const orders = ordersRes?.data?.data?.orders || ordersRes?.data?.orders || [];

        // 1. Tự tính thống kê
        const totalRevenue = orders.reduce((sum, order) => {
            // Chỉ cộng tiền các đơn hàng hợp lệ (không bị hủy)
            if (order.status !== 'CANCELLED') {
                return sum + (order.totalPrice || 0);
            }
            return sum;
        }, 0);

        setStats({
            totalBooks: books.length || 0, // Lưu ý: Đây chỉ là số lượng của trang hiện tại nếu có phân trang
            totalUsers: users.length || 0,
            totalOrders: orders.length || 0,
            totalRevenue: totalRevenue
        });

        // 2. Cập nhật danh sách đơn hàng gần đây (Lấy 5 đơn đầu tiên)
        setRecentOrders(orders.slice(0, 5));

        // 3. Cập nhật danh sách sách (Hiện tại lấy 5 sách đầu tiên làm ví dụ)
        // Nếu backend cũ chưa có API sách bán chạy, ta hiển thị sách mới nhất
        setTopBooks(books.slice(0, 5));

      } catch (error) {
        console.error("Lỗi tải dashboard:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const getStatusColor = (status) => {
    if (!status) return 'bg-gray-100 text-gray-800';
    switch (status.toUpperCase()) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMED': return 'bg-blue-100 text-blue-800';
      case 'SHIPPED': return 'bg-purple-100 text-purple-800';
      case 'DELIVERED': return 'bg-green-100 text-green-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading) return (
      <div className="flex items-center justify-center h-screen">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
  );

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-gray-800">Bảng điều khiển</h1>
        <p className="text-gray-500">Tổng quan về cửa hàng sách của bạn</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard title="Tổng số sách" value={stats.totalBooks} icon="📚" color="bg-blue-500" />
        <StatCard title="Tổng người dùng" value={stats.totalUsers} icon="👥" color="bg-green-500" />
        <StatCard title="Tổng đơn hàng" value={stats.totalOrders} icon="🛍️" color="bg-yellow-500" />
        <StatCard title="Doanh thu tạm tính" value={formatCurrency(stats.totalRevenue)} icon="💰" color="bg-purple-500" />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* --- ĐƠN HÀNG GẦN ĐÂY --- */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex justify-between items-center mb-4">
            <h2 className="font-bold text-lg text-gray-800">Đơn hàng gần đây</h2>
            <button onClick={() => navigate('/admin/orders')} className="text-blue-600 text-sm hover:underline">
                Xem tất cả
            </button>
          </div>
          
          <div className="space-y-4">
            {recentOrders.length > 0 ? (
              recentOrders.map((order) => (
                <div key={order._id || order.id} className="flex items-center justify-between p-4 bg-gray-50 rounded-lg hover:bg-gray-100 transition">
                  <div className="flex items-center space-x-4">
                    <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold">
                       {/* Lấy chữ cái đầu an toàn */}
                       {getUserName(order).charAt(0).toUpperCase()}
                    </div>
                    <div>
                      {/* Hiển thị tên user đã qua xử lý */}
                      <p className="font-medium text-gray-900">{getUserName(order)}</p>
                      <p className="text-xs text-gray-500">{order.orderCode || order._id}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-gray-900">{formatCurrency(order.totalPrice)}</p>
                    <span className={`text-xs px-2 py-1 rounded-full ${getStatusColor(order.status)}`}>
                      {order.status}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-center text-gray-500 py-4">Chưa có đơn hàng nào</p>
            )}
          </div>
        </div>

        {/* --- SÁCH HIỆN CÓ --- */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex justify-between items-center mb-4">
            <h2 className="font-bold text-lg text-gray-800">Sách hiện có</h2>
            <button onClick={() => navigate('/admin/books')} className="text-blue-600 text-sm hover:underline">Xem tất cả</button>
          </div>
          
          <div className="space-y-4">
            {topBooks.length > 0 ? (
              topBooks.map((book, index) => (
                <div key={book._id || book.id} className="flex items-center justify-between p-3 hover:bg-gray-50 rounded-lg transition">
                  <div className="flex items-center space-x-4">
                    <span className="w-6 h-6 flex items-center justify-center rounded-full bg-gray-100 text-xs font-bold text-gray-600">
                      {index + 1}
                    </span>
                    <div className="w-10 h-14 bg-gray-200 rounded overflow-hidden flex-shrink-0">
                        {book.imageUrl ? (
                            <img src={book.imageUrl} alt={book.title} className="w-full h-full object-cover" />
                        ) : (
                            <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">Img</div>
                        )}
                    </div>
                    <div>
                      <p className="font-medium text-gray-900 line-clamp-1" title={book.title}>{book.title}</p>
                      <p className="text-xs text-gray-500">Giá: {formatCurrency(book.price)}</p>
                    </div>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-center text-gray-500 py-4">Chưa có dữ liệu sách</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

// Component thẻ thống kê
const StatCard = ({ title, value, icon, color }) => (
  <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 flex items-center space-x-4">
    <div className={`w-12 h-12 rounded-full flex items-center justify-center text-white text-xl ${color}`}>
      {icon}
    </div>
    <div>
      <p className="text-sm text-gray-500">{title}</p>
      <p className="text-2xl font-bold text-gray-800">{value}</p>
    </div>
  </div>
);

export default DashboardPage;