import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom'; // Import hook
import { userAPI } from '../../../services/apiService';

const UsersPage = () => {
  const navigate = useNavigate(); // Khởi tạo hook
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // --- 1. Debounce Search ---
  const [searchTermInput, setSearchTermInput] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  
  const [filterRole, setFilterRole] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const [usersPerPage] = useState(10);
  const [pagination, setPagination] = useState({});

  // Debounce Effect
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTermInput);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTermInput]);

  // --- 2. Fetch Users ---
  useEffect(() => {
    const fetchUsers = async () => {
      setLoading(true);
      try {
        const response = await userAPI.getUsers({
          page: currentPage,
          limit: usersPerPage,
          search: debouncedSearchTerm, // Dùng biến đã debounce
          role: filterRole !== 'all' ? filterRole : undefined
        });
        
        console.log('👥 Users Response:', response);
        
        // Handle cấu trúc trả về từ UserService
        const data = response?.data?.data || response?.data || {};
        setUsers(data.users || []);
        setPagination(data.pagination || {});
        
      } catch (error) {
        console.error('Error fetching users:', error);
        setUsers([]);
      } finally {
        setLoading(false);
      }
    };

    fetchUsers();
  }, [currentPage, debouncedSearchTerm, filterRole]); 

  const formatDate = (dateString) => new Date(dateString).toLocaleDateString('vi-VN');
  const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  
  const getRoleColor = (role) => {
      switch(role) {
          case 'admin': return 'bg-purple-100 text-purple-800';
          case 'staff': return 'bg-orange-100 text-orange-800';
          default: return 'bg-blue-100 text-blue-800';
      }
  };
  
  const getStatusColor = (status) => (status === 'ACTIVE' || status === 'active') ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800';

  const handleUserAction = (userId, action) => {
      console.log(`Action ${action} for user ${userId}`);
      if (action === 'delete') {
          if(window.confirm('Bạn có chắc chắn muốn xóa người dùng này?')) {
              // Gọi API delete ở đây nếu cần
              // await userAPI.deleteUser(userId);
              // fetchUsers(); // Refresh list
          }
      }
  };

  const handlePageChange = (newPage) => {
      if (newPage >= 1 && newPage <= (pagination.totalPages || 1)) {
          setCurrentPage(newPage);
      }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="ml-4">Đang tải người dùng...</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Action Button */}
      <div className="flex justify-end">
        <button 
            onClick={() => navigate('/admin/users/create')} // Thêm sự kiện onClick
            className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          Thêm người dùng
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tên, Email, SĐT..."
              value={searchTermInput}
              onChange={(e) => setSearchTermInput(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Vai trò</label>
            <select
              value={filterRole}
              onChange={(e) => setFilterRole(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">Tất cả</option>
              <option value="admin">Quản trị viên</option>
              <option value="staff">Nhân viên</option>
              <option value="user">Người dùng</option>
            </select>
          </div>
          <div className="flex items-end">
            <button
              onClick={() => { setSearchTermInput(''); setFilterRole('all'); }}
              className="w-full bg-gray-100 text-gray-700 px-4 py-2 rounded-lg hover:bg-gray-200 transition-colors"
            >
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Người dùng
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Vai trò
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Trạng thái
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Ngày tham gia
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Thao tác
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {users.length > 0 ? (
                users.map((user) => (
                <tr key={user.id || Math.random()} className="hover:bg-gray-50">
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                        <span className="text-blue-600 font-medium text-sm">
                          {user.name?.charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <div className="ml-4">
                        <div className="text-sm font-medium text-gray-900">{user.name}</div>
                        <div className="text-sm text-gray-500">{user.email}</div>
                        <div className="text-sm text-gray-500">{user.phone}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getRoleColor(user.role)}`}>
                      {user.role === 'admin' ? 'Quản trị viên' : user.role === 'staff' ? 'Nhân viên' : 'Người dùng'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(user.status || (user.isActive ? 'active' : 'inactive'))}`}>
                      {(user.status === 'ACTIVE' || user.status === 'active' || user.isActive) ? 'Hoạt động' : 'Không hoạt động'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    {user.createdAt ? formatDate(user.createdAt) : 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                    <div className="flex space-x-2">
                      <button 
                        onClick={() => handleUserAction(user.id, 'edit')}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        Sửa
                      </button>
                      <button 
                        onClick={() => handleUserAction(user.id, 'delete')}
                        className="text-red-600 hover:text-red-900"
                      >
                        Xóa
                      </button>
                    </div>
                  </td>
                </tr>
                ))
              ) : (
                <tr><td colSpan="5" className="px-6 py-8 text-center text-gray-500">
                    <div className="flex flex-col items-center">
                      <svg className="w-12 h-12 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                      </svg>
                      <p className="text-lg font-medium text-gray-900 mb-2">Không có người dùng nào</p>
                      <p className="text-sm text-gray-500">Chưa có người dùng nào phù hợp với bộ lọc</p>
                    </div>
                </td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination UI */}
        {pagination.totalPages > 1 && (
          <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
            <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
              <div>
                <p className="text-sm text-gray-700">
                  Hiển thị trang <span className="font-medium">{currentPage}</span> / <span className="font-medium">{pagination.totalPages}</span>
                </p>
              </div>
              <div>
                <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
                  <button
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                    className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                  >
                    Trước
                  </button>
                  <button
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === pagination.totalPages}
                    className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
                  >
                    Sau
                  </button>
                </nav>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default UsersPage;