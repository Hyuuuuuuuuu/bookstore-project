import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { orderAPI } from '../../../services/apiService';

const OrdersPage = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  
  // Loading state
  const [loading, setLoading] = useState(false);
  
  // Search & Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState(''); // '' = All
  
  // Pagination (nếu backend hỗ trợ)
  const [currentPage, setCurrentPage] = useState(1);
  // const [totalPages, setTotalPages] = useState(1); // Tạm thời chưa dùng nếu API trả về all

  // 1. Debounce Search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // 2. Fetch Orders
  useEffect(() => {
    const fetchOrders = async () => {
      setLoading(true);
      try {
        // Chuẩn bị params gửi lên server
        const params = {
          search: debouncedSearchTerm,
          status: filterStatus || undefined,
          sortBy: 'createdAt',
          sortOrder: 'desc'
          // page: currentPage, // Uncomment nếu backend hỗ trợ phân trang
          // limit: 10
        };

        const response = await orderAPI.getOrders(params);
        
        // Xử lý dữ liệu trả về từ nhiều format khác nhau của backend
        const ordersData = response?.data?.data?.orders || response?.data?.orders || response?.data || [];
        
        setOrders(ordersData);
      } catch (error) {
        console.error('Error fetching orders:', error);
        setOrders([]); 
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, [debouncedSearchTerm, filterStatus]); // Gọi lại khi search hoặc filter thay đổi

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('vi-VN');
  };

  const getStatusColor = (status) => {
    switch ((status || '').toLowerCase()) {
      case 'pending': return 'bg-yellow-100 text-yellow-800';
      case 'confirmed': return 'bg-blue-100 text-blue-800';
      case 'shipped': return 'bg-purple-100 text-purple-800';
      case 'delivered': return 'bg-green-100 text-green-800';
      case 'cancelled': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status) => {
    switch ((status || '').toLowerCase()) {
      case 'pending': return 'Chờ xử lý';
      case 'confirmed': return 'Đã xác nhận';
      case 'shipped': return 'Đang giao';
      case 'delivered': return 'Đã nhận';
      case 'cancelled': return 'Đã hủy';
      default: return status;
    }
  };

  // Logic chuyển đổi trạng thái (chỉ cho phép các trạng thái hợp lệ tiếp theo)
  const getAllowedStatusOptions = (currentStatus) => {
    const STATUS = {
      PENDING: 'pending',
      CONFIRMED: 'confirmed',
      SHIPPED: 'shipped',
      DELIVERED: 'delivered',
      CANCELLED: 'cancelled'
    };

    const s = (currentStatus || '').toLowerCase();

    // Logic nghiệp vụ: Cho phép chuyển đi đâu từ trạng thái hiện tại?
    // Để đơn giản và linh hoạt cho Admin, tạm thời cho phép chuyển hầu hết các trạng thái
    // Trừ khi đã Cancelled hoặc Delivered thì hạn chế hơn.
    switch (s) {
      case STATUS.CANCELLED:
        return [STATUS.CANCELLED]; // Đã hủy thì thôi
      case STATUS.DELIVERED:
        return [STATUS.DELIVERED]; // Đã giao thành công thì thôi
      default:
        return [STATUS.PENDING, STATUS.CONFIRMED, STATUS.SHIPPED, STATUS.DELIVERED, STATUS.CANCELLED];
    }
  };

  const handleStatusChange = async (orderId, newStatus) => {
    if (!window.confirm(`Bạn có chắc muốn chuyển trạng thái đơn hàng sang "${getStatusText(newStatus)}"?`)) return;

    try {
      await orderAPI.updateOrderStatus(orderId, newStatus);
      
      // Cập nhật UI ngay lập tức (Optimistic update)
      setOrders(prevOrders =>
        prevOrders.map(order =>
          (order._id === orderId || order.id === orderId) ? { ...order, status: newStatus } : order
        )
      );
      alert('Cập nhật trạng thái thành công!');
    } catch (error) {
      console.error('Error updating order status:', error);
      alert('Lỗi khi cập nhật trạng thái. Vui lòng thử lại.');
    }
  };

  const clearFilters = () => {
    setSearchTerm('');
    setFilterStatus('');
  };

  return (
    <div className="space-y-6">
      {/* Filters Section - KHÔNG được bao bọc bởi loading để tránh mất focus */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Search Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm kiếm theo mã đơn hàng, tên khách hàng..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* Status Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Tất cả</option>
              <option value="pending">Chờ xử lý</option>
              <option value="confirmed">Đã xác nhận</option>
              <option value="shipped">Đang giao</option>
              <option value="delivered">Đã nhận</option>
              <option value="cancelled">Đã hủy</option>
            </select>
          </div>

          {/* Action Buttons */}
          <div className="flex items-end">
            <button
              onClick={clearFilters}
              className="w-full px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>

      {/* Orders Table Section */}
      <div className="relative min-h-[400px]">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : null}

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Mã đơn hàng
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Khách hàng
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Chi tiết
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Tổng tiền
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Trạng thái
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Ngày đặt
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Cập nhật trạng thái
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {orders.length > 0 ? (
                orders.map((order) => (
                  <tr key={order._id || order.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      {order.orderCode || order._id || order.id}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {order.userName || order.user?.name || order.userId?.name || 'Khách lẻ'}
                        </div>
                        <div className="text-sm text-gray-500">
                          {order.userEmail || order.user?.email || order.userId?.email || ''}
                        </div>
                        <div className="text-sm text-gray-500">
                          {order.userPhone || order.shippingAddress?.phone || ''}
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <Link 
                        to={`/admin/orders/${order._id || order.id}`}
                        className="text-blue-600 hover:text-blue-900 text-sm font-medium"
                      >
                        Xem chi tiết
                      </Link>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-semibold">
                      {formatCurrency(order.totalPrice || 0)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(order.status)}`}>
                        {getStatusText(order.status)}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(order.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <select
                        value={(order.status || '').toLowerCase()}
                        onChange={(e) => handleStatusChange(order._id || order.id, e.target.value)}
                        className="border border-gray-300 rounded-md text-sm py-1 px-2 focus:ring-blue-500 focus:border-blue-500"
                        disabled={order.status === 'CANCELLED' || order.status === 'DELIVERED'} // Disable nếu đã xong
                      >
                        {getAllowedStatusOptions(order.status).map((s) => (
                          <option key={s} value={s}>
                            {getStatusText(s)}
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                    <div className="flex flex-col items-center">
                      <svg className="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                      <p className="text-lg font-medium text-gray-900 mb-1">Không tìm thấy đơn hàng nào</p>
                      <p className="text-sm text-gray-500">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default OrdersPage;