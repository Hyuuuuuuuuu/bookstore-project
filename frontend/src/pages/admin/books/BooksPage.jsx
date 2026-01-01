import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookAPI, categoryAPI } from '../../../services/apiService';

const BooksPage = () => {
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // --- 1. SETUP DEBOUNCE CHO TÌM KIẾM ---
  const [searchTermInput, setSearchTermInput] = useState(''); 
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState(''); 

  // State bộ lọc
  const [filterCategory, setFilterCategory] = useState(''); 
  const [filterStatus, setFilterStatus] = useState('');     
  const [currentPage, setCurrentPage] = useState(1);
  const [pagination, setPagination] = useState({});

  // Effect Debounce
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTermInput);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTermInput]);

  // --- 2. GỌI API KHI BỘ LỌC THAY ĐỔI ---
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        // Fetch categories (chỉ cần lấy 1 lần, nhưng để đây cho tiện)
        const categoriesResponse = await categoryAPI.getCategories();
        setCategories(categoriesResponse?.data?.data?.categories || []);

        // Fetch Books với filters
        const booksResponse = await bookAPI.getBooks({
          page: currentPage,
          limit: 10,
          search: debouncedSearchTerm, // Dùng giá trị đã debounce
          categoryId: filterCategory !== 'all' ? filterCategory : undefined,
          stock: filterStatus !== 'all' ? filterStatus : undefined // Mapping status vào param 'stock' của API cũ hoặc sửa API
        });

        const booksData = booksResponse?.data?.data?.books || [];
        const paginationData = booksResponse?.data?.data?.pagination || {};

        setBooks(booksData);
        setPagination(paginationData);
        
      } catch (error) {
        console.error('Error fetching data:', error);
        setBooks([]);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [currentPage, debouncedSearchTerm, filterCategory, filterStatus]); // Lắng nghe thay đổi

  // ... (Giữ nguyên các hàm helper formatCurrency, getStatusColor, handleBookAction)
  const formatCurrency = (amount) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
  
  const getStatusColor = (status, stock, isActive) => {
    if (!isActive) return 'bg-red-100 text-red-800'; // Ngừng bán
    if (stock === 0) return 'bg-yellow-100 text-yellow-800'; // Hết hàng
    return 'bg-green-100 text-green-800'; // Đang bán
  };

  const getStatusText = (status, stock, isActive) => {
    if (!isActive) return 'Ngừng bán';
    if (stock === 0) return 'Hết hàng';
    return 'Đang bán';
  };

  const handleBookAction = async (bookId, action) => {
      if(action === 'view') navigate(`/admin/books/${bookId}`);
      if(action === 'delete') {
          if(window.confirm("Xóa sách?")) {
              await bookAPI.deleteBook(bookId);
              // Trigger reload bằng cách set lại 1 state nào đó hoặc gọi lại fetch
              window.location.reload(); 
          }
      }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-end space-x-4">
        <button
          onClick={() => navigate('/admin/books/create')}
          className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
        >
          Thêm sách mới
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          
          {/* Ô tìm kiếm có Debounce */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm kiếm sách..."
              value={searchTermInput} // Bind vào state input
              onChange={(e) => setSearchTermInput(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* Filter Category: Gửi ID thay vì Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Danh mục</label>
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">Tất cả</option>
              {categories.map((category) => (
                // QUAN TRỌNG: value={category._id} (Gửi ID lên server)
                <option key={category._id} value={category._id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          {/* Filter Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái</label>
            <select
              value={filterStatus}
              onChange={(e) => setFilterStatus(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="all">Tất cả</option>
              <option value="active">Đang bán</option>
              <option value="inactive">Ngừng bán</option>
            </select>
          </div>

          <div className="flex items-end">
            <button
              onClick={() => {
                setSearchTermInput('');
                setFilterCategory('all');
                setFilterStatus('all');
              }}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              Xóa bộ lọc
            </button>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="relative">
        {loading ? (
             <div className="flex justify-center p-8">Đang tải...</div>
        ) : (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tiêu đề</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Danh mục</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Giá</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tồn kho</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {books.length > 0 ? books.map((book) => (
                  <tr key={book._id}>
                    <td className="px-6 py-4">
                        <div className="text-sm font-medium text-gray-900">{book.title}</div>
                        <div className="text-xs text-gray-500">{book.author}</div>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-500">
                        {book.category?.name || 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">{formatCurrency(book.price)}</td>
                    <td className="px-6 py-4 text-sm text-gray-900">{book.stock}</td>
                    <td className="px-6 py-4">
                        <span className={`px-2 py-1 text-xs rounded-full ${getStatusColor(null, book.stock, book.isActive)}`}>
                            {getStatusText(null, book.stock, book.isActive)}
                        </span>
                    </td>
                    <td className="px-6 py-4 text-sm font-medium">
                        <button onClick={() => handleBookAction(book._id, 'view')} className="text-blue-600 hover:text-blue-900 mr-3">Xem</button>
                        <button onClick={() => navigate(`/admin/books/update/${book._id}`)} className="text-green-600 hover:text-green-900 mr-3">Sửa</button>
                        <button onClick={() => handleBookAction(book._id, 'delete')} className="text-red-600 hover:text-red-900">Xóa</button>
                    </td>
                  </tr>
                )) : (
                    <tr><td colSpan="6" className="text-center py-4">Không tìm thấy sách nào</td></tr>
                )}
              </tbody>
            </table>
        </div>
        )}
      </div>
      
      {/* Pagination Controls Here (Optional) */}
    </div>
  );
};

export default BooksPage;