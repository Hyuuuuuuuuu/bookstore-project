import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { categoryAPI, bookAPI } from '../../../services/apiService';

const CategoriesPage = () => {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // State lưu giá trị thực của ô input (để người dùng gõ thoải mái)
  const [searchTermInput, setSearchTermInput] = useState('');
  
  // State lưu giá trị dùng để gọi API (được cập nhật sau khi debounce)
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');

  // Debounce Effect: Chỉ cập nhật debouncedSearchTerm sau khi người dùng ngừng gõ 500ms
  useEffect(() => {
    const timerId = setTimeout(() => {
      setDebouncedSearchTerm(searchTermInput);
    }, 500);

    return () => {
      clearTimeout(timerId);
    };
  }, [searchTermInput]);

  // Fetch Data Effect: Chạy khi debouncedSearchTerm thay đổi
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch categories và books song song
        const [categoriesResponse, booksResponse] = await Promise.all([
          categoryAPI.getCategories({
            search: debouncedSearchTerm // Dùng từ khóa đã debounce để gọi API
          }),
          bookAPI.getBooks()
        ]);
        
        console.log('📂 CategoriesPage API Response:', categoriesResponse);
        console.log('📚 BooksPage API Response:', booksResponse);
        
        const categoriesData = categoriesResponse?.data?.data?.categories || categoriesResponse?.data?.categories || categoriesResponse?.data || [];
        const booksData = booksResponse?.data?.data?.books || booksResponse?.data?.books || booksResponse?.data || [];
        
        setCategories(categoriesData);
        setBooks(booksData);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching data:', error);
        setCategories([]);
        setBooks([]);
        setLoading(false);
      }
    };

    fetchData();
  }, [debouncedSearchTerm]); // Chỉ gọi lại khi debouncedSearchTerm thay đổi

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('vi-VN');
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'active': return 'bg-green-100 text-green-800';
      case 'inactive': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  // Function to count books in a category
  const getBookCount = (categoryId) => {
    if (!Array.isArray(books)) return 0;
    return books.filter(book => book?.categoryId?._id === categoryId || book?.categoryId === categoryId).length;
  };

  // Filter local (nếu cần filter thêm ở client side, nhưng ở đây API đã filter rồi nên có thể bỏ qua hoặc giữ lại để chắc chắn)
  const filteredCategories = Array.isArray(categories) ? categories : [];

  const handleCategoryAction = async (categoryId, action) => {
    try {
      switch (action) {
        case 'view':
          console.log('Viewing category:', categoryId);
          navigate(`/admin/categories/${categoryId}`);
          break;
          
        case 'edit':
          console.log('Editing category:', categoryId);
          navigate(`/admin/categories/update/${categoryId}`);
          break;
          
        case 'delete':
          if (window.confirm('Bạn có chắc chắn muốn xóa danh mục này?')) {
            await categoryAPI.deleteCategory(categoryId);
            console.log('✅ Category deleted:', categoryId);
            
            // Refresh categories list
            const categoriesResponse = await categoryAPI.getCategories({ search: debouncedSearchTerm });
            setCategories(categoriesResponse?.data?.data?.categories || categoriesResponse?.data?.categories || categoriesResponse?.data || []);
          }
          break;
          
        default:
          console.log(`Action ${action} for category ${categoryId}`);
      }
    } catch (error) {
      console.error(`Error ${action} category:`, error);
      alert(`Lỗi khi ${action === 'delete' ? 'xóa' : 'thực hiện'} danh mục. Vui lòng thử lại.`);
    }
  };

  // Debug: Log current state
  console.log('📂 CategoriesPage - Loading:', loading, 'Categories count:', categories.length);

  return (
    <div className="space-y-6">
      {/* Action Buttons */}
      <div className="flex justify-end space-x-4">
        <button 
          onClick={() => navigate('/admin/categories/create')}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          Thêm danh mục mới
        </button>
      </div>

      {/* Search */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="max-w-md">
          <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
          <input
            type="text"
            placeholder="Tìm theo tên hoặc mô tả danh mục..."
            
            // QUAN TRỌNG: Dùng state input trực tiếp
            value={searchTermInput}
            onChange={(e) => setSearchTermInput(e.target.value)}
            
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            autoFocus // Giữ focus khi component mount lần đầu
          />
        </div>
      </div>

      {/* Categories Grid */}
      <div className="relative">
        {loading ? (
          <div className="flex items-center justify-center h-64">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            <p className="ml-4">Đang tải danh mục...</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {Array.isArray(filteredCategories) && filteredCategories.length > 0 ? (
              filteredCategories.map((category) => (
              <div key={category?.id || category?._id || Math.random()} className="bg-white p-6 rounded-lg shadow-sm border border-gray-200 hover:shadow-md transition-shadow">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex-1">
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">{category?.name || 'N/A'}</h3>
                    <p className="text-sm text-gray-600 mb-3 line-clamp-2">{category?.description || 'N/A'}</p>
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <span>{getBookCount(category?._id)} sách</span>
                      <span>•</span>
                      <span>{category?.createdAt ? formatDate(category.createdAt) : 'N/A'}</span>
                    </div>
                  </div>
                  <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(category?.status || 'active')}`}>
                    {category?.status === 'inactive' ? 'Không hoạt động' : 'Hoạt động'}
                  </span>
                </div>
                
                <div className="flex space-x-2">
                  <button
                    onClick={() => handleCategoryAction(category?._id, 'view')}
                    className="flex-1 text-blue-600 hover:text-blue-900 text-sm font-medium py-2 px-3 border border-blue-200 rounded-lg hover:bg-blue-50 transition-colors"
                  >
                    Xem
                  </button>
                  <button
                    onClick={() => navigate(`/admin/categories/update/${category?._id}`)}
                    className="flex-1 text-green-600 hover:text-green-900 text-sm font-medium py-2 px-3 border border-green-200 rounded-lg hover:bg-green-50 transition-colors"
                  >
                    Sửa
                  </button>
                  <button
                    onClick={() => handleCategoryAction(category?._id, 'delete')}
                    className="flex-1 text-red-600 hover:text-red-900 text-sm font-medium py-2 px-3 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
                  >
                    Xóa
                  </button>
                </div>
              </div>
              ))
            ) : (
              <div className="col-span-full flex flex-col items-center justify-center py-12">
                <svg className="w-16 h-16 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
                <p className="text-lg font-medium text-gray-900 mb-2">Không tìm thấy danh mục nào</p>
                <p className="text-sm text-gray-500">Thử tìm kiếm với từ khóa khác hoặc thêm danh mục mới</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default CategoriesPage;