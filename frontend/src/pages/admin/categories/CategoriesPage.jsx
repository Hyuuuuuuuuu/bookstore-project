import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { categoryAPI, bookAPI } from '../../../services/apiService';

const CategoriesPage = () => {
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [newCategory, setNewCategory] = useState({ name: '', description: '' });
  const [editingCategory, setEditingCategory] = useState(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [bookCountByCategory, setBookCountByCategory] = useState(new Map());

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch categories and books in parallel
        const [categoriesResponse, booksResponse] = await Promise.all([
          categoryAPI.getCategories({
            search: searchTerm
          }),
          // Fetch many books so we can count per category (backend may paginate)
          bookAPI.getBooks({ limit: 1000 })
        ]);
        
        console.log('📂 CategoriesPage API Response:', categoriesResponse);
        console.log('📚 BooksPage API Response:', booksResponse);
        
        const categoriesData = categoriesResponse?.data?.data?.categories || categoriesResponse?.data?.categories || categoriesResponse?.data || [];
        const booksData = booksResponse?.data?.data?.books || booksResponse?.data?.books || booksResponse?.data || [];
        
        setCategories(categoriesData);
        setBooks(booksData);

        // Create a map for book counts by category for better performance
        // Normalize keys to strings to avoid mismatches between ObjectId objects and string IDs
        const countMap = new Map();
        if (Array.isArray(booksData)) {
          booksData.forEach(book => {
            // Support multiple backend shapes:
            // - Mongo-like: book.categoryId._id or book.categoryId
            // - Java DTO: book.category.id or book.category
            const rawCategoryId =
              book?.categoryId?._id ||
              book?.categoryId ||
              book?.category?._id ||
              book?.category?.id ||
              book?.category;

            if (rawCategoryId !== undefined && rawCategoryId !== null) {
              const key = String(rawCategoryId);
              countMap.set(key, (countMap.get(key) || 0) + 1);
            }
          });
        }
        setBookCountByCategory(countMap);

        setLoading(false);
      } catch (error) {
        console.error('Error fetching data:', error);
        setCategories([]);
        setBooks([]);
        setLoading(false);
      }
    };

    fetchData();
  }, [searchTerm]);

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

  // Function to count books in a category (normalize lookup key to string)
  const getBookCount = (categoryId) => {
    if (categoryId === undefined || categoryId === null) return 0;
    return bookCountByCategory.get(String(categoryId)) || 0;
  };

  const filteredCategories = Array.isArray(categories) ? categories.filter(category =>
    category?.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    category?.description?.toLowerCase().includes(searchTerm.toLowerCase())
  ) : [];


  const handleCategoryAction = async (categoryId, action) => {
    try {
      switch (action) {
        case 'view':
          console.log('Viewing category:', categoryId);
          // Navigate to category detail page
          navigate(`/admin/categories/${categoryId}`);
          break;
          
        case 'edit':
          console.log('Editing category:', categoryId);
          // Navigate to category update page
          navigate(`/admin/categories/update/${categoryId}`);
          break;
          
        case 'delete':
          if (window.confirm('Bạn có chắc chắn muốn xóa danh mục này?')) {
            await categoryAPI.deleteCategory(categoryId);
            console.log('✅ Category deleted:', categoryId);
            
            // Refresh categories list
            const categoriesResponse = await categoryAPI.getCategories();
            setCategories(categoriesResponse?.data?.data?.categories || categoriesResponse?.data?.categories || categoriesResponse?.data || []);

            // Update book count map after deletion (delete normalized key)
            const updatedCountMap = new Map(bookCountByCategory);
            updatedCountMap.delete(String(categoryId));
            setBookCountByCategory(updatedCountMap);
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


  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="ml-4">Đang tải danh mục...</p>
      </div>
    );
  }

  // Debug: Log current state
  console.log('📂 CategoriesPage - Loading:', loading, 'Categories count:', categories.length);
  console.log('📂 CategoriesPage - Categories:', categories);
  console.log('📂 CategoriesPage - FilteredCategories:', filteredCategories);

  try {
    return (
    <div className="space-y-6">
      {/* Search */}
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

      {/* Categories Table */}
      <div className="relative">
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
              {Array.isArray(filteredCategories) && filteredCategories.length > 0 ? (
                filteredCategories.map((category) => (
                  <tr key={category?._id || category?.id || category?.name}>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="text-sm font-medium text-gray-900">{category?.name || 'N/A'}</div>
                    </td>
                    <td className="px-6 py-4 whitespace-normal text-sm text-gray-600">{category?.description || 'N/A'}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{getBookCount(category?._id)}</td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">{category?.createdAt ? formatDate(category.createdAt) : 'N/A'}</td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(category?.status)}`}>
                        {category?.status === 'active' ? 'Hoạt động' : 'Không hoạt động'}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleCategoryAction(category?._id, 'view')}
                          className="text-blue-600 hover:text-blue-900"
                        >
                          Xem
                        </button>
                        <button
                          onClick={() => navigate(`/admin/categories/update/${category?._id}`)}
                          className="text-green-600 hover:text-green-900"
                        >
                          Sửa
                        </button>
                        <button
                          onClick={() => handleCategoryAction(category?._id, 'delete')}
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
                    <div className="flex flex-col items-center">
                      <svg className="w-16 h-16 text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                      </svg>
                      <p className="text-lg font-medium text-gray-900 mb-2">Không có danh mục nào</p>
                      <p className="text-sm text-gray-500">Hãy thêm danh mục mới hoặc thử lại sau</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

    </div>
    );
  } catch (error) {
    console.error('❌ CategoriesPage render error:', error);
    return (
      <div className="space-y-6">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6">
          <div className="flex items-center">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Lỗi hiển thị trang danh mục
              </h3>
              <div className="mt-2 text-sm text-red-700">
                <p>Đã xảy ra lỗi khi hiển thị trang danh mục. Vui lòng thử lại.</p>
                <p className="mt-1 text-xs">Error: {error.message}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }
};

export default CategoriesPage;
