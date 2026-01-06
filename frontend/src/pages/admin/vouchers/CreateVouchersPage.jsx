import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { voucherAPI } from '../../../services/apiService';

const CreateVouchersPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    type: 'percentage',
    value: '',
    validFrom: '',
    validTo: '',
    minOrderAmount: '',
    maxDiscountAmount: '',
    usageLimit: '',
    isActive: true
  });
  const [errors, setErrors] = useState({});
  const [autoGenerateCode, setAutoGenerateCode] = useState(true);

  // Mapping from words/phrases in voucher name to prefixes
  const prefixMapping = [
    { keywords: ['khuyến mãi', 'sách', 'khuyến mãi sách'], prefix: 'BOOK' },
    { keywords: ['ưu đãi thành viên', 'thành viên', 'member'], prefix: 'MEMBER' },
    { keywords: ['người dùng mới', 'new user', 'new'], prefix: 'NEWU' },
    { keywords: ['sale cuối năm', 'cuối năm', 'year'], prefix: 'YEAR' },
    { keywords: ['flash sale', 'flash'], prefix: 'FLASH' }
  ];

  const normalize = (s) => (s || '').toString().toLowerCase().normalize('NFKD').replace(/[\u0300-\u036f]/g, '');

  const pickPrefixFromName = (name) => {
    const n = normalize(name);
    for (const m of prefixMapping) {
      for (const kw of m.keywords) {
        if (n.includes(normalize(kw))) return m.prefix;
      }
    }
    // fallback: take first letters of up to 4 words (uppercase)
    const words = (name || '').trim().split(/\s+/).filter(Boolean);
    if (words.length === 0) return 'VCHR';
    if (words.length === 1) {
      // take up to 4 chars of single word
      return words[0].replace(/[^A-Za-z0-9]/g, '').toUpperCase().slice(0, 4) || 'VCHR';
    }
    const letters = words.slice(0, 4).map(w => w[0] ? w[0].toUpperCase() : '').join('');
    return (letters || 'VCHR').toUpperCase();
  };

  const randomCode = (length = 6) => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let out = '';
    for (let i = 0; i < length; i++) out += chars.charAt(Math.floor(Math.random() * chars.length));
    return out;
  };

  const generateVoucherCode = (name) => {
    const prefix = pickPrefixFromName(name || formData.name);
    return `${prefix}-${randomCode(6)}`;
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    const newVal = type === 'checkbox' ? checked : value;
    setFormData(prev => {
      const next = { ...prev, [name]: newVal };
      // if admin changes name and autoGenerateCode enabled, update code automatically
      if (name === 'name' && autoGenerateCode) {
        next.code = generateVoucherCode(newVal);
      }
      return next;
    });
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    
    if (!formData.code.trim()) {
      newErrors.code = 'Mã voucher là bắt buộc';
    } else if (formData.code.trim().length < 3) {
      newErrors.code = 'Mã voucher phải có ít nhất 3 ký tự';
    }
    
    if (!formData.name.trim()) {
      newErrors.name = 'Tên voucher là bắt buộc';
    }
    
    if (formData.type !== 'free_shipping') {
      if (!formData.value || Number(formData.value) <= 0) {
        newErrors.value = 'Giá trị voucher phải lớn hơn 0';
      }
    }
    
    if (!formData.validFrom) {
      newErrors.validFrom = 'Ngày bắt đầu là bắt buộc';
    }
    
    if (!formData.validTo) {
      newErrors.validTo = 'Ngày kết thúc là bắt buộc';
    } else if (formData.validFrom && formData.validTo && new Date(formData.validTo) <= new Date(formData.validFrom)) {
      newErrors.validTo = 'Ngày kết thúc phải sau ngày bắt đầu';
    }
    
    if (formData.type !== 'free_shipping' && formData.minOrderAmount && formData.minOrderAmount < 0) {
      newErrors.minOrderAmount = 'Đơn hàng tối thiểu không được âm';
    }
    
    if (formData.type !== 'free_shipping' && formData.maxDiscountAmount && formData.maxDiscountAmount < 0) {
      newErrors.maxDiscountAmount = 'Giảm giá tối đa không được âm';
    }
    
    if (formData.usageLimit && formData.usageLimit < 1) {
      newErrors.usageLimit = 'Giới hạn sử dụng phải lớn hơn 0';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setLoading(true);
    try {
      const submitData = {
        code: formData.code.trim(),
        name: formData.name.trim(),
        type: formData.type,
        validFrom: formData.validFrom || undefined,
        validTo: formData.validTo || undefined,
        usageLimit: formData.usageLimit ? parseInt(formData.usageLimit) : undefined,
        isActive: Boolean(formData.isActive)
      };

      if (formData.type !== 'free_shipping') {
        submitData.value = formData.value ? parseFloat(formData.value) : undefined;
        submitData.minOrderAmount = formData.minOrderAmount ? parseFloat(formData.minOrderAmount) : undefined;
        submitData.maxDiscountAmount = formData.maxDiscountAmount ? parseFloat(formData.maxDiscountAmount) : undefined;
      }

      await voucherAPI.createVoucher(submitData);
      alert('Tạo voucher thành công!');
      navigate('/admin/vouchers');
    } catch (error) {
      console.error('Error creating voucher:', error);
      const serverMessage = error?.response?.data?.message;
      if (serverMessage) {
        alert(serverMessage);
      } else {
        alert('Có lỗi xảy ra khi tạo voucher. Vui lòng thử lại.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">

      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        <div className="px-6 py-4 border-b border-gray-200">
          <h1 className="text-2xl font-semibold text-gray-900">Tạo voucher mới</h1>
          <p className="text-gray-600">Điền thông tin voucher để tạo mã giảm giá</p>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-6">
          {/* Basic Information */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Mã voucher <span className="text-red-500">*</span>
              </label>
              <div className="flex items-center space-x-2">
                <input
                  type="text"
                  name="code"
                  value={formData.code}
                  onChange={(e) => {
                    // keep uppercase
                    const v = e.target.value.toUpperCase();
                    setFormData(prev => ({ ...prev, code: v }));
                    if (errors.code) setErrors(prev => ({ ...prev, code: '' }));
                  }}
                  className={`flex-1 px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                    errors.code ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Nhập mã voucher (VD: BOOK-7F2K9Q) hoặc bấm Sinh mã"
                  style={{ textTransform: 'uppercase' }}
                />
                <button
                  type="button"
                  onClick={() => {
                    const code = generateVoucherCode(formData.name);
                    setFormData(prev => ({ ...prev, code }));
                    if (errors.code) setErrors(prev => ({ ...prev, code: '' }));
                  }}
                  className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                >
                  Sinh mã
                </button>
              </div>
              <div className="mt-2 flex items-center space-x-3">
                <label className="flex items-center text-sm text-gray-600">
                  <input
                    type="checkbox"
                    checked={autoGenerateCode}
                    onChange={(e) => setAutoGenerateCode(e.target.checked)}
                    className="mr-2"
                  />
                  Tự động tạo mã từ tên
                </label>
                {errors.code && <p className="mt-1 text-sm text-red-500">{errors.code}</p>}
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Tên voucher <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.name ? 'border-red-500' : 'border-gray-300'
                }`}
                placeholder="Nhập tên voucher"
              />
              {errors.name && <p className="mt-1 text-sm text-red-500">{errors.name}</p>}
            </div>
          </div>

          {/* Type and Value */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Loại voucher <span className="text-red-500">*</span>
              </label>
              <select
                name="type"
                value={formData.type}
                onChange={handleInputChange}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
              >
                <option value="percentage">Phần trăm (%)</option>
                <option value="fixed_amount">Tiền mặt (VNĐ)</option>
                <option value="free_shipping">Miễn phí vận chuyển</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Giá trị {formData.type !== 'free_shipping' && <span className="text-red-500">*</span>}
              </label>
              {formData.type !== 'free_shipping' ? (
                <>
                  <input
                    type="number"
                    name="value"
                    value={formData.value}
                    onChange={handleInputChange}
                    min="0"
                    step="0.01"
                    className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                      errors.value ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder={formData.type === 'percentage' ? '10' : '50000'}
                  />
                  {errors.value && <p className="mt-1 text-sm text-red-500">{errors.value}</p>}
                </>
              ) : (
                <div className="w-full px-3 py-2 rounded-lg bg-gray-50 text-sm text-gray-600 border border-gray-200">
                  Tự động miễn phí vận chuyển
                </div>
              )}
            </div>
          </div>

          {/* Validity Period */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Ngày bắt đầu <span className="text-red-500">*</span>
              </label>
              <input
                type="datetime-local"
                name="validFrom"
                value={formData.validFrom}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.validFrom ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.validFrom && <p className="mt-1 text-sm text-red-500">{errors.validFrom}</p>}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Ngày kết thúc <span className="text-red-500">*</span>
              </label>
              <input
                type="datetime-local"
                name="validTo"
                value={formData.validTo}
                onChange={handleInputChange}
                className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                  errors.validTo ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.validTo && <p className="mt-1 text-sm text-red-500">{errors.validTo}</p>}
            </div>
          </div>

          {/* Conditions */}
          {formData.type !== 'free_shipping' && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Đơn hàng tối thiểu (VNĐ)
                </label>
                <input
                  type="number"
                  name="minOrderAmount"
                  value={formData.minOrderAmount}
                  onChange={handleInputChange}
                  min="0"
                  step="1000"
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                    errors.minOrderAmount ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="100000"
                />
                {errors.minOrderAmount && <p className="mt-1 text-sm text-red-500">{errors.minOrderAmount}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Giảm giá tối đa (VNĐ)
                </label>
                <input
                  type="number"
                  name="maxDiscountAmount"
                  value={formData.maxDiscountAmount}
                  onChange={handleInputChange}
                  min="0"
                  step="1000"
                  className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                    errors.maxDiscountAmount ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="50000"
                />
                {errors.maxDiscountAmount && <p className="mt-1 text-sm text-red-500">{errors.maxDiscountAmount}</p>}
              </div>
            </div>
          )}

          {/* Usage Limit */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Giới hạn sử dụng
            </label>
            <input
              type="number"
              name="usageLimit"
              value={formData.usageLimit}
              onChange={handleInputChange}
              min="1"
              className={`w-full px-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 ${
                errors.usageLimit ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="100 (để trống = không giới hạn)"
            />
            {errors.usageLimit && <p className="mt-1 text-sm text-red-500">{errors.usageLimit}</p>}
            <p className="mt-1 text-sm text-gray-500">
              Số lần tối đa voucher có thể được sử dụng
            </p>
          </div>

          {/* Active Status */}
          <div className="flex items-center">
            <input
              type="checkbox"
              name="isActive"
              checked={formData.isActive}
              onChange={handleInputChange}
              className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
            />
            <label className="ml-2 block text-sm text-gray-900">
              Voucher đang hoạt động
            </label>
          </div>

          {/* Preview */}
          <div className="bg-gray-50 p-4 rounded-lg">
            <h3 className="text-sm font-medium text-gray-700 mb-2">Xem trước voucher:</h3>
            <div className="bg-white p-3 rounded border">
              <div className="flex justify-between items-center">
                <div>
                  <h4 className="font-medium text-gray-900">{formData.code || 'MÃ_VOUCHER'}</h4>
                  <p className="text-sm text-gray-600">{formData.name || 'Tên voucher'}</p>
                </div>
                <div className="text-right">
                  <p className="text-sm text-gray-900">
                    {formData.type === 'percentage' 
                      ? `${formData.value || 0}%` 
                      : formData.type === 'free_shipping'
                      ? 'Miễn phí vận chuyển'
                      : `${formData.value || 0} VNĐ`
                    }
                  </p>
                  <p className="text-xs text-gray-500">
                    {formData.validFrom && formData.validTo 
                      ? `${new Date(formData.validFrom).toLocaleDateString('vi-VN')} - ${new Date(formData.validTo).toLocaleDateString('vi-VN')}`
                      : 'Chưa chọn thời gian'
                    }
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Submit Buttons */}
          <div className="flex justify-end space-x-4 pt-6 border-t border-gray-200">
            <button
              type="button"
              onClick={() => navigate('/admin/vouchers')}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded-lg hover:bg-gray-200 focus:ring-2 focus:ring-gray-500 focus:ring-offset-2"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-lg hover:bg-blue-700 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Đang tạo...' : 'Tạo voucher'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateVouchersPage;
