import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { voucherAPI } from '../../../services/apiService'; 

const VouchersPage = () => {
  const navigate = useNavigate();
  const [vouchers, setVouchers] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // State tìm kiếm & lọc
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [filterType, setFilterType] = useState('all');
  const [filterStatus, setFilterStatus] = useState('all');

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(searchTerm), 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  const fetchVouchers = async () => {
    setLoading(true);
    try {
      const res = await voucherAPI.getVouchers({ 
        search: debouncedSearch,
        type: filterType !== 'all' ? filterType : undefined,
        status: filterStatus !== 'all' ? filterStatus : undefined,
        page: 1, limit: 10
      });
      setVouchers(res?.data?.data?.vouchers || []);
    } catch (error) {
      console.error("Lỗi tải voucher:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVouchers();
  }, [debouncedSearch, filterType, filterStatus]);

  // --- HÀM MỚI: Xử lý đổi trạng thái nhanh ---
  const handleToggleStatus = async (voucher) => {
    try {
      // Gọi API update chỉ sửa trường isActive
      // Lưu ý: Backend cần API update hỗ trợ partial update hoặc bạn gửi full object
      // Ở đây ta gửi object đã sửa isActive để an toàn với API Update hiện tại
      const updatedVoucher = { 
          ...voucher, 
          isActive: !voucher.isActive,
          // Đảm bảo gửi đúng tên trường cho Backend
          startDate: voucher.startDate, 
          endDate: voucher.endDate
      };
      
      await voucherAPI.updateVoucher(voucher.id, updatedVoucher);
      
      // Cập nhật lại giao diện ngay lập tức
      setVouchers(vouchers.map(v => 
        v.id === voucher.id ? { ...v, isActive: !v.isActive } : v
      ));
    } catch (error) {
      alert('Không thể cập nhật trạng thái: ' + (error.response?.data?.message || 'Lỗi server'));
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Bạn có chắc chắn muốn xóa voucher này?')) {
      try {
        await voucherAPI.deleteVoucher(id);
        alert('Xóa thành công!');
        fetchVouchers();
      } catch (error) {
        alert('Lỗi khi xóa voucher');
      }
    }
  };

  const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
  const formatDate = (date) => date ? new Date(date).toLocaleDateString('vi-VN') : 'N/A';

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
         <h1 className="text-2xl font-bold text-gray-800">Quản lý voucher</h1>
         <button 
           onClick={() => navigate('/admin/vouchers/create')}
           className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 font-medium shadow-sm"
         >
           + Tạo voucher mới
         </button>
      </div>

      {/* Bộ lọc */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
           <label className="block text-sm font-medium mb-1 text-gray-700">Tìm kiếm</label>
           <input 
             type="text" placeholder="Nhập mã voucher..."
             className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 outline-none"
             value={searchTerm}
             onChange={e => setSearchTerm(e.target.value)}
           />
        </div>
        <div>
           <label className="block text-sm font-medium mb-1 text-gray-700">Loại voucher</label>
           <select 
             className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 outline-none"
             value={filterType}
             onChange={e => setFilterType(e.target.value)}
           >
             <option value="all">Tất cả</option>
             <option value="PERCENTAGE">Phần trăm (%)</option>
             <option value="FIXED_AMOUNT">Tiền mặt (VNĐ)</option>
           </select>
        </div>
        <div>
           <label className="block text-sm font-medium mb-1 text-gray-700">Trạng thái</label>
           <select 
             className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500 outline-none"
             value={filterStatus}
             onChange={e => setFilterStatus(e.target.value)}
           >
             <option value="all">Tất cả</option>
             <option value="true">Hoạt động</option>
             <option value="false">Ngừng hoạt động</option>
           </select>
        </div>
      </div>

      {/* Bảng danh sách */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
             <tr>
               <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Voucher</th>
               <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Loại</th>
               <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Giá trị</th>
               <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Hạn dùng</th>
               <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">Sử dụng</th>
               <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase">Trạng thái</th>
               <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">Thao tác</th>
             </tr>
          </thead>
          <tbody className="divide-y divide-gray-200">
             {loading ? <tr><td colSpan="7" className="text-center py-8 text-gray-500">Đang tải dữ liệu...</td></tr> : 
              vouchers.length > 0 ? vouchers.map(v => (
               <tr key={v.id} className="hover:bg-gray-50">
                 <td className="px-6 py-4">
                    <div className="font-bold text-blue-600">{v.code}</div>
                    <div className="text-xs text-gray-500 max-w-[200px] truncate" title={v.name}>{v.name}</div>
                 </td>
                 
                 {/* CẬP NHẬT: Hiển thị đúng loại voucher từ Enum */}
                 <td className="px-6 py-4 text-sm text-gray-700">
                    {v.type === 'PERCENTAGE' ? 'Phần trăm' : 'Tiền mặt'}
                 </td>

                 {/* CẬP NHẬT: Sửa lỗi NaN - Dùng v.value thay vì v.discountValue */}
                 <td className="px-6 py-4 font-bold text-green-600">
                    {v.type === 'PERCENTAGE' ? `${v.value}%` : formatCurrency(v.value)}
                 </td>

                 <td className="px-6 py-4 text-xs text-gray-500">
                    <div>Từ: {formatDate(v.startDate)}</div>
                    <div>Đến: {formatDate(v.endDate)}</div>
                 </td>
                 <td className="px-6 py-4 text-sm">
                    {v.usageCount} / {v.usageLimit || '∞'}
                 </td>
                 
                 {/* CẬP NHẬT: Cho phép bấm vào để đổi trạng thái */}
                 <td className="px-6 py-4 text-center">
                    <button 
                        onClick={() => handleToggleStatus(v)}
                        className={`px-3 py-1 text-xs rounded-full font-medium transition-colors cursor-pointer ${
                            v.isActive 
                            ? 'bg-green-100 text-green-800 hover:bg-green-200' 
                            : 'bg-red-100 text-red-800 hover:bg-red-200'
                        }`}
                        title="Bấm để đổi trạng thái"
                    >
                      {v.isActive ? 'Hoạt động' : 'Đã khóa'}
                    </button>
                 </td>

                 <td className="px-6 py-4 text-right space-x-3">
                    <button 
                      onClick={() => navigate(`/admin/vouchers/update/${v.id}`)}
                      className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                    >
                      Sửa
                    </button>
                    <button 
                      onClick={() => handleDelete(v.id)}
                      className="text-red-600 hover:text-red-800 text-sm font-medium"
                    >
                      Xóa
                    </button>
                 </td>
               </tr>
             )) : (
                 <tr><td colSpan="7" className="text-center py-8 text-gray-500">Không tìm thấy voucher nào</td></tr>
             )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default VouchersPage;