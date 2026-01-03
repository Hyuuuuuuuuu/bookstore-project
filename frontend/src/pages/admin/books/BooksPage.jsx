import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { bookAPI, categoryAPI } from '../../../services/apiService';

const BooksPage = () => {
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [categories, setCategories] = useState([]);
  
  // Loading state
  const [loading, setLoading] = useState(false);
  
  // Search & Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('');
  const [filterCategoryId, setFilterCategoryId] = useState('');
  const [filterStock, setFilterStock] = useState(''); // 'inStock', 'outOfStock', 'lowStock'
  
  const [currentPage, setCurrentPage] = useState(1);
  const [booksPerPage] = useState(15);
  const [pagination, setPagination] = useState({ totalItems: 0, totalPages: 1 });
  const [viewMode, setViewMode] = useState('table');

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchTerm]);

  // Fetch Data
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const params = {
          page: currentPage,
          limit: booksPerPage,
          search: debouncedSearchTerm,
          categoryId: filterCategoryId || undefined,
          stock: filterStock || undefined,
          sortBy: 'createdAt',
          sortOrder: 'desc',
          includeInactive: true // QUAN TRỌNG: Để Admin thấy cả sách hết hàng/ẩn
        };

        const [booksResponse, categoriesResponse] = await Promise.all([
          bookAPI.getBooks(params),
          categoryAPI.getCategories()
        ]);

        const booksData = booksResponse?.data?.data?.books || booksResponse?.data?.books || [];
        const pagingData = booksResponse?.data?.data?.pagination || { totalItems: 0, totalPages: 1 };
        
        setBooks(booksData);
        setPagination(pagingData);

        const catsData = categoriesResponse?.data?.data?.categories || categoriesResponse?.data?.categories || [];
        setCategories(catsData);

      } catch (error) {
        console.error('Error fetching data:', error);
        setBooks([]);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [currentPage, debouncedSearchTerm, filterCategoryId, filterStock]); 

  const handleBookAction = async (bookId, action) => {
    try {
      switch (action) {
        case 'view':
          navigate(`/admin/books/${bookId}`);
          break;
        case 'delete':
          if (window.confirm('Bạn có chắc chắn muốn xóa sách này?')) {
            await bookAPI.deleteBook(bookId);
            alert('Xóa sách thành công!');
            setBooks(prev => prev.filter(b => (b._id || b.id) !== bookId));
          }
          break;
        default:
          break;
      }
    } catch (error) {
      console.error(`Error ${action} book:`, error);
      alert(`Lỗi khi thực hiện thao tác. Vui lòng thử lại.`);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const getStatusInfo = (book) => {
    const stock = book.stock || 0;
    // Status hiển thị trên UI
    if (!book.isActive) return { text: 'Ngừng kinh doanh', color: 'bg-red-100 text-red-800' };
    if (stock === 0) return { text: 'Hết hàng', color: 'bg-gray-100 text-gray-800' };
    if (stock < 10) return { text: 'Sắp hết', color: 'bg-yellow-100 text-yellow-800' };
    return { text: 'Có sẵn', color: 'bg-green-100 text-green-800' };
  };

  const clearFilters = () => {
    setSearchTerm('');
    setFilterCategoryId('');
    setFilterStock('');
    setCurrentPage(1);
  };

  return (
    <div className="space-y-6">
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {/* Search Input */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tìm kiếm sách..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          {/* Category Filter */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Danh mục</label>
            <select
              value={filterCategoryId}
              onChange={(e) => {
                setFilterCategoryId(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Tất cả danh mục</option>
              {categories.map((category) => (
                <option key={category._id || category.id} value={category._id || category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter - Updated */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Trạng thái kho</label>
            <select
              value={filterStock}
              onChange={(e) => {
                setFilterStock(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="">Tất cả</option>
              <option value="inStock">Còn hàng</option>
              <option value="lowStock">Sắp hết hàng (&lt; 10)</option>
              <option value="outOfStock">Hết hàng</option>
            </select>
          </div>

          {/* Action Buttons */}
          <div className="flex items-end justify-end space-x-3">
            <button
              onClick={clearFilters}
              className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              Xóa bộ lọc
            </button>
            <button
              onClick={() => navigate('/admin/books/create')}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors whitespace-nowrap"
            >
              Thêm sách mới
            </button>
            
            <div className="flex border border-gray-300 rounded-lg overflow-hidden">
              <button
                onClick={() => setViewMode('table')}
                className={`p-2 ${viewMode === 'table' ? 'bg-gray-100' : 'bg-white hover:bg-gray-50'}`}
                title="Xem dạng bảng"
              >
                <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 7h18M3 12h18M3 17h18M3 4v16" />
                </svg>
              </button>
              <button
                onClick={() => setViewMode('card')}
                className={`p-2 ${viewMode === 'card' ? 'bg-gray-100' : 'bg-white hover:bg-gray-50'}`}
                title="Xem dạng lưới"
              >
                <svg className="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="relative min-h-[400px]">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : null}

        {viewMode === 'table' ? (
          <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tiêu đề</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tác giả</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Danh mục</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Giá</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Tồn kho</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {books.length > 0 ? (
                  books.map((book) => {
                    const statusInfo = getStatusInfo(book);
                    return (
                      <tr key={book._id || book.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="flex items-center">
                            <div className="w-10 h-14 bg-gray-200 rounded flex-shrink-0 overflow-hidden">
                              {book.imageUrl ? (
                                <img
                                  src={book.imageUrl.startsWith('http') ? book.imageUrl : `http://localhost:5000${book.imageUrl}`}
                                  alt=""
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <div className="w-full h-full flex items-center justify-center text-xs text-gray-400">No Img</div>
                              )}
                            </div>
                            <div className="ml-4">
                              <div className="text-sm font-medium text-gray-900 truncate max-w-[200px]" title={book.title}>{book.title}</div>
                              <div className="text-xs text-gray-500">{book.isbn}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{book.author}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                          {book.category?.name || book.categoryId?.name || '---'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                          {formatCurrency(book.price)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">{book.stock}</td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${statusInfo.color}`}>
                            {statusInfo.text}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                          <div className="flex space-x-3">
                            <button onClick={() => handleBookAction(book._id || book.id, 'view')} className="text-blue-600 hover:text-blue-900">Xem</button>
                            <button onClick={() => navigate(`/admin/books/update/${book._id || book.id}`)} className="text-green-600 hover:text-green-900">Sửa</button>
                            <button onClick={() => handleBookAction(book._id || book.id, 'delete')} className="text-red-600 hover:text-red-900">Xóa</button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan="7" className="px-6 py-8 text-center text-gray-500">
                      Không tìm thấy sách nào phù hợp.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          // Grid View
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {books.length > 0 ? (
              books.map((book) => {
                const statusInfo = getStatusInfo(book);
                return (
                  <div key={book._id || book.id} className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 flex flex-col">
                    <div className="aspect-[2/3] w-full bg-gray-100 rounded-md overflow-hidden mb-4 relative group">
                      {book.imageUrl ? (
                        <img
                          src={book.imageUrl.startsWith('http') ? book.imageUrl : `http://localhost:5000${book.imageUrl}`}
                          alt={book.title}
                          className="w-full h-full object-cover transition-transform group-hover:scale-105"
                        />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center text-gray-400">No Image</div>
                      )}
                    </div>
                    <h3 className="text-sm font-medium text-gray-900 line-clamp-2 mb-1" title={book.title}>{book.title}</h3>
                    <p className="text-xs text-gray-500 mb-2">{book.author}</p>
                    
                    <div className="mt-auto pt-2 border-t border-gray-100">
                      <div className="flex justify-between items-center mb-2">
                        <span className="text-sm font-bold text-gray-900">{formatCurrency(book.price)}</span>
                        <span className={`text-xs px-2 py-0.5 rounded-full ${statusInfo.color}`}>{statusInfo.text}</span>
                      </div>
                      <div className="flex justify-between text-xs font-medium">
                        <button onClick={() => navigate(`/admin/books/update/${book._id || book.id}`)} className="text-green-600 hover:text-green-800">Sửa</button>
                        <button onClick={() => handleBookAction(book._id || book.id, 'delete')} className="text-red-600 hover:text-red-800">Xóa</button>
                      </div>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="col-span-full text-center py-8 text-gray-500">
                Không tìm thấy sách nào.
              </div>
            )}
          </div>
        )}

        {/* Pagination */}
        {pagination.totalPages > 1 && (
          <div className="flex justify-center mt-6">
            <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
              <button
                onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
              >
                Trước
              </button>
              {[...Array(pagination.totalPages)].map((_, i) => (
                <button
                  key={i}
                  onClick={() => setCurrentPage(i + 1)}
                  className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                    currentPage === i + 1
                      ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                      : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                  }`}
                >
                  {i + 1}
                </button>
              ))}
              <button
                onClick={() => setCurrentPage(p => Math.min(pagination.totalPages, p + 1))}
                disabled={currentPage === pagination.totalPages}
                className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50"
              >
                Sau
              </button>
            </nav>
          </div>
        )}
      </div>
    </div>
  );
};

export default BooksPage;