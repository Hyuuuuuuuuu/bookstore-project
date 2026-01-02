import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { orderAPI } from '../../../services/apiService';

const OrdersPage = () => {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  // Removed pagination - now getting all orders

  useEffect(() => {
    const fetchOrders = async () => {
      setLoading(true);
      try {
        const response = await orderAPI.getOrders({
          search: searchTerm,
          status: filterStatus !== 'all' ? filterStatus : undefined,
          sortBy: 'createdAt',
          sortOrder: 'desc'
        });
        setOrders(response?.data?.data?.orders || response?.data?.orders || response?.data || []);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching orders:', error);
        setOrders([]); // Set empty array instead of mock data
        setLoading(false);
      }
    };

    fetchOrders();
  }, [searchTerm, filterStatus]);

  // Debug: Log current state
  console.log('📦 OrdersPage - Loading:', loading, 'Orders count:', orders.length);
  console.log('📦 OrdersPage - Orders:', orders);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'pending': return 'bg-yellow-100 text-yellow-800';
      case 'confirmed': return 'bg-blue-100 text-blue-800';
      case 'shipped': return 'bg-purple-100 text-purple-800';
      case 'delivered': return 'bg-green-100 text-green-800';
      case 'cancelled': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'pending': return 'Chờ xử lý';
      case 'confirmed': return 'Đã xác nhận';
      case 'shipped': return 'Đang giao';
      case 'delivered': return 'Đã nhận';
      case 'cancelled': return 'Đã hủy';
      default: return status;
    }
  };

  const getAllowedStatusOptions = (currentStatus) => {
    const STATUS = {
      PENDING: 'pending',
      CONFIRMED: 'confirmed',
      SHIPPED: 'shipped',
      DELIVERED: 'delivered',
      CANCELLED: 'cancelled'
    };

    switch ((currentStatus || '').toLowerCase()) {
      case STATUS.PENDING:
        return [STATUS.PENDING, STATUS.CONFIRMED, STATUS.CANCELLED];
      case STATUS.CONFIRMED:
        return [STATUS.CONFIRMED, STATUS.SHIPPED, STATUS.CANCELLED];
      case STATUS.SHIPPED:
        return [STATUS.SHIPPED, STATUS.DELIVERED];
      case STATUS.DELIVERED:
        return [STATUS.DELIVERED];
      case STATUS.CANCELLED:
        return [STATUS.CANCELLED];
      default:
        return [STATUS.PENDING, STATUS.CONFIRMED, STATUS.SHIPPED, STATUS.DELIVERED, STATUS.CANCELLED];
    }
  };

  const handleStatusChange = async (orderId, newStatus) => {
    try {
      await orderAPI.updateOrderStatus(orderId, newStatus);
      setOrders(prevOrders =>
        prevOrders.map(order =>
          order.id === orderId ? { ...order, status: newStatus } : order
        )
      );
    } catch (error) {
      console.error('Error updating order status:', error);
    }
  };

  // No need for pagination filtering since we get all orders from API
  const filteredOrders = Array.isArray(orders) ? orders.filter(order => {
    const matchesSearch = !searchTerm || 
                          order?.orderCode?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          order?._id?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          order?.userId?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          order?.userId?.email?.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          order?.shippingAddress?.name?.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = filterStatus === 'all' || order?.status === filterStatus;
    return matchesSearch && matchesStatus;
  }) : [];

  // Debug: Log filtered orders
  console.log('📦 OrdersPage - FilteredOrders:', filteredOrders);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="ml-4">Đang tải đơn hàng...</p>
      </div>
    );
  }


  try {
    // Show error message if no orders and not loading
    if (!loading && orders.length === 0) {
    return (
      <div className="space-y-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Không thể tải dữ liệu đơn hàng
              </h3>
              <div className="mt-2 text-sm text-red-700">
                <p>Vui lòng kiểm tra kết nối mạng và thử lại sau.</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Filters */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm kiếm theo mã đơn hàng, tên khách hàng..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
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
              onClick={() => {
                setSearchTerm('');
                setFilterStatus('all');
              }}
              className="w-full px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>

      {/* Orders Table */}
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
                Sản phẩm
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
                Thao tác
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {Array.isArray(filteredOrders) && filteredOrders.length > 0 ? (
              filteredOrders.map((order) => (
                <tr key={order?._id || order?.id || Math.random()} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                    {order?.orderCode || order?._id || order?.id || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div>
                      <div className="text-sm font-medium text-gray-900">{order?.userName || order?.shippingAddress?.name || 'N/A'}</div>
                      <div className="text-sm text-gray-500">{order?.userEmail || 'N/A'}</div>
                      <div className="text-sm text-gray-500">{order?.userPhone || order?.shippingAddress?.phone || 'N/A'}</div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="text-sm text-gray-900">
                      <Link 
                        to={`/admin/orders/${order?._id || order?.id}`}
                        className="text-blue-600 hover:text-blue-900 font-medium"
                      >
                        Xem chi tiết
                      </Link>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {formatCurrency(order?.totalPrice || 0)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(order?.status)}`}>
                      {getStatusText(order?.status)}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {order?.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex space-x-2">
                      <select
                        value={order?.status || 'pending'}
                        onChange={(e) => handleStatusChange(order?._id || order?.id, e.target.value)}
                        className="border border-gray-300 rounded-md text-sm"
                      >
                        {['pending', 'confirmed', 'shipped', 'delivered', 'cancelled'].map((s) => {
                          const allowed = getAllowedStatusOptions(order?.status);
                          const isAllowed = allowed.includes(s);
                          return (
                            <option
                              key={s}
                              value={s}
                              disabled={!isAllowed}
                              title={isAllowed ? getStatusText(s) : 'Không thể chuyển sang trạng thái này từ trạng thái hiện tại'}
                            >
                              {getStatusText(s)}
                            </option>
                          );
                        })}
                      </select>
                    </div>
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="7" className="px-6 py-8 text-center text-gray-500">
                  <div className="flex flex-col items-center">
                    <svg className="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                    </svg>
                    <p className="text-lg font-medium text-gray-900 mb-2">Không có đơn hàng nào</p>
                    <p className="text-sm text-gray-500">Chưa có đơn hàng nào trong hệ thống</p>
                  </div>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Orders Summary */}
      <div className="flex justify-between items-center mt-4">
        <div>
          <p className="text-sm text-gray-700">
            Hiển thị <span className="font-medium">{Array.isArray(filteredOrders) ? filteredOrders.length : 0}</span> đơn hàng
          </p>
        </div>
      </div>
    </div>
    );
  } catch (error) {
    console.error('❌ OrdersPage render error:', error);
    return (
      <div className="space-y-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Lỗi hiển thị trang đơn hàng
              </h3>
              <div className="mt-2 text-sm text-red-700">
                <p>Đã xảy ra lỗi khi hiển thị trang đơn hàng. Vui lòng thử lại.</p>
                <p className="mt-1 text-xs">Error: {error.message}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
};

export default OrdersPage;