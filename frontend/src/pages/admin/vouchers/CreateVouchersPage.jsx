import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { voucherAPI } from '../../../services/apiService';

const CreateVoucherPage = () => {
  const navigate = useNavigate();
  const { id } = useParams(); // Lấy ID từ URL nếu đang ở chế độ Sửa
  const isEditMode = Boolean(id);

  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    type: 'FIXED_AMOUNT', // Mặc định
    value: 0,
    minOrderAmount: 0,
    maxDiscountAmount: 0,
    usageLimit: 100,
    startDate: '', // Format: YYYY-MM-DDTHH:mm
    endDate: '',   // Format: YYYY-MM-DDTHH:mm
    isActive: true
  });

  // Load dữ liệu nếu đang sửa
  useEffect(() => {
    if (isEditMode) {
      const fetchVoucher = async () => {
        try {
          const res = await voucherAPI.getVoucherById(id);
          const data = res?.data?.data || res?.data;
          
          if (data) {
            // Fill dữ liệu vào form
            setFormData({
              code: data.code || '',
              name: data.name || '',
              description: data.description || '',
              type: data.type || 'FIXED_AMOUNT',
              value: data.value || 0,
              minOrderAmount: data.minOrderAmount || 0,
              maxDiscountAmount: data.maxDiscountAmount || 0,
              usageLimit: data.usageLimit || 100,
              // Chuyển đổi ngày từ ISO về format input datetime-local
              startDate: data.startDate ? data.startDate.slice(0, 16) : '', 
              endDate: data.endDate ? data.endDate.slice(0, 16) : '',
              isActive: data.isActive
            });
          }
        } catch (error) {
          console.error("Lỗi tải voucher:", error);
          alert("Không thể tải thông tin voucher!");
        }
      };
      fetchVoucher();
    }
  }, [id, isEditMode]);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Chuẩn bị dữ liệu gửi đi (Payload)
      // Đảm bảo tên trường khớp 100% với VoucherDTO ở Backend
      const payload = {
        ...formData,
        // Ép kiểu số để tránh gửi string
        value: Number(formData.value),
        minOrderAmount: Number(formData.minOrderAmount),
        maxDiscountAmount: Number(formData.maxDiscountAmount),
        usageLimit: Number(formData.usageLimit),
        // Backend cần format ISO chuẩn (yyyy-MM-ddTHH:mm:ss)
        startDate: formData.startDate ? new Date(formData.startDate).toISOString() : null,
        endDate: formData.endDate ? new Date(formData.endDate).toISOString() : null
      };

      if (isEditMode) {
        await voucherAPI.updateVoucher(id, payload);
        alert('Cập nhật voucher thành công!');
      } else {
        await voucherAPI.createVoucher(payload);
        alert('Tạo voucher mới thành công!');
      }
      navigate('/admin/vouchers'); // Quay về danh sách
      
    } catch (error) {
      console.error("Submit error:", error);
      const msg = error.response?.data?.message || "Có lỗi xảy ra, vui lòng kiểm tra lại!";
      alert(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">
          {isEditMode ? `Cập nhật voucher #${id}` : 'Tạo voucher mới'}
        </h1>
        <button onClick={() => navigate('/admin/vouchers')} className="text-gray-600 hover:underline">
          Quay lại danh sách
        </button>
      </div>

      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 space-y-6">
        
        {/* Hàng 1: Mã & Tên */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium mb-1">Mã voucher <span className="text-red-500">*</span></label>
            <input 
              type="text" name="code" required
              className="w-full border rounded px-3 py-2 uppercase font-bold tracking-wider"
              placeholder="VD: SUMMER2024"
              value={formData.code} onChange={handleChange}
              disabled={isEditMode} // Không cho sửa mã khi update
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Tên chương trình <span className="text-red-500">*</span></label>
            <input 
              type="text" name="name" required
              className="w-full border rounded px-3 py-2"
              placeholder="VD: Khuyến mãi mùa hè"
              value={formData.name} onChange={handleChange}
            />
          </div>
        </div>

        {/* Mô tả */}
        <div>
          <label className="block text-sm font-medium mb-1">Mô tả</label>
          <textarea 
            name="description" rows="2"
            className="w-full border rounded px-3 py-2"
            value={formData.description} onChange={handleChange}
          ></textarea>
        </div>

        {/* Hàng 2: Loại & Giá trị */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div>
            <label className="block text-sm font-medium mb-1">Loại giảm giá</label>
            <select name="type" className="w-full border rounded px-3 py-2" value={formData.type} onChange={handleChange}>
              <option value="FIXED_AMOUNT">Giảm tiền mặt (VNĐ)</option>
              <option value="PERCENTAGE">Giảm phần trăm (%)</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Giá trị giảm <span className="text-red-500">*</span></label>
            <input 
              type="number" name="value" required min="0"
              className="w-full border rounded px-3 py-2 font-bold text-green-600"
              value={formData.value} onChange={handleChange}
            />
            <p className="text-xs text-gray-500 mt-1">
               {formData.type === 'PERCENTAGE' ? 'Nhập số % (VD: 10)' : 'Nhập số tiền (VD: 50000)'}
            </p>
          </div>
          <div>
             <label className="block text-sm font-medium mb-1">Giảm tối đa (Cho %)</label>
             <input 
               type="number" name="maxDiscountAmount" min="0"
               className="w-full border rounded px-3 py-2"
               value={formData.maxDiscountAmount} onChange={handleChange}
               disabled={formData.type === 'FIXED_AMOUNT'}
             />
          </div>
        </div>

        {/* Hàng 3: Thời gian */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium mb-1">Ngày bắt đầu <span className="text-red-500">*</span></label>
            <input 
              type="datetime-local" name="startDate" required
              className="w-full border rounded px-3 py-2"
              value={formData.startDate} onChange={handleChange}
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Ngày kết thúc <span className="text-red-500">*</span></label>
            <input 
              type="datetime-local" name="endDate" required
              className="w-full border rounded px-3 py-2"
              value={formData.endDate} onChange={handleChange}
            />
          </div>
        </div>

        {/* Hàng 4: Điều kiện & Giới hạn */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium mb-1">Đơn tối thiểu (VNĐ)</label>
            <input 
              type="number" name="minOrderAmount" min="0"
              className="w-full border rounded px-3 py-2"
              value={formData.minOrderAmount} onChange={handleChange}
            />
          </div>
          <div>
             <label className="block text-sm font-medium mb-1">Tổng lượt sử dụng</label>
             <input 
               type="number" name="usageLimit" min="1"
               className="w-full border rounded px-3 py-2"
               value={formData.usageLimit} onChange={handleChange}
             />
          </div>
        </div>

        {/* Checkbox Active */}
        <div className="flex items-center space-x-2">
          <input 
            type="checkbox" id="isActive" name="isActive"
            className="w-5 h-5 text-blue-600 rounded"
            checked={formData.isActive} onChange={handleChange}
          />
          <label htmlFor="isActive" className="font-medium text-gray-700">Kích hoạt voucher này ngay</label>
        </div>

        {/* Nút Submit */}
        <div className="flex justify-end space-x-3 pt-4 border-t">
          <button 
            type="button" 
            onClick={() => navigate('/admin/vouchers')}
            className="px-4 py-2 text-gray-700 bg-gray-100 rounded hover:bg-gray-200"
          >
            Hủy bỏ
          </button>
          <button 
            type="submit" 
            disabled={loading}
            className="px-6 py-2 text-white bg-blue-600 rounded hover:bg-blue-700 font-medium disabled:opacity-50"
          >
            {loading ? 'Đang xử lý...' : (isEditMode ? 'Cập nhật' : 'Tạo mới')}
          </button>
        </div>

      </form>
    </div>
  );
};

export default CreateVoucherPage;