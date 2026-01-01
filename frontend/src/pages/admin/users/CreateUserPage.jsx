import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userAPI } from '../../../services/apiService';

const CreateUserPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    phone: '',
    role: 'user'
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await userAPI.createUser(formData); // Cần thêm hàm này vào apiService
      alert('Tạo người dùng thành công!');
      navigate('/admin/users');
    } catch (error) {
      alert(error.response?.data?.message || 'Có lỗi xảy ra');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">Thêm người dùng mới</h1>
        <button onClick={() => navigate('/admin/users')} className="text-gray-600 hover:text-gray-900">
          Quay lại
        </button>
      </div>

      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Họ và tên</label>
            <input
              type="text"
              required
              className="w-full px-3 py-2 border rounded-lg"
              value={formData.name}
              onChange={e => setFormData({...formData, name: e.target.value})}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
            <input
              type="email"
              required
              className="w-full px-3 py-2 border rounded-lg"
              value={formData.email}
              onChange={e => setFormData({...formData, email: e.target.value})}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Mật khẩu</label>
            <input
              type="password"
              required
              minLength={6}
              className="w-full px-3 py-2 border rounded-lg"
              value={formData.password}
              onChange={e => setFormData({...formData, password: e.target.value})}
            />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Số điện thoại</label>
              <input
                type="text"
                className="w-full px-3 py-2 border rounded-lg"
                value={formData.phone}
                onChange={e => setFormData({...formData, phone: e.target.value})}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Vai trò</label>
              <select
                className="w-full px-3 py-2 border rounded-lg"
                value={formData.role}
                onChange={e => setFormData({...formData, role: e.target.value})}
              >
                <option value="user">Người dùng (User)</option>
                <option value="staff">Nhân viên (Staff)</option>
                <option value="admin">Quản trị viên (Admin)</option>
              </select>
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <button
              type="submit"
              disabled={loading}
              className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? 'Đang xử lý...' : 'Tạo người dùng'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateUserPage;