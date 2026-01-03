import React, { useState, useEffect } from 'react'
import { 
  PlusIcon, 
  PencilIcon, 
  TrashIcon, 
  EyeIcon,
  EyeSlashIcon
} from '../../../components/icons/Icons'
import ShippingProviderModal from '../../../components/admin/ShippingProviderModal'
import axiosClient from '../../../services/axiosClient'

const ShippingProvidersPage = () => {
  const [providers, setProviders] = useState([])
  
  // Loading state
  const [loading, setLoading] = useState(false)
  
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProvider, setEditingProvider] = useState(null)
  
  // Search & Filter state
  const [searchTerm, setSearchTerm] = useState('')
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState('')
  const [filterActive, setFilterActive] = useState('all') // all, active, inactive
  const [sortBy, setSortBy] = useState('createdAt')
  const [sortOrder, setSortOrder] = useState('desc')

  // 1. Debounce Search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm)
    }, 500)
    return () => clearTimeout(timer)
  }, [searchTerm])

  // 2. Fetch Providers
  const fetchProviders = async () => {
    try {
      setLoading(true)
      
      const params = new URLSearchParams()
      if (debouncedSearchTerm) params.append('search', debouncedSearchTerm)
      if (filterActive !== 'all') params.append('status', filterActive) // Dùng 'status' cho đúng với Controller
      params.append('sortBy', sortBy)
      params.append('sortOrder', sortOrder)

      const response = await axiosClient.get(`/shipping-providers?${params}`)
      setProviders(response.data.data.providers || [])
    } catch (error) {
      console.error('Error fetching providers:', error)
      alert('Không thể tải danh sách đơn vị vận chuyển')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProviders()
  }, [debouncedSearchTerm, filterActive, sortBy, sortOrder])

  // Create provider
  const handleCreateProvider = async (providerData) => {
    try {
      await axiosClient.post('/shipping-providers', providerData)
      alert('Tạo đơn vị vận chuyển thành công!')
      setIsModalOpen(false)
      fetchProviders()
    } catch (error) {
      console.error('Error creating provider:', error)
      alert(error.response?.data?.message || 'Không thể tạo đơn vị vận chuyển')
    }
  }

  // Update provider
  const handleUpdateProvider = async (providerId, providerData) => {
    try {
      await axiosClient.put(`/shipping-providers/${providerId}`, providerData)
      alert('Cập nhật đơn vị vận chuyển thành công!')
      setIsModalOpen(false)
      setEditingProvider(null)
      fetchProviders()
    } catch (error) {
      console.error('Error updating provider:', error)
      alert(error.response?.data?.message || 'Không thể cập nhật đơn vị vận chuyển')
    }
  }

  // Delete provider
  const handleDeleteProvider = async (providerId) => {
    if (!window.confirm('Bạn có chắc chắn muốn xóa đơn vị vận chuyển này?')) {
      return
    }

    try {
      await axiosClient.delete(`/shipping-providers/${providerId}`)
      alert('Xóa đơn vị vận chuyển thành công!')
      fetchProviders()
    } catch (error) {
      console.error('Error deleting provider:', error)
      alert(error.response?.data?.message || 'Không thể xóa đơn vị vận chuyển')
    }
  }

  // Toggle active status
  const handleToggleActive = async (providerId, currentStatus) => {
    try {
      // Logic chuyển đổi trạng thái
      const newStatus = (currentStatus === 'ACTIVE') ? 'DISABLED' : 'ACTIVE';
      await axiosClient.put(`/shipping-providers/${providerId}`, { status: newStatus })
      
      // Update UI optimistic
      setProviders(prev => prev.map(p => 
        p._id === providerId ? { ...p, status: newStatus } : p
      ))
      
      alert(`Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'vô hiệu hóa'} đơn vị vận chuyển!`)
    } catch (error) {
      console.error('Error toggling provider status:', error)
      alert(error.response?.data?.message || 'Không thể cập nhật trạng thái')
      fetchProviders() // Fallback load lại nếu lỗi
    }
  }

  // Modal handlers
  const handleEditProvider = (provider) => {
    setEditingProvider(provider)
    setIsModalOpen(true)
  }

  const handleCreateNew = () => {
    setEditingProvider(null)
    setIsModalOpen(true)
  }

  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingProvider(null)
  }

  // Helper to clear filters
  const clearFilters = () => {
    setSearchTerm('')
    setFilterActive('all')
    setSortBy('createdAt')
    setSortOrder('desc')
  }

  return (
    <div className="space-y-6">
      {/* Search & Filters Section - KHÔNG bọc bởi loading */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          {/* Search */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tìm kiếm</label>
            <input
              type="text"
              placeholder="Tên hoặc mã đơn vị..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Filter Status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Trạng thái</label>
            <select
              value={filterActive}
              onChange={(e) => setFilterActive(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">Tất cả</option>
              <option value="active">Đang hoạt động</option>
              <option value="inactive">Không hoạt động</option>
            </select>
          </div>

          {/* Sort By */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Sắp xếp theo</label>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="createdAt">Ngày tạo</option>
              <option value="name">Tên</option>
              <option value="baseFee">Phí cơ bản</option>
              <option value="estimatedTime">Thời gian giao</option>
            </select>
          </div>

          {/* Sort Order */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Thứ tự</label>
            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="desc">Giảm dần</option>
              <option value="asc">Tăng dần</option>
            </select>
          </div>

          {/* Action Buttons */}
          <div className="flex items-end space-x-2">
            <button
              onClick={clearFilters}
              className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              Xóa lọc
            </button>
            <button
              onClick={handleCreateNew}
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center gap-1"
            >
              <PlusIcon className="w-5 h-5" />
              <span>Thêm mới</span>
            </button>
          </div>
        </div>
      </div>

      {/* Table Section - Loading hiển thị ở đây */}
      <div className="relative min-h-[400px]">
        {loading ? (
          <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-75 z-10 rounded-lg">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          </div>
        ) : null}

        <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Đơn vị vận chuyển</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Phí cơ bản</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thời gian giao</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Ngày tạo</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Thao tác</th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {providers.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-8 text-center text-gray-500">
                      Không có đơn vị vận chuyển nào phù hợp
                    </td>
                  </tr>
                ) : (
                  providers.map((provider) => {
                    // Xác định status từ field status hoặc active (fallback)
                    const isActive = provider.status === 'ACTIVE' || (!provider.status && provider.active);
                    const statusText = isActive ? 'ACTIVE' : 'DISABLED';

                    return (
                      <tr key={provider._id} className="hover:bg-gray-50">
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div>
                            <div className="text-sm font-medium text-gray-900">{provider.name}</div>
                            <div className="text-sm text-gray-500">Mã: {provider.code}</div>
                            {provider.description && (
                              <div className="text-xs text-gray-400 mt-1 max-w-xs truncate" title={provider.description}>
                                {provider.description}
                              </div>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900">{provider.baseFee?.toLocaleString('vi-VN')} ₫</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <div className="text-sm text-gray-900">{provider.estimatedTime}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                            isActive ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                          }`}>
                            {isActive ? 'Hoạt động' : 'Không hoạt động'}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {new Date(provider.createdAt).toLocaleDateString('vi-VN')}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-left text-sm font-medium">
                          <div className="flex items-center justify-start space-x-3">
                            <button
                              onClick={() => handleToggleActive(provider._id, statusText)}
                              className={`p-1.5 rounded-md transition-colors ${
                                isActive ? 'text-red-600 hover:bg-red-50' : 'text-green-600 hover:bg-green-50'
                              }`}
                              title={isActive ? 'Vô hiệu hóa' : 'Kích hoạt'}
                            >
                              {isActive ? <EyeSlashIcon className="w-5 h-5" /> : <EyeIcon className="w-5 h-5" />}
                            </button>
                            <button
                              onClick={() => handleEditProvider(provider)}
                              className="text-blue-600 hover:text-blue-900 p-1.5 hover:bg-blue-50 rounded-md"
                              title="Sửa"
                            >
                              <PencilIcon className="w-5 h-5" />
                            </button>
                            <button
                              onClick={() => handleDeleteProvider(provider._id)}
                              className="text-red-600 hover:text-red-900 p-1.5 hover:bg-red-50 rounded-md"
                              title="Xóa"
                            >
                              <TrashIcon className="w-5 h-5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Modal */}
      {isModalOpen && (
        <ShippingProviderModal
          provider={editingProvider}
          onClose={handleCloseModal}
          onSave={editingProvider ? handleUpdateProvider : handleCreateProvider}
        />
      )}
    </div>
  )
}

export default ShippingProvidersPage