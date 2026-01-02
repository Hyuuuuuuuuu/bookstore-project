import React, { useState, useEffect } from 'react'
import { 
  PlusIcon, 
  PencilIcon, 
  TrashIcon, 
  EyeIcon,
  EyeSlashIcon,
  ArrowPathIcon
} from '../../../components/icons/Icons'
import ShippingProviderModal from '../../../components/admin/ShippingProviderModal'
import axiosClient from '../../../services/axiosClient'

const ShippingProvidersPage = () => {
  const [providers, setProviders] = useState([])
  const [loading, setLoading] = useState(true)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [editingProvider, setEditingProvider] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterActive, setFilterActive] = useState('all') // all, active, inactive
  const [sortBy, setSortBy] = useState('createdAt')
  const [sortOrder, setSortOrder] = useState('desc')

  // Fetch providers
  const fetchProviders = async () => {
    try {
      setLoading(true)
      
      const params = new URLSearchParams()
      if (searchTerm) params.append('search', searchTerm)
      if (filterActive !== 'all') params.append('active', filterActive === 'active')
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
  }, [searchTerm, filterActive, sortBy, sortOrder])

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
      // currentStatus may be 'ACTIVE' or 'DISABLED' (string). Toggle to opposite.
      const newStatus = (currentStatus === 'ACTIVE') ? 'DISABLED' : 'ACTIVE';
      await axiosClient.put(`/shipping-providers/${providerId}`, { status: newStatus })
      alert(`Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'vô hiệu hóa'} đơn vị vận chuyển!`)
      fetchProviders()
    } catch (error) {
      console.error('Error toggling provider status:', error)
      alert(error.response?.data?.message || 'Không thể cập nhật trạng thái')
    }
  }

  // Open modal for editing
  const handleEditProvider = (provider) => {
    setEditingProvider(provider)
    setIsModalOpen(true)
  }

  // Open modal for creating
  const handleCreateNew = () => {
    setEditingProvider(null)
    setIsModalOpen(true)
  }

  // Close modal
  const handleCloseModal = () => {
    setIsModalOpen(false)
    setEditingProvider(null)
  }

  return (
    <div className="space-y-6">

      {/* Filters and Search */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 mb-6">
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          {/* Search */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Tìm kiếm
            </label>
            <input
              type="text"
              placeholder="Tên hoặc mã đơn vị..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Filter by status */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Trạng thái
            </label>
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

          {/* Sort by */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Sắp xếp theo
            </label>
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

          {/* Sort order */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Thứ tự
            </label>
            <select
              value={sortOrder}
              onChange={(e) => setSortOrder(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="desc">Giảm dần</option>
              <option value="asc">Tăng dần</option>
            </select>
          </div>
          <div className="flex items-end justify-end">
            <button
              onClick={handleCreateNew}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Thêm đơn vị vận chuyển
            </button>
          </div>
        </div>
      </div>

      {/* Providers Table */}
      <div className="bg-white rounded-lg shadow-sm border border-gray-200">
        {loading ? (
          <div className="p-8 text-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="text-gray-600 mt-2">Đang tải...</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Đơn vị vận chuyển
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Phí cơ bản
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thời gian giao
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Trạng thái
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Ngày tạo
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Thao tác
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {providers.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="px-6 py-8 text-center text-gray-500">
                      Không có đơn vị vận chuyển nào
                    </td>
                  </tr>
                ) : (
                  providers.map((provider) => (
                    <tr key={provider._id} className="hover:bg-gray-50">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div>
                          <div className="text-sm font-medium text-gray-900">
                            {provider.name}
                          </div>
                          <div className="text-sm text-gray-500">
                            Mã: {provider.code}
                          </div>
                          {provider.description && (
                            <div className="text-xs text-gray-400 mt-1">
                              {provider.description}
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">
                          {provider.baseFee?.toLocaleString('vi-VN')} ₫
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">
                          {provider.estimatedTime}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          (provider.status ? provider.status === 'ACTIVE' : provider.active)
                            ? 'bg-green-100 text-green-800'
                            : 'bg-red-100 text-red-800'
                        }`}>
                          {(provider.status ? provider.status === 'ACTIVE' : provider.active) ? 'Hoạt động' : 'Không hoạt động'}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {new Date(provider.createdAt).toLocaleDateString('vi-VN')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-left text-sm font-medium">
                        <div className="flex items-center justify-start space-x-4">
                          <button
                            onClick={() => handleToggleActive(provider._id, provider.status || (provider.active ? 'ACTIVE' : 'DISABLED'))}
                            className={`p-2 rounded-md ${
                              (provider.status ? provider.status === 'ACTIVE' : provider.active)
                                ? 'text-red-600 hover:bg-red-50'
                                : 'text-green-600 hover:bg-green-50'
                            }`}
                            title={(provider.status ? provider.status === 'ACTIVE' : provider.active) ? 'Vô hiệu hóa' : 'Kích hoạt'}
                          >
                            {(provider.status ? provider.status === 'ACTIVE' : provider.active) ? (
                              <EyeSlashIcon className="w-4 h-4" />
                            ) : (
                              <EyeIcon className="w-4 h-4" />
                            )}
                          </button>
                          <button
                            onClick={() => handleEditProvider(provider)}
                            className="text-blue-600 hover:text-blue-900"
                          >
                            Sửa
                          </button>
                          <button
                            onClick={() => handleDeleteProvider(provider._id)}
                            className="text-red-600 hover:underline text-sm font-medium whitespace-nowrap"
                          >
                            Xóa
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
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
