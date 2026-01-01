import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { orderAPI } from '../../../services/apiService';

const OrdersPage = () => {
  const navigate = useNavigate(); // Thêm hook navigate
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // --- 1. Debounce Search ---
  const [searchTermInput, setSearchTermInput] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  
  const [filterStatus, setFilterStatus] = useState('all');

  // Debounce Effect
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTermInput);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTermInput]);

  // --- 2. Fetch Data ---
  useEffect(() => {
    const fetchOrders = async () => {
      setLoading(true);
      try {
        const response = await orderAPI.getOrders({
          search: debouncedSearchTerm,
          status: filterStatus !== 'all' ? filterStatus : undefined,
          sortBy: 'createdAt',
          sortOrder: 'desc'
        });
        setOrders(response?.data?.data?.orders || response?.data?.orders || []);
      } catch (error) {
        console.error('Error fetching orders:', error);
        setOrders([]); 
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, [debouncedSearchTerm, filterStatus]);

  // Helpers
  const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  
  const getStatusColor = (status) => {
    switch (status) {
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMED': return 'bg-blue-100 text-blue-800';
      case 'SHIPPED': return 'bg-purple-100 text-purple-800';
      case 'DELIVERED': return 'bg-green-100 text-green-800';
      case 'CANCELLED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const handleStatusChange = async (orderId, newStatus) => {
    try {
      await orderAPI.updateOrderStatus(orderId, newStatus);
      // Cập nhật UI ngay lập tức
      setOrders(prev => prev.map(o => o._id === orderId ? {...o, status: newStatus} : o));
    } catch (error) {
      alert('Lỗi cập nhật trạng thái');
    }
  };

  return (
    <div className="space-y-6">
      {/* Filters */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Mã đơn, Tên khách, Email, SĐT..."
              value={searchTermInput}
              onChange={(e) => setSearchTermInput(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">Tất cả</option>
              <option value="pending">Chờ xử lý</option>
              <option value="confirmed">Đã xác nhận</option>
              <option value="shipped">Đã giao</option>
              <option value="delivered">Đã nhận</option>
              <option value="cancelled">Đã hủy</option>
            </select>
          </div>
          <div className="flex items-end">
            <button
              onClick={() => { setSearchTermInput(''); setFilterStatus('all'); }}
              className="w-full px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200"
            >
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        {loading ? <div className="p-8 text-center">Đang tải...</div> : (
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Mã đơn</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Khách hàng</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Tổng tiền</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trạng thái</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ngày đặt</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Thao tác</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {orders.length > 0 ? orders.map((order) => (
              <tr key={order._id}>
                <td className="px-6 py-4 font-medium text-blue-600">
                  {order.orderCode || order._id}
                </td>
                <td className="px-6 py-4">
                  {/* Sử dụng trường mới từ DTO */}
                  <div className="text-sm font-medium text-gray-900">{order.userName || order.shippingName || 'Khách vãng lai'}</div>
                  <div className="text-xs text-gray-500">{order.userEmail}</div>
                  <div className="text-xs text-gray-500">{order.shippingPhone}</div>
                </td>
                <td className="px-6 py-4 text-sm font-bold text-gray-900">
                  {formatCurrency(order.totalPrice)}
                </td>
                <td className="px-6 py-4">
                  <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(order.status)}`}>
                    {order.status}
                  </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">
                  {order.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN') : ''}
                </td>
                <td className="px-6 py-4 flex gap-2">
                  <select
                    value={order.status}
                    onChange={(e) => handleStatusChange(order._id, e.target.value)}
                    className="text-xs border rounded p-1"
                  >
                    <option value="PENDING">Chờ xử lý</option>
                    <option value="CONFIRMED">Đã xác nhận</option>
                    <option value="SHIPPED">Đã giao</option>
                    <option value="DELIVERED">Đã nhận</option>
                    <option value="CANCELLED">Hủy</option>
                  </select>
                  <Link to={`/admin/orders/${order._id}`} className="text-blue-600 hover:text-blue-900 text-sm font-medium">
                    Xem
                  </Link>
                </td>
              </tr>
            )) : (
              <tr><td colSpan="6" className="text-center py-8">Không có đơn hàng nào</td></tr>
            )}
          </tbody>
        </table>
        )}
      </div>
    </div>
  );
};

export default OrdersPage;