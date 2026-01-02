import React, { useState, useEffect } from 'react';
import { paymentAPI } from '../../../services/apiService'; // Đảm bảo đã có paymentAPI

const PaymentsPage = () => {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // --- 1. Debounce Search ---
  const [searchTermInput, setSearchTermInput] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  
  const [filterStatus, setFilterStatus] = useState('all');
  const [pagination, setPagination] = useState({});

  // Debounce Effect: Đợi 0.5s sau khi ngừng gõ mới set giá trị search thật
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTermInput);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTermInput]);

  // --- 2. Fetch Data ---
  useEffect(() => {
    const fetchPayments = async () => {
      setLoading(true);
      try {
        // Gọi API với tham số search và status
        const response = await paymentAPI.getPayments({
          search: debouncedSearchTerm,
          status: filterStatus !== 'all' ? filterStatus : undefined,
          page: 1, 
          limit: 10
        });

        const data = response?.data?.data || response?.data || {};
        setPayments(data.payments || []);
        setPagination(data.pagination || {});
        
      } catch (error) {
        console.error('Error fetching payments:', error);
        setPayments([]);
      } finally {
        setLoading(false);
      }
    };

    fetchPayments();
  }, [debouncedSearchTerm, filterStatus]); // Chạy lại khi search hoặc filter thay đổi

  // Helpers
  const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  const formatDate = (dateString) => new Date(dateString).toLocaleDateString('vi-VN');
  
  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      case 'REFUNDED': return 'bg-gray-100 text-gray-800';
      default: return 'bg-blue-100 text-blue-800';
    }
  };

  return (
    <div className="space-y-6">
      {/* Filters */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          
          {/* Ô Tìm kiếm */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Mã giao dịch, tên khách hàng..."
              value={searchTermInput} // Bind vào state input
              onChange={(e) => setSearchTermInput(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* Lọc Trạng thái */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">Tất cả trạng thái</option>
              <option value="PENDING">Chờ xử lý (PENDING)</option>
              <option value="COMPLETED">Thành công (COMPLETED)</option>
              <option value="FAILED">Thất bại (FAILED)</option>
              <option value="REFUNDED">Hoàn tiền (REFUNDED)</option>
            </select>
          </div>

          {/* Nút Xóa bộ lọc */}
          <div className="flex items-end">
            <button
              onClick={() => {
                setSearchTermInput('');
                setFilterStatus('all');
              }}
              className="w-full px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors"
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
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Mã giao dịch</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Đơn hàng</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Số tiền</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Phương thức</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Trạng thái</th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Ngày</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {payments.length > 0 ? payments.map((payment) => (
              <tr key={payment.id}>
                <td className="px-6 py-4">
                    <div className="text-sm font-medium text-gray-900">{payment.transactionId || 'N/A'}</div>
                    <div className="text-xs text-gray-500">
                        {/* Hiển thị tên khách hàng nếu có trong DTO, hoặc lấy từ Payment description */}
                        {payment.description} 
                    </div>
                </td>
                <td className="px-6 py-4 text-blue-600 font-medium">
                    {payment.orderCode || payment.orderId}
                </td>
                <td className="px-6 py-4 text-sm font-bold text-gray-900">
                    {formatCurrency(payment.amount)}
                </td>
                <td className="px-6 py-4 text-sm text-gray-700">
                    {payment.method}
                </td>
                <td className="px-6 py-4">
                    <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(payment.status)}`}>
                        {payment.status}
                    </span>
                </td>
                <td className="px-6 py-4 text-sm text-gray-500">
                    {payment.createdAt ? formatDate(payment.createdAt) : ''}
                </td>
              </tr>
            )) : (
              <tr><td colSpan="6" className="text-center py-8 text-gray-500">Không tìm thấy giao dịch nào</td></tr>
            )}
          </tbody>
        </table>
        )}
      </div>
    </div>
  );
};

export default PaymentsPage;