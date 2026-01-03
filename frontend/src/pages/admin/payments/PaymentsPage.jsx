import React, { useState, useEffect } from 'react';
import { paymentAPI } from '../../../services/apiService';

const PaymentsPage = () => {
  const [payments, setPayments] = useState([]);
  
  // Loading state
  const [loading, setLoading] = useState(false);
  
  // Search & Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [paymentsPerPage] = useState(10);

  // 1. Debounce Search Term
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // 2. Fetch Payments
  useEffect(() => {
    const fetchPayments = async () => {
      setLoading(true);
      try {
        const response = await paymentAPI.getPayments({
          search: debouncedSearchTerm,
          status: filterStatus !== 'all' ? filterStatus : undefined
        });

        const paymentsData = response?.data?.data?.payments || response?.data?.payments || response?.data || [];
        setPayments(paymentsData);
      } catch (error) {
        console.error('❌ Error fetching payments:', error);
        setPayments([]);
      } finally {
        setLoading(false);
      }
    };

    fetchPayments();
  }, [debouncedSearchTerm, filterStatus]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const getStatusColor = (status) => {
    const s = (status || '').toLowerCase();
    switch (s) {
      case 'completed': return 'bg-green-100 text-green-800';
      case 'pending': return 'bg-yellow-100 text-yellow-800';
      case 'failed': return 'bg-red-100 text-red-800';
      case 'refunded': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status) => {
    const s = (status || '').toLowerCase();
    switch (s) {
      case 'completed': return 'Hoàn thành';
      case 'pending': return 'Chờ xử lý';
      case 'failed': return 'Thất bại';
      case 'refunded': return 'Đã hoàn tiền';
      default: return status || '';
    }
  };

  // Client-side pagination logic (nếu API trả về all)
  const indexOfLastPayment = currentPage * paymentsPerPage;
  const indexOfFirstPayment = indexOfLastPayment - paymentsPerPage;
  const currentPayments = Array.isArray(payments) ? payments.slice(indexOfFirstPayment, indexOfLastPayment) : [];
  const totalPages = Math.ceil((Array.isArray(payments) ? payments.length : 0) / paymentsPerPage);

  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  const clearFilters = () => {
    setSearchTerm('');
    setFilterStatus('all');
    setCurrentPage(1);
  };

  return (
    <div className="space-y-6">
      {/* Search & Filter Section - KHÔNG được bọc bởi loading */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {/* Search Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm theo mã giao dịch, mã đơn hàng..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Status Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái</label>
            <select
              value={filterStatus}
              onChange={(e) => {
                setFilterStatus(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">Tất cả trạng thái</option>
              <option value="completed">Hoàn thành</option>
              <option value="pending">Chờ xử lý</option>
              <option value="failed">Thất bại</option>
              <option value="refunded">Đã hoàn tiền</option>
            </select>
          </div>

          {/* Action Buttons */}
          <div className="flex items-end">
            <button
              onClick={clearFilters}
              className="w-full bg-gray-100 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>

      {/* Table Section - Loading hiển thị ở đây */}
      <div className="relative min-h-[400px]">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : null}

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Mã giao dịch
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Đơn hàng
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Số tiền
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Phương thức
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Trạng thái
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Ngày
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {currentPayments.length > 0 ? (
                  currentPayments.map((payment) => (
                    <tr key={payment._id || payment.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">{payment.transactionCode || payment.transactionId || 'N/A'}</div>
                        <div className="text-sm text-gray-500">{payment.description || 'Không có mô tả'}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {payment.orderId ? (
                          <span className="text-blue-600 hover:text-blue-800 cursor-pointer">
                            {typeof payment.orderId === 'object' ? payment.orderId.orderCode || payment.orderId._id : payment.orderId}
                          </span>
                        ) : 'N/A'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {formatCurrency(payment.amount || 0)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        <span className="capitalize">
                          {payment.method === 'cod' ? 'Thanh toán khi nhận hàng' :
                            payment.method === 'vnpay' ? 'VNPay' :
                              payment.method === 'momo' ? 'MoMo' :
                                payment.method === 'bank_transfer' ? 'Chuyển khoản' :
                                  payment.method || 'N/A'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(payment.status)}`}>
                          {getStatusText(payment.status)}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {payment.createdAt ? new Date(payment.createdAt).toLocaleDateString('vi-VN') : 'N/A'}
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="6" className="px-6 py-8 text-center text-gray-500">
                      <div className="flex flex-col items-center">
                        <svg className="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
                        </svg>
                        <p className="text-lg font-medium text-gray-900 mb-2">Không tìm thấy giao dịch nào</p>
                        <p className="text-sm text-gray-500">Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex justify-center mt-6">
            <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
              <button
                onClick={() => handlePageChange(Math.max(1, currentPage - 1))}
                disabled={currentPage === 1}
                className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
              >
                Trước
              </button>
              {[...Array(totalPages)].map((_, i) => (
                <button
                  key={i}
                  onClick={() => handlePageChange(i + 1)}
                  className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                    currentPage === i + 1
                      ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                      : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                onClick={() => handlePageChange(Math.min(totalPages, currentPage + 1))}
                disabled={currentPage === totalPages}
                className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
              >
                Sau
              </button>
            </nav>
          </div>
        )}
      </div>
    </div>
  );
};

export default PaymentsPage;