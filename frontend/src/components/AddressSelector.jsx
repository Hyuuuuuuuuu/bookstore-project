import React, { useState, useEffect } from 'react'
import { addressService } from '../services/addressService'
import { fetchProvinces, fetchDistricts, fetchWards } from '../utils/vietnamAddress'

const AddressSelector = ({ selectedAddressId, onAddressSelect, onAddNew }) => {
  const [addresses, setAddresses] = useState([])
  const [loading, setLoading] = useState(true)
  const [showAddForm, setShowAddForm] = useState(false)
  
  // Address dropdown data
  const [provinces, setProvinces] = useState([])
  const [districts, setDistricts] = useState([])
  const [wards, setWards] = useState([])
  const [loadingProvinces, setLoadingProvinces] = useState(false)
  const [loadingDistricts, setLoadingDistricts] = useState(false)
  const [loadingWards, setLoadingWards] = useState(false)
  
  const [formData, setFormData] = useState({
    name: '',
    phone: '',
    address: '',
    city: '',
    district: '',
    ward: '',
    provinceCode: '',
    districtCode: '',
    wardCode: '',
    isDefault: false
  })

  useEffect(() => {
    fetchAddresses()
    loadProvinces()
  }, [])

  // Load provinces on component mount
  const loadProvinces = async () => {
    try {
      setLoadingProvinces(true)
      const data = await fetchProvinces()
      setProvinces(data)
    } catch (error) {
      console.error('Error loading provinces:', error)
    } finally {
      setLoadingProvinces(false)
    }
  }

  // Load districts when province is selected
  const loadDistricts = async (provinceCode) => {
    if (!provinceCode) {
      setDistricts([])
      setWards([])
      return
    }
    try {
      setLoadingDistricts(true)
      const data = await fetchDistricts(provinceCode)
      setDistricts(data)
      setFormData(prev => ({
        ...prev,
        district: '',
        ward: '',
        districtCode: '',
        wardCode: ''
      }))
      setWards([])
    } catch (error) {
      console.error('Error loading districts:', error)
    } finally {
      setLoadingDistricts(false)
    }
  }

  // Load wards when district is selected
  const loadWards = async (districtCode) => {
    if (!districtCode) {
      setWards([])
      return
    }
    try {
      setLoadingWards(true)
      const data = await fetchWards(districtCode)
      setWards(data)
      setFormData(prev => ({
        ...prev,
        ward: '',
        wardCode: ''
      }))
    } catch (error) {
      console.error('Error loading wards:', error)
    } finally {
      setLoadingWards(false)
    }
  }

  const fetchAddresses = async () => {
    try {
      setLoading(true)
      const response = await addressService.getUserAddresses()
      const fetchedAddresses = response.data.addresses || []
      setAddresses(fetchedAddresses)
      
      // Nếu chưa có địa chỉ nào, tự động hiển thị form thêm địa chỉ mới
      if (fetchedAddresses.length === 0) {
        setShowAddForm(true)
      }
    } catch (error) {
      // Nếu lỗi 404 hoặc không có địa chỉ, tự động hiển thị form thêm địa chỉ mới
      if (error.response?.status === 404 || error.code === 'ERR_BAD_REQUEST') {
        setAddresses([])
        setShowAddForm(true)
      } else {
        alert('Không thể tải danh sách địa chỉ')
        console.error('Error fetching addresses:', error)
      }
    } finally {
      setLoading(false)
    }
  }

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target

    // Handle cascade dropdown changes
    if (name === 'provinceCode') {
      // Tìm province theo code (xử lý cả string và number)
      const selectedProvince = provinces.find(p => 
        String(p.code) === String(value) || p.code === value
      )
      console.log('🔍 Selecting province:', { code: value, province: selectedProvince })
      
      setFormData(prev => {
        const newData = {
          ...prev,
          provinceCode: value,
          city: selectedProvince?.name || '', // Set city name ngay lập tức
          district: '', // Reset district khi đổi province
          districtCode: '',
          ward: '', // Reset ward khi đổi province
          wardCode: ''
        }
        console.log('✅ Updated formData.city to:', newData.city)
        return newData
      })
      loadDistricts(value)
    } else if (name === 'districtCode') {
      // Tìm district theo code (xử lý cả string và number)
      const selectedDistrict = districts.find(d => 
        String(d.code) === String(value) || d.code === value
      )
      console.log('🔍 Selecting district:', { code: value, district: selectedDistrict })
      
      setFormData(prev => {
        const newData = {
          ...prev,
          districtCode: value,
          district: selectedDistrict?.name || '', // Set district name ngay lập tức
          ward: '', // Reset ward khi đổi district
          wardCode: ''
        }
        console.log('✅ Updated formData.district to:', newData.district)
        return newData
      })
      loadWards(value)
    } else if (name === 'wardCode') {
      // Tìm ward theo code (xử lý cả string và number)
      const selectedWard = wards.find(w => 
        String(w.code) === String(value) || w.code === value
      )
      console.log('🔍 Selecting ward:', { code: value, ward: selectedWard })
      
      setFormData(prev => {
        const newData = {
          ...prev,
          wardCode: value,
          ward: selectedWard?.name || '' // Set ward name ngay lập tức
        }
        console.log('✅ Updated formData.ward to:', newData.ward)
        return newData
      })
    } else {
      // Các input field khác
      setFormData(prev => ({
        ...prev,
        [name]: type === 'checkbox' ? checked : value
      }))
    }
  }

  const handleAddAddress = async (e) => {
    e.preventDefault()
    
    // Debug: Log formData để kiểm tra
    console.log('📋 FormData before validation:', formData)
    
    // Validation phía frontend - đảm bảo city, district, ward đã được chọn
    if (!formData.city || !formData.district || !formData.ward) {
      alert(`Vui lòng chọn đầy đủ Tỉnh/Thành phố, Quận/Huyện và Phường/Xã\n\nHiện tại:\n- City: ${formData.city || '(chưa chọn)'}\n- District: ${formData.district || '(chưa chọn)'}\n- Ward: ${formData.ward || '(chưa chọn)'}`)
      return
    }

    // Chuẩn bị data để gửi (chỉ gửi các field cần thiết, không gửi provinceCode, districtCode, wardCode)
    const addressData = {
      name: formData.name,
      phone: formData.phone,
      address: formData.address,
      city: formData.city.trim(), // Trim để loại bỏ khoảng trắng thừa
      district: formData.district.trim(),
      ward: formData.ward.trim(),
      isDefault: formData.isDefault
    }
    
    // Debug: Log data sẽ gửi lên
    console.log('📤 Address data to send:', addressData)
    
    try {
      const response = await addressService.createAddress(addressData)
      alert('Thêm địa chỉ thành công')
      
      setShowAddForm(false)
      setFormData({
        name: '',
        phone: '',
        address: '',
        city: '',
        district: '',
        ward: '',
        provinceCode: '',
        districtCode: '',
        wardCode: '',
        isDefault: false
      })
      setDistricts([])
      setWards([])
      
      // Refresh addresses list
      await fetchAddresses()
      
      // Auto-select the new address
      onAddressSelect(response.data.address._id)
      
    } catch (error) {
      console.error('❌ Error adding address:', error)
      console.error('❌ Error response:', error.response?.data)
      
      // Hiển thị thông báo lỗi chi tiết hơn
      const errorMessage = error.response?.data?.message || 
                          error.response?.data?.error || 
                          error.message || 
                          'Có lỗi xảy ra khi thêm địa chỉ'
      alert(`Lỗi: ${errorMessage}`)
    }
  }

  const handleCancelAdd = () => {
    setShowAddForm(false)
    setFormData({
      name: '',
      phone: '',
      address: '',
      city: '',
      district: '',
      ward: '',
      provinceCode: '',
      districtCode: '',
      wardCode: '',
      isDefault: false
    })
    setDistricts([])
    setWards([])
  }

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="animate-pulse">
          <div className="h-6 bg-gray-200 rounded w-1/4 mb-4"></div>
          <div className="space-y-3">
            <div className="h-4 bg-gray-200 rounded"></div>
            <div className="h-4 bg-gray-200 rounded w-3/4"></div>
            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold text-gray-900">Chọn địa chỉ giao hàng</h3>
        <button
          onClick={() => setShowAddForm(true)}
          className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors text-sm"
        >
          <span>+</span>
          Thêm địa chỉ
        </button>
      </div>

      {/* Add Address Form */}
      {showAddForm && (
        <div className="border border-gray-200 rounded-lg p-4 mb-6 bg-gray-50">
          <h4 className="text-md font-medium text-gray-900 mb-4">Thêm địa chỉ mới</h4>
          
          <form onSubmit={handleAddAddress} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Họ và tên *
                </label>
                <input
                  type="text"
                  name="name"
                  value={formData.name}
                  onChange={handleInputChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                  placeholder="Nhập họ và tên"
                />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Số điện thoại *
                </label>
                <input
                  type="tel"
                  name="phone"
                  value={formData.phone}
                  onChange={handleInputChange}
                  required
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                  placeholder="Nhập số điện thoại"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Địa chỉ chi tiết *
              </label>
              <input
                type="text"
                name="address"
                value={formData.address}
                onChange={handleInputChange}
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm"
                placeholder="Nhập số nhà và tên đường"
              />
              <p className="text-xs text-gray-500 mt-1">
                Ví dụ: 123 Nguyễn Văn A (không cần ghi lại tỉnh/thành phố, quận/huyện, phường/xã đã chọn ở trên)
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Tỉnh/Thành phố *
                </label>
                <select
                  name="provinceCode"
                  value={formData.provinceCode}
                  onChange={handleInputChange}
                  required
                  disabled={loadingProvinces}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
                >
                  <option value="">-- Chọn Tỉnh/Thành phố --</option>
                  {provinces.map((province) => (
                    <option key={province.code} value={province.code}>
                      {province.name}
                    </option>
                  ))}
                </select>
                {loadingProvinces && (
                  <p className="text-xs text-gray-500 mt-1">Đang tải...</p>
                )}
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Quận/Huyện *
                </label>
                <select
                  name="districtCode"
                  value={formData.districtCode}
                  onChange={handleInputChange}
                  required
                  disabled={!formData.provinceCode || loadingDistricts}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
                >
                  <option value="">-- Chọn Quận/Huyện --</option>
                  {districts.map((district) => (
                    <option key={district.code} value={district.code}>
                      {district.name}
                    </option>
                  ))}
                </select>
                {loadingDistricts && (
                  <p className="text-xs text-gray-500 mt-1">Đang tải...</p>
                )}
                {!formData.provinceCode && (
                  <p className="text-xs text-gray-500 mt-1">Vui lòng chọn Tỉnh/Thành phố trước</p>
                )}
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Phường/Xã *
                </label>
                <select
                  name="wardCode"
                  value={formData.wardCode}
                  onChange={handleInputChange}
                  required
                  disabled={!formData.districtCode || loadingWards}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm disabled:bg-gray-100 disabled:cursor-not-allowed"
                >
                  <option value="">-- Chọn Phường/Xã --</option>
                  {wards.map((ward) => (
                    <option key={ward.code} value={ward.code}>
                      {ward.name}
                    </option>
                  ))}
                </select>
                {loadingWards && (
                  <p className="text-xs text-gray-500 mt-1">Đang tải...</p>
                )}
                {!formData.districtCode && (
                  <p className="text-xs text-gray-500 mt-1">Vui lòng chọn Quận/Huyện trước</p>
                )}
              </div>
            </div>

            <div className="flex items-center">
              <input
                type="checkbox"
                name="isDefault"
                checked={formData.isDefault}
                onChange={handleInputChange}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label className="ml-2 text-sm text-gray-700">
                Đặt làm địa chỉ mặc định
              </label>
            </div>

            <div className="flex gap-3">
              <button
                type="submit"
                className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors text-sm"
              >
                Thêm địa chỉ
              </button>
              <button
                type="button"
                onClick={handleCancelAdd}
                className="bg-gray-300 hover:bg-gray-400 text-gray-700 px-4 py-2 rounded-lg transition-colors text-sm"
              >
                Hủy
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Address List */}
      <div className="space-y-3">
        {addresses.length === 0 ? (
          <div className="text-center py-8">
            <div className="text-4xl mb-3">📍</div>
            <p className="text-gray-600 mb-4">Chưa có địa chỉ nào</p>
            <button
              onClick={() => setShowAddForm(true)}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors text-sm"
            >
              Thêm địa chỉ đầu tiên
            </button>
          </div>
        ) : (
          addresses.map((address) => (
            <div
              key={address._id}
              className={`border-2 rounded-lg p-4 cursor-pointer transition-all ${
                selectedAddressId === address._id
                  ? 'border-blue-500 bg-blue-50'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
              onClick={() => onAddressSelect(address._id)}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-gray-500">👤</span>
                    <span className="font-medium text-gray-900 text-sm">{address.name}</span>
                    {address.isDefault && (
                      <span className="bg-blue-100 text-blue-800 text-xs px-2 py-0.5 rounded-full">
                        Mặc định
                      </span>
                    )}
                  </div>
                  
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-gray-500">📞</span>
                    <span className="text-gray-700 text-sm">{address.phone}</span>
                  </div>
                  
                  <div className="flex items-start gap-2">
                    <span className="text-gray-500 mt-0.5">📍</span>
                    <div className="text-gray-700 text-sm">
                      <p>{address.address}</p>
                      <p>{address.ward}, {address.district}, {address.city}</p>
                    </div>
                  </div>
                </div>
                
                {selectedAddressId === address._id && (
                  <span className="text-blue-600 ml-2">✓</span>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export default AddressSelector
