import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { orderAPI, bookAPI } from '../../services/apiService';
import PageLayout from '../../layouts/PageLayout';

const OrderDetailPage = () => {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    const fetchOrderDetail = async () => {
      try {
        setLoading(true);
        const response = await orderAPI.getOrder(orderId);
        console.log('📦 Order detail response:', response);
        const fetched = response?.data?.data || response?.data;
        // enrich order items with book details if needed
        const enriched = await enrichOrderItems(fetched);
        setOrder(enriched);
        setLoading(false);
      } catch (error) {
        console.error('❌ Error fetching order detail:', error);
        setError('Không thể tải chi tiết đơn hàng');
        setLoading(false);
      }
    };

    if (orderId) {
      fetchOrderDetail();
    }
  }, [orderId]);

  // Enrich order items: if item.bookId is an id (string/number), fetch book detail
  const enrichOrderItems = async (orderData) => {
    if (!orderData) return orderData;
    const items = orderData.orderItems || orderData.items || [];
    if (!items.length) return orderData;

    const enrichedItems = await Promise.all(items.map(async (item) => {
      try {
        // if bookId is an object with title, no need to fetch
        const bookRef = item.bookId || item.book || null;
        if (bookRef && typeof bookRef === 'object' && (bookRef.title || bookRef.name)) {
          return { ...item, bookDetails: bookRef };
        }
        const bookId = typeof bookRef === 'string' || typeof bookRef === 'number' ? bookRef : item.bookId || item.book;
        if (bookId) {
          const res = await bookAPI.getBook(bookId);
          const book = res?.data?.data || res?.data || null;
          return { ...item, bookDetails: book };
        }
      } catch (e) {
        console.warn('Failed to fetch book details for item', item, e);
      }
      return { ...item, bookDetails: null };
    }));

    return { ...orderData, orderItems: enrichedItems };
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  const getStatusColor = (status) => {
    // Normalize status string for robust matching (supports strings or objects)
    let st = status;
    if (typeof st === 'object') {
      st = (st.name || st.status || st.code || st.value || '').toString();
    } else {
      st = String(st || '');
    }
    st = st.trim().toLowerCase();
    switch (st) {
      case 'pending': return 'bg-yellow-100 text-yellow-800';
      case 'confirmed': return 'bg-blue-100 text-blue-800';
      case 'shipped': return 'bg-purple-100 text-purple-800';
      case 'delivered': return 'bg-green-100 text-green-800';
      case 'cancelled': return 'bg-red-100 text-red-800';
      case 'digital_delivered': return 'bg-green-100 text-green-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getStatusText = (status) => {
    let st = status;
    if (typeof st === 'object') {
      st = (st.name || st.status || st.code || st.value || '').toString();
    } else {
      st = String(st || '');
    }
    st = st.trim().toLowerCase();
    switch (st) {
      case 'pending': return 'Chờ xử lý';
      case 'confirmed': return 'Đã xác nhận';
      case 'shipped': return 'Đang giao';
      case 'delivered': return 'Đã nhận';
      case 'cancelled': return 'Đã hủy';
      case 'digital_delivered': return 'Đã giao (Sách điện tử)';
      default: return status;
    }
  };

  const getPaymentMethodText = (method) => {
    switch (method) {
      case 'cod': return 'Thanh toán khi nhận hàng';
      case 'credit_card': return 'Thẻ tín dụng';
      case 'bank_transfer': return 'Chuyển khoản ngân hàng';
      case 'paypal': return 'PayPal';
      default: return method;
    }
  };

  const handleCancelOrder = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn hàng này?')) {
      return;
    }

    try {
      setCancelling(true);
      await orderAPI.cancelOrder(orderId);
      
      // Refresh order data
      const response = await orderAPI.getOrder(orderId);
      setOrder(response?.data?.data || response?.data);
      
      alert('Đơn hàng đã được hủy thành công');
    } catch (error) {
      console.error('Error cancelling order:', error);
      alert(error.response?.data?.message || 'Có lỗi xảy ra khi hủy đơn hàng');
    } finally {
      setCancelling(false);
    }
  };

  const canCancelOrder = () => {
    if (!order || !order.status) return false;
    let st = order.status;
    if (typeof st === 'object') {
      st = (st.name || st.status || st.code || st.value || '').toString();
    } else {
      st = String(st);
    }
    st = st.trim().toLowerCase();
    return ['pending', 'confirmed'].includes(st);
  };

  const handleContactSupport = () => {
    // Build robust order info for support context (fallbacks for various backend shapes)
    const orderInfo = {
      orderId: order._id || order.id || null,
      orderCode: order.orderCode || order.code || order._id || order.id || null,
      status: order.status || null,
      totalPrice: order.totalPrice || order.totalAmount || order.originalAmount || 0,
      discountAmount: order.discountAmount || order.discount || 0,
      finalPrice: (order.totalPrice || order.totalAmount || 0) - (order.discountAmount || order.discount || 0),
      paymentMethod: order.paymentMethod || order.paymentMethodCode || null,
      createdAt: order.createdAt || order.created_at || null,
      items: (order.orderItems || order.items || []).map(item => {
        const book = item.bookId || item.book || {};
        return {
          title: book.title || item.title || 'Sách không xác định',
          author: book.author || item.author || 'Tác giả không xác định',
          quantity: item.quantity || item.qty || 1,
          price: item.priceAtPurchase || item.price || item.unitPrice || 0
        }
      }),
      shippingAddress: order.shippingAddress || order.address || null
    };

    // Persist to localStorage for ChatPage to pick up
    try {
      localStorage.setItem('supportOrderInfo', JSON.stringify(orderInfo));
    } catch (e) {
      console.warn('Failed to store supportOrderInfo', e);
    }

    // Navigate to chat page
    navigate('/chat');
  };

  if (loading) {
    return (
      <PageLayout>
        <div className="min-h-screen bg-white flex items-center justify-center">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-amber-600 mx-auto mb-4"></div>
            <p className="text-gray-600">Đang tải chi tiết đơn hàng...</p>
          </div>
        </div>
      </PageLayout>
    );
  }

  if (error || !order) {
    return (
      <PageLayout>
        <div className="min-h-screen bg-white flex items-center justify-center">
          <div className="text-center">
            <div className="text-red-500 mb-4">
              <svg className="w-16 h-16 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.5 0L4.268 19.5c-.77.833.192 2.5 1.732 2.5z" />
              </svg>
            </div>
            <h2 className="text-2xl font-semibold text-gray-900 mb-4">Không tìm thấy đơn hàng</h2>
            <p className="text-gray-600 mb-8">{error || 'Đơn hàng không tồn tại hoặc bạn không có quyền xem'}</p>
            <div className="space-x-4">
              <Link 
                to="/orders" 
                className="bg-amber-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-amber-700 transition-colors"
              >
                Xem đơn hàng của tôi
              </Link>
              <Link 
                to="/" 
                className="bg-gray-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-gray-700 transition-colors"
              >
                Về trang chủ
              </Link>
            </div>
          </div>
        </div>
      </PageLayout>
    );
  }

  return (
    <PageLayout>
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold text-gray-900 mb-2">Chi tiết đơn hàng</h1>
              <p className="text-lg text-gray-600">
                Mã đơn hàng: <span className="font-medium text-amber-600">{order.orderCode || order._id}</span>
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <span className={`inline-flex px-3 py-1 text-sm font-medium rounded-full ${getStatusColor(order.status)}`}>
                {getStatusText(order.status)}
              </span>
              {canCancelOrder() && (
                <button
                  onClick={handleCancelOrder}
                  disabled={cancelling}
                  className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  {cancelling ? 'Đang hủy...' : 'Hủy đơn hàng'}
                </button>
              )}
              <Link 
                to="/orders" 
                className="bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700 transition-colors"
              >
                Quay lại
              </Link>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Order Items */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-2xl shadow-sm border border-gray-100">
              <div className="p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-6">Sản phẩm đã đặt</h2>
                
                {order.orderItems && order.orderItems.length > 0 ? (
                  <div className="space-y-4">
                    {order.orderItems.map((item, index) => {
                      const book = item.bookDetails || item.bookId || item.book || {};
                      const imageUrl = book?.imageUrl || book?.image || null;
                      const title = book?.title || book?.name || item.title || 'Sách không xác định';
                      const author = book?.author || item.author || 'Tác giả không xác định';
                      const keyId = item._id || book?.id || book?._id || `idx_${index}`;
                      return (
                        <div key={keyId} className="flex items-center space-x-4 p-4 border border-gray-200 rounded-xl">
                          {/* Book Image */}
                          <div className="flex-shrink-0">
                            {imageUrl ? (
                              <img
                                src={imageUrl.startsWith('http') ? imageUrl : `http://localhost:5000${imageUrl}`}
                                alt={title}
                                className="w-16 h-20 object-cover rounded-xl"
                                onError={(e) => {
                                  e.target.style.display = 'none';
                                  if (e.target.nextSibling) {
                                    e.target.nextSibling.style.display = 'block';
                                  }
                                }}
                              />
                            ) : null}
                            <div className="w-16 h-20 bg-gray-200 rounded-xl flex items-center justify-center" style={{display: imageUrl ? 'none' : 'flex'}}>
                              <svg className="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.746 0 3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                              </svg>
                            </div>
                          </div>

                          {/* Book Info */}
                          <div className="flex-1 min-w-0">
                            <h3 className="text-lg font-medium text-gray-900 truncate">
                              {title}
                            </h3>
                            <p className="text-sm text-gray-600">{author}</p>
                            <p className="text-sm text-gray-500">
                              Số lượng: {item.quantity || 1} • Giá: {formatCurrency(item.priceAtPurchase || item.price || item.unitPrice || 0)}
                            </p>
                          </div>

                          {/* Total Price */}
                          <div className="text-right">
                            <p className="text-lg font-semibold text-black">
                              {formatCurrency((item.priceAtPurchase || item.price || item.unitPrice || 0) * (item.quantity || 1))}
                            </p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="text-center py-8">
                    <p className="text-gray-500">Không có sản phẩm nào trong đơn hàng này</p>
                  </div>
                )}
              </div>
            </div>
          </div>
          
          {/* Order Summary */}
          <div className="lg:col-span-1">
            <div className="space-y-6">
              {/* Order Info */}
              <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                <h3 className="text-xl font-semibold text-gray-900 mb-4">Thông tin đơn hàng</h3>
                <div className="space-y-3">
                  <div>
                    <span className="text-sm text-gray-600">Mã đơn hàng:</span>
                    <p className="font-medium">{order.orderCode || order._id}</p>
                  </div>
                  <div>
                    <span className="text-sm text-gray-600">Ngày đặt:</span>
                    <p className="font-medium">
                      {order.createdAt ? new Date(order.createdAt).toLocaleDateString('vi-VN', {
                        year: 'numeric',
                        month: 'long',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit'
                      }) : 'N/A'}
                    </p>
                  </div>
                  <div>
                    <span className="text-sm text-gray-600">Trạng thái:</span>
                    <p className="font-medium">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(order.status)}`}>
                        {getStatusText(order.status)}
                      </span>
                    </p>
                  </div>
                  <div>
                    <span className="text-sm text-gray-600">Phương thức thanh toán:</span>
                    <p className="font-medium">{getPaymentMethodText(order.paymentMethod)}</p>
                  </div>
                  {order.confirmedAt && (
                    <div>
                      <span className="text-sm text-gray-600">Ngày xác nhận:</span>
                      <p className="font-medium">
                        {new Date(order.confirmedAt).toLocaleDateString('vi-VN', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </p>
                    </div>
                  )}
                  {order.shippedAt && (
                    <div>
                      <span className="text-sm text-gray-600">Ngày giao hàng:</span>
                      <p className="font-medium">
                        {new Date(order.shippedAt).toLocaleDateString('vi-VN', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </p>
                    </div>
                  )}
                  {order.deliveredAt && (
                    <div>
                      <span className="text-sm text-gray-600">Ngày nhận hàng:</span>
                      <p className="font-medium">
                        {new Date(order.deliveredAt).toLocaleDateString('vi-VN', {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit'
                        })}
                      </p>
                    </div>
                  )}
                  {order.note && (
                    <div>
                      <span className="text-sm text-gray-600">Ghi chú:</span>
                      <p className="font-medium">{order.note}</p>
                    </div>
                  )}
                </div>
              </div>

              {/* Shipping Address */}
              {order.shippingAddress && (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                  <h3 className="text-xl font-semibold text-gray-900 mb-4">Địa chỉ giao hàng</h3>
                  <div className="space-y-2">
                    <p className="font-medium">{order.shippingAddress.name}</p>
                    <p className="text-gray-600">{order.shippingAddress.phone}</p>
                    <p className="text-gray-600">{order.shippingAddress.address}</p>
                    <p className="text-gray-600">
                      {order.shippingAddress.ward}, {order.shippingAddress.district}, {order.shippingAddress.city}
                    </p>
                    {order.shippingProvider && (
                      <div className="mt-4 pt-4 border-t border-gray-200">
                        <p className="text-sm text-gray-600">Đơn vị vận chuyển:</p>
                        <p className="font-medium">{order.shippingProvider.name}</p>
                        {order.shippingProvider.estimatedTime && (
                          <p className="text-sm text-gray-600">
                            Thời gian giao dự kiến: {order.shippingProvider.estimatedTime}
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Order Summary */}
              <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
                <h3 className="text-xl font-semibold text-gray-900 mb-4">Tóm tắt đơn hàng</h3>
                <div className="space-y-3">
                  <div className="flex justify-between">
                    <span className="text-gray-600">Tạm tính:</span>
                    <span className="font-medium">{formatCurrency(order.originalAmount || order.totalPrice)}</span>
                  </div>
                  {order.discountAmount > 0 && (
                    <div className="flex justify-between text-green-600">
                      <span>Giảm giá:</span>
                      <span className="font-medium">-{formatCurrency(order.discountAmount)}</span>
                    </div>
                  )}
                  {order.voucherId && (
                    <div className="flex justify-between text-blue-600">
                      <span>Voucher:</span>
                      <span className="font-medium">{order.voucherId.code || 'Đã áp dụng'}</span>
                    </div>
                  )}
                  <div className="flex justify-between">
                    <span className="text-gray-600">Phí vận chuyển:</span>
                    <span className="font-medium">
                      {order.shippingFee > 0 ? formatCurrency(order.shippingFee) : 'Miễn phí'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-600">Đơn vị vận chuyển:</span>
                    <span className="font-medium">
                      {order.shippingProvider?.name || 'Chưa xác định'}
                    </span>
                  </div>
                  <div className="border-t pt-3">
                    <div className="flex justify-between">
                      <span className="text-lg font-semibold">Tổng cộng:</span>
                      <span className="text-lg font-semibold text-amber-600">
                        {formatCurrency(order.totalPrice)}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </PageLayout>
  );
};

export default OrderDetailPage;
