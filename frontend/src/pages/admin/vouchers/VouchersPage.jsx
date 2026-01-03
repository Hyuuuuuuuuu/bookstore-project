import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { voucherAPI } from '../../../services/apiService';

const VouchersPage = () => {
  const navigate = useNavigate();
  const [vouchers, setVouchers] = useState([]);
  
  // Loading state
  const [loading, setLoading] = useState(false);
  
  // Search & Filter
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [filterType, setFilterType] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');
  
  // Pagination
  const [currentPage, setCurrentPage] = useState(1);
  const [vouchersPerPage] = useState(10);

  // 1. Debounce Search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // 2. Fetch Vouchers
  useEffect(() => {
    const fetchVouchers = async () => {
      setLoading(true);
      try {
        const response = await voucherAPI.getVouchers({
          page: currentPage,
          limit: vouchersPerPage,
          search: debouncedSearchTerm,
          type: filterType !== 'all' ? filterType : undefined,
          status: filterStatus !== 'all' ? filterStatus : undefined
        });
        
        const vouchersData = response?.data?.data?.vouchers || response?.data?.vouchers || [];
        setVouchers(vouchersData);
      } catch (error) {
        console.error('Error fetching vouchers:', error);
        setVouchers([]);
      } finally {
        setLoading(false);
      }
    };
    
    fetchVouchers();
  }, [currentPage, debouncedSearchTerm, filterType, filterStatus]);

  const handleStatusChange = async (voucherId, newStatus) => {
    try {
      // Cập nhật trạng thái
      await voucherAPI.updateVoucher(voucherId, { isActive: newStatus });

      // Refresh list
      const response = await voucherAPI.getVouchers({
        page: currentPage,
        limit: vouchersPerPage,
        search: debouncedSearchTerm,
        type: filterType !== 'all' ? filterType : undefined,
        status: filterStatus !== 'all' ? filterStatus : undefined
      });

      const vouchersData = response?.data?.data?.vouchers || response?.data?.vouchers || [];
      setVouchers(vouchersData);
      
      alert('Cập nhật trạng thái thành công!');
    } catch (error) {
      console.error('Error updating voucher status:', error);
      alert('Có lỗi xảy ra khi cập nhật trạng thái voucher');
    }
  };

  const handleDeleteVoucher = async (voucherId) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa voucher này?')) {
      try {
        await voucherAPI.deleteVoucher(voucherId);
        
        // Cập nhật UI ngay lập tức
        setVouchers(prev => prev.filter(v => (v._id || v.id) !== voucherId));
        alert('Xóa voucher thành công!');
      } catch (error) {
        console.error('Error deleting voucher:', error);
        alert('Có lỗi xảy ra khi xóa voucher!');
      }
    }
  };

  const getTypeName = (type) => {
    const normalizedType = (type || '').toString().toLowerCase();
    switch (normalizedType) {
      case 'percentage': return 'Phần trăm';
      case 'fixed_amount': return 'Tiền mặt';
      case 'free_shipping': return 'Miễn phí vận chuyển';
      default: return type;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-800';
      case 'expired': return 'bg-yellow-100 text-yellow-800';
      case 'inactive': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'active': return 'Hoạt động';
      case 'expired': return 'Hết hạn';
      case 'inactive': return 'Không hoạt động';
      default: return status;
    }
  };

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

  const isExpired = (validTo) => {
    if (!validTo) return false;
    return new Date(validTo) < new Date();
  };

  // Pagination logic (client side if API returns all, otherwise rely on API pagination)
  // Assuming API handles pagination for now, but if not we can add slicing here
  // const indexOfLastVoucher = currentPage * vouchersPerPage; ...

  return (
    <div className="space-y-6">
      {/* Filters Section - KHÔNG bọc bởi loading */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm kiếm voucher..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Loại voucher</label>
            <select
              value={filterType}
              onChange={(e) => {
                setFilterType(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">Tất cả</option>
              <option value="percentage">Phần trăm</option>
              <option value="fixed_amount">Tiền mặt</option>
              <option value="free_shipping">Miễn phí vận chuyển</option>
            </select>
          </div>
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
              <option value="all">Tất cả</option>
              <option value="active">Hoạt động</option>
              <option value="expired">Hết hạn</option>
              <option value="inactive">Không hoạt động</option>
            </select>
          </div>
          <div className="flex items-end justify-end">
            <button
              onClick={() => navigate('/admin/vouchers/create')}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors whitespace-nowrap"
            >
              Tạo voucher mới
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
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Voucher</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Loại</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Giá trị</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thời gian</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Sử dụng</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {vouchers.length > 0 ? (
                  vouchers.map((voucher) => (
                    <tr key={voucher._id || voucher.id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div>
                          <div className="text-sm font-medium text-gray-900">{voucher.code}</div>
                          <div className="text-sm text-gray-500 truncate max-w-[150px]" title={voucher.name}>{voucher.name}</div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {getTypeName(voucher.type)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900 font-medium">
                        {((voucher.type || '').toString().toLowerCase()) === 'percentage'
                          ? `${voucher.value}%`
                          : ((voucher.type || '').toString().toLowerCase()) === 'free_shipping'
                          ? 'Free ship'
                          : formatCurrency(voucher.value)
                        }
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        <div className="flex flex-col">
                          <span>Từ: {formatDate(voucher.validFrom)}</span>
                          <span>Đến: {formatDate(voucher.validTo)}</span>
                          {isExpired(voucher.validTo) && (
                            <span className="text-red-500 text-xs font-medium">(Đã hết hạn)</span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                        {voucher.usedCount || 0} / {voucher.usageLimit || '∞'}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <select
                          value={voucher.status}
                          onChange={(e) => handleStatusChange(voucher._id || voucher.id, e.target.value === 'active')}
                          className={`text-xs font-medium px-3 py-1 rounded-full border-0 focus:ring-2 focus:ring-blue-500 cursor-pointer ${getStatusColor(voucher.status)}`}
                          disabled={voucher.status === 'expired'}
                        >
                          <option value="active">Hoạt động</option>
                          <option value="inactive">Không hoạt động</option>
                          <option value="expired" disabled>Hết hạn</option>
                        </select>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex space-x-3">
                          <button
                            onClick={() => navigate(`/admin/vouchers/${voucher._id || voucher.id}`)}
                            className="text-green-600 hover:text-green-900"
                          >
                            Xem
                          </button>
                          <button
                            onClick={() => navigate(`/admin/vouchers/update/${voucher._id || voucher.id}`)}
                            className="text-blue-600 hover:text-blue-900"
                          >
                            Sửa
                          </button>
                          <button
                            onClick={() => handleDeleteVoucher(voucher._id || voucher.id)}
                            className="text-red-600 hover:text-red-900"
                          >
                            Xóa
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan="7" className="px-6 py-12 text-center text-gray-500">
                      <div className="flex flex-col items-center">
                        <svg className="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
                        </svg>
                        <p className="text-lg font-medium text-gray-900 mb-1">Không tìm thấy voucher nào</p>
                        <p className="text-sm text-gray-500">Thử thay đổi bộ lọc hoặc tạo voucher mới</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Pagination Controls could be added here similar to other pages */}
        <div className="mt-4 flex justify-center">
          <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
            <button
              onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
              disabled={currentPage === 1}
              className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
            >
              Trước
            </button>
            <span className="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
              Trang {currentPage}
            </span>
            <button
              onClick={() => setCurrentPage(p => p + 1)}
              disabled={vouchers.length < vouchersPerPage} // Disable if less than limit items returned
              className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
            >
              Sau
            </button>
          </nav>
        </div>
      </div>
    </div>
  );
};

export default VouchersPage;