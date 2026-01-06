import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { userAPI, orderAPI } from '../../../services/apiService';

const UserDetailPage = () => {
  const { userId } = useParams();
  const [user, setUser] = useState(null);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        setLoading(true);
        const [userResp, ordersResp] = await Promise.all([
          userAPI.getUser(userId),
          orderAPI.getUserOrders(userId, { page: 1, limit: 200 })
        ]);
        const userPayload = userResp?.data?.data || userResp?.data || null;
        const ordersPayload = ordersResp?.data?.data?.orders || ordersResp?.data?.orders || ordersResp?.data || [];
        setUser(userPayload);
        setOrders(Array.isArray(ordersPayload) ? ordersPayload : []);
      } catch (e) {
        console.error('Error loading user detail', e);
        setError('Không thể tải thông tin người dùng');
      } finally {
        setLoading(false);
      }
    };
    if (userId) fetch();
  }, [userId]);

  const aggregatePurchasedBooks = () => {
    const map = new Map();
    orders.forEach(o => {
      const items = o.orderItems || o.items || o.order_items || [];
      if (!Array.isArray(items)) return;
      items.forEach(it => {
        const book = it.book || it.product || {};
        const id = String(book._id || book.id || it.bookId || it.productId || 'unknown');
        const title = book.title || book.name || it.title || 'Unknown';
        const qty = Number(it.quantity || it.qty || it.count || 0) || 0;
        const existing = map.get(id) || { bookId: id, title, qty: 0 };
        existing.qty += qty;
        map.set(id, existing);
      });
    });
    return Array.from(map.values()).sort((a, b) => b.qty - a.qty);
  };

  if (loading) return <div className="p-6">Đang tải...</div>;
  if (error) return <div className="p-6 text-red-600">{error}</div>;
  if (!user) return <div className="p-6">Không tìm thấy người dùng</div>;

  const purchasedBooks = aggregatePurchasedBooks();

  return (
    <div className="space-y-6 p-6">
      <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">{user.name || user.fullName || 'Người dùng'}</h1>
            <p className="text-sm text-gray-500">{user.email}</p>
            <p className="text-sm text-gray-500">{user.phone}</p>
          </div>
          <div>
            <Link to="/admin/users" className="px-3 py-2 bg-gray-100 rounded">Quay lại</Link>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-medium mb-4">Đơn hàng của người dùng</h3>
          {orders.length === 0 ? (
            <p className="text-gray-500">Chưa có đơn hàng</p>
          ) : (
            <div className="space-y-3">
              {orders.map(o => (
                <div key={o._id || o.id} className="p-3 border rounded-lg flex items-center justify-between">
                  <div>
                    <div className="text-sm font-medium">{o.orderCode || o._id}</div>
                    <div className="text-xs text-gray-500">Trạng thái: {o.status}</div>
                    <div className="text-xs text-gray-500">Ngày: {o.createdAt ? new Date(o.createdAt).toLocaleString() : ''}</div>
                  </div>
                  <div className="text-right">
                    <div className="font-medium">{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(o.totalPrice || o.total || 0)}</div>
                    <Link to={`/admin/orders/${o._id || o.id}`} className="text-blue-600 text-sm">Xem chi tiết</Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-medium mb-4">Sách đã mua</h3>
          {purchasedBooks.length === 0 ? (
            <p className="text-gray-500">Chưa mua sách nào</p>
          ) : (
            <div className="space-y-2">
              {purchasedBooks.map(b => (
                <div key={b.bookId} className="flex items-center justify-between p-2 border rounded">
                  <div className="truncate">{b.title}</div>
                  <div className="text-sm text-gray-600">Số lượng: {b.qty}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default UserDetailPage;


