import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { voucherAPI } from '../../../services/apiService';

const UpdateVoucherPage = () => {
  const navigate = useNavigate();
  const { id } = useParams(); // Lấy ID voucher từ URL
  const [loading, setLoading] = useState(false);
  const [initialLoading, setInitialLoading] = useState(true);

  // State lưu dữ liệu Form
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    description: '',
    type: 'FIXED_AMOUNT',
    value: 0,
    minOrderAmount: 0,
    maxDiscountAmount: 0,
    usageLimit: 100,
    startDate: '',
    endDate: '',
    isActive: true
  });

  // 1. Load dữ liệu Voucher cũ khi vào trang
  useEffect(() => {
    const fetchVoucher = async () => {
      try {
        const res = await voucherAPI.getVoucherById(id);
        const data = res?.data?.data || res?.data;

        if (data) {
          setFormData({
            code: data.code || '',
            name: data.name || '',
            description: data.description || '',
            // Đảm bảo type luôn viết hoa để khớp với select box
            type: (data.type || 'FIXED_AMOUNT').toUpperCase(),
            value: data.value || 0,
            minOrderAmount: data.minOrderAmount || 0,
            maxDiscountAmount: data.maxDiscountAmount || 0,
            usageLimit: data.usageLimit || 0,
            // Cắt chuỗi ISO (2025-01-01T10:00:00) thành format input (2025-01-01T10:00)
            startDate: data.startDate ? data.startDate.slice(0, 16) : '',
            endDate: data.endDate ? data.endDate.slice(0, 16) : '',
            isActive: data.isActive
          });
        }
      } catch (error) {
        console.error("Lỗi tải voucher:", error);
        alert("Không thể tải thông tin voucher!");
        navigate('/admin/vouchers');
      } finally {
        setInitialLoading(false);
      }
    };

    fetchVoucher();
  }, [id, navigate]);

  // 2. Xử lý khi nhập liệu
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  // 3. Xử lý Submit (QUAN TRỌNG: Sửa lỗi Enum tại đây)
  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Chuẩn bị dữ liệu gửi đi
      const payload = {
        ...formData,
        value: Number(formData.value),
        minOrderAmount: Number(formData.minOrderAmount),
        maxDiscountAmount: Number(formData.maxDiscountAmount),
        usageLimit: Number(formData.usageLimit),
        
        // --- SỬA LỖI Ở ĐÂY: Ép kiểu sang IN HOA ---
        // Backend chỉ nhận "FIXED_AMOUNT" hoặc "PERCENTAGE"
        type: formData.type.toUpperCase(), 
        // ----------------------------------------

        // Chuyển đổi ngày về chuẩn ISO 8601 để Backend đọc được
        startDate: formData.startDate ? new Date(formData.startDate).toISOString() : null,
        endDate: formData.endDate ? new Date(formData.endDate).toISOString() : null
      };

      // Gọi API Update
      await voucherAPI.updateVoucher(id, payload);
      
      alert('Cập nhật voucher thành công!');
      navigate('/admin/vouchers');
      
    } catch (error) {
      console.error("Lỗi cập nhật:", error);
      const msg = error.response?.data?.message || "Có lỗi xảy ra khi cập nhật!";
      alert(msg);
    } finally {
      setLoading(false);
    }
  };

  if (initialLoading) return <div className="p-8 text-center">Đang tải dữ liệu...</div>;

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800">Cập nhật voucher #{id}</h1>
        <button onClick={() => navigate('/admin/vouchers')} className="text-gray-600 hover:underline">
          Quay lại danh sách
        </button>
      </div>

      <form onSubmit={handleSubmit} className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 space-y-6">
        
        {/* Hàng 1: Mã & Tên */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-sm font-medium mb-1">Mã voucher</label>
            <input 
              type="text" name="code"
              className="w-full border rounded px-3 py-2 bg-gray-100 text-gray-500 font-bold uppercase"
              value={formData.code} 
              readOnly // Không cho sửa mã khi đang Update
              title="Không thể thay đổi mã voucher"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Tên chương trình <span className="text-red-500">*</span></label>
            <input 
              type="text" name="name" required
              className="w-full border rounded px-3 py-2"
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
               {formData.type === 'PERCENTAGE' ? '(%)' : '(VNĐ)'}
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
          <label htmlFor="isActive" className="font-medium text-gray-700">Đang hoạt động</label>
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
            {loading ? 'Đang cập nhật...' : 'Lưu thay đổi'}
          </button>
        </div>

      </form>
    </div>
  );
};

export default UpdateVoucherPage;