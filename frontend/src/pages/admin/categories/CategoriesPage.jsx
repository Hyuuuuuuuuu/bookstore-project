import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { categoryAPI, bookAPI } from '../../../services/apiService';

const CategoriesPage = () => {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  
  // Loading state
  const [loading, setLoading] = useState(false);
  
  // Search state
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  
  // Stats map
  const [bookCountByCategory, setBookCountByCategory] = useState(new Map());

  // 1. Debounce Search Term
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // 2. Fetch Data
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch categories và books song song
        const [categoriesResponse, booksResponse] = await Promise.all([
          categoryAPI.getCategories({
            search: debouncedSearchTerm // Dùng giá trị đã debounce
          }),
          // Lấy sách để đếm số lượng (nếu API category chưa trả về count)
          bookAPI.getBooks({ limit: 1000 })
        ]);
        
        const categoriesData = categoriesResponse?.data?.data?.categories || categoriesResponse?.data?.categories || [];
        const booksData = booksResponse?.data?.data?.books || booksResponse?.data?.books || [];
        
        setCategories(categoriesData);

        // Tính toán số lượng sách theo danh mục
        const countMap = new Map();
        if (Array.isArray(booksData)) {
          booksData.forEach(book => {
            const rawCategoryId =
              book?.categoryId?._id ||
              book?.categoryId ||
              book?.category?._id ||
              book?.category?.id ||
              book?.category;

            if (rawCategoryId) {
              const key = String(rawCategoryId);
              countMap.set(key, (countMap.get(key) || 0) + 1);
            }
          });
        }
        setBookCountByCategory(countMap);

      } catch (error) {
        console.error('Error fetching data:', error);
        setCategories([]);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [debouncedSearchTerm]);

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('vi-VN');
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-800';
      case 'inactive': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getBookCount = (categoryId) => {
    if (!categoryId) return 0;
    return bookCountByCategory.get(String(categoryId)) || 0;
  };

  const handleCategoryAction = async (categoryId, action) => {
    try {
      switch (action) {
        case 'view':
          navigate(`/admin/categories/${categoryId}`);
          break;
        case 'edit': // Sửa lỗi logic cũ: navigate sai path
          navigate(`/admin/categories/update/${categoryId}`);
          break;
        case 'delete':
          if (window.confirm('Bạn có chắc chắn muốn xóa danh mục này?')) {
            await categoryAPI.deleteCategory(categoryId);
            alert('Xóa danh mục thành công!');
            
            // Cập nhật UI ngay lập tức
            setCategories(prev => prev.filter(c => (c._id || c.id) !== categoryId));
          }
          break;
        default:
          break;
      }
    } catch (error) {
      console.error(`Error ${action} category:`, error);
      alert(`Lỗi khi thực hiện thao tác. Vui lòng thử lại.`);
    }
  };

  return (
    <div className="space-y-6">
      {/* Search & Filter Section - KHÔNG BAO BỌC BỞI LOADING */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm theo tên hoặc mô tả danh mục..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <div className="flex items-end justify-end">
            <button 
              onClick={() => navigate('/admin/categories/create')}
              className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
            >
              Thêm danh mục mới
            </button>
          </div>
        </div>
      </div>

      {/* Content Section - Chỉ hiển thị Loading ở đây */}
      <div className="relative min-h-[400px]">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : null}

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Danh mục</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Mô tả</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Số sách</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Ngày tạo</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {categories.length > 0 ? (
                categories.map((category) => (
                  <tr key={category._id || category.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{category.name}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-normal text-sm text-gray-600 max-w-xs">
                      {category.description || 'N/A'}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                      {getBookCount(category._id || category.id)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatDate(category.createdAt)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(category.status)}`}>
                        {category.status === 'active' ? 'Hoạt động' : 'Không hoạt động'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-3">
                        <button
                          onClick={() => handleCategoryAction(category._id || category.id, 'view')}
                          className="text-blue-600 hover:text-blue-900"
                        >
                          Xem
                        </button>
                        <button
                          onClick={() => handleCategoryAction(category._id || category.id, 'edit')}
                          className="text-green-600 hover:text-green-900"
                        >
                          Sửa
                        </button>
                        <button
                          onClick={() => handleCategoryAction(category._id || category.id, 'delete')}
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
                  <td colSpan="6" className="px-6 py-8 text-center text-gray-500">
                    Không tìm thấy danh mục nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default CategoriesPage;