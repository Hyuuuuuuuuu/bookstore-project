import React, { useState, useEffect } from 'react';
import axiosClient from '../../../services/axiosClient';

const ReportsPage = () => {
  const [reports, setReports] = useState({});
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState('30days');

  useEffect(() => {
    const fetchReports = async () => {
      setLoading(true);
      try {
        // Gọi API báo cáo chuyên biệt từ Backend
        const response = await axiosClient.get(`/reports/analytics?range=${dateRange}`);
        console.log('📊 Report API Response:', response.data);

        setReports(response.data.data);
        setLoading(false);
      } catch (error) {
        console.error('Error fetching reports:', error);
        setReports({
          sales: { total: 0, growth: 0 },
          orders: { total: 0, growth: 0, status: { completed: 0, pending: 0, cancelled: 0 } },
          topBooks: [],
          customers: { new: 0, total: 0 }
        });
        setLoading(false);
      }
    };

    fetchReports();
  }, [dateRange]);

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <p className="ml-4">Đang tải báo cáo...</p>
      </div>
    );
  }

  // Calculate completion rate
  const completionRate = reports.orders?.total > 0 
    ? Math.round((reports.orders.status.completed / reports.orders.total) * 100) 
    : 0;

  return (
    <div className="space-y-6">
      {/* Date Range Filter */}
      <div className="flex justify-end">
        <select
          value={dateRange}
          onChange={(e) => setDateRange(e.target.value)}
          className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent bg-white"
        >
          <option value="7days">7 ngày qua</option>
          <option value="30days">30 ngày qua</option>
          <option value="90days">90 ngày qua</option>
          <option value="1year">1 năm qua</option>
        </select>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {/* Doanh thu */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center mb-2">
            <div className="p-3 rounded-full bg-green-100 mr-4">
              <span className="text-green-600 text-xl font-bold">$</span>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-600">Tổng doanh thu</p>
              <p className="text-2xl font-bold text-gray-900">{formatCurrency(reports.sales?.total || 0)}</p>
            </div>
          </div>
          <div className={`text-sm font-medium ${reports.sales?.growth >= 0 ? 'text-green-600' : 'text-red-600'}`}>
            {reports.sales?.growth >= 0 ? '+' : ''}{reports.sales?.growth}% <span className="text-gray-500 font-normal">so với kỳ trước</span>
          </div>
        </div>

        {/* Đơn hàng */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center mb-2">
            <div className="p-3 rounded-full bg-blue-100 mr-4">
              <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z" /></svg>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-600">Tổng đơn hàng</p>
              <p className="text-2xl font-bold text-gray-900">{reports.orders?.total || 0}</p>
            </div>
          </div>
          <div className={`text-sm font-medium ${reports.orders?.growth >= 0 ? 'text-blue-600' : 'text-red-600'}`}>
            {reports.orders?.growth >= 0 ? '+' : ''}{reports.orders?.growth}% <span className="text-gray-500 font-normal">so với kỳ trước</span>
          </div>
        </div>

        {/* Khách hàng mới */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center mb-2">
            <div className="p-3 rounded-full bg-purple-100 mr-4">
              <svg className="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" /></svg>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-600">Khách hàng mới</p>
              <p className="text-2xl font-bold text-gray-900">{reports.customers?.new || 0}</p>
            </div>
          </div>
          <div className="text-sm text-purple-600 font-medium">
            Trong kỳ này
          </div>
        </div>

        {/* Tỷ lệ hoàn thành */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <div className="flex items-center mb-2">
            <div className="p-3 rounded-full bg-yellow-100 mr-4">
              <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>
            </div>
            <div>
              <p className="text-sm font-medium text-gray-600">Tỷ lệ hoàn thành</p>
              <p className="text-2xl font-bold text-gray-900">{completionRate}%</p>
            </div>
          </div>
          <div className="text-sm text-yellow-600 font-medium">
            Đơn hàng thành công
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Books */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Sách nổi bật (Top View)</h3>
          <div className="space-y-3">
            {reports.topBooks?.length > 0 ? (
              reports.topBooks.map((book, index) => (
                <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center">
                    <span className="w-6 h-6 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-xs font-bold mr-3">
                      {index + 1}
                    </span>
                    <div>
                      <p className="font-medium text-gray-900">{book.name}</p>
                      <p className="text-sm text-gray-600">{book.sales} lượt xem</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-medium text-gray-900">{formatCurrency(book.revenue)}</p>
                  </div>
                </div>
              ))
            ) : (
              <p className="text-center text-gray-500 py-4">Chưa có dữ liệu sách</p>
            )}
          </div>
        </div>

        {/* Order Status */}
        <div className="bg-white p-6 rounded-lg shadow-sm border border-gray-200">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Trạng thái đơn hàng</h3>
          <div className="space-y-6">
            <div>
              <div className="flex justify-between mb-1">
                <span className="text-sm font-medium text-gray-700">Hoàn thành</span>
                <span className="text-sm font-medium text-gray-900">{reports.orders?.status?.completed || 0}</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2.5">
                <div className="bg-green-600 h-2.5 rounded-full" style={{ width: `${(reports.orders?.status?.completed / reports.orders?.total) * 100 || 0}%` }}></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between mb-1">
                <span className="text-sm font-medium text-gray-700">Chờ xử lý</span>
                <span className="text-sm font-medium text-gray-900">{reports.orders?.status?.pending || 0}</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2.5">
                <div className="bg-yellow-400 h-2.5 rounded-full" style={{ width: `${(reports.orders?.status?.pending / reports.orders?.total) * 100 || 0}%` }}></div>
              </div>
            </div>

            <div>
              <div className="flex justify-between mb-1">
                <span className="text-sm font-medium text-gray-700">Đã hủy</span>
                <span className="text-sm font-medium text-gray-900">{reports.orders?.status?.cancelled || 0}</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2.5">
                <div className="bg-red-600 h-2.5 rounded-full" style={{ width: `${(reports.orders?.status?.cancelled / reports.orders?.total) * 100 || 0}%` }}></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ReportsPage;