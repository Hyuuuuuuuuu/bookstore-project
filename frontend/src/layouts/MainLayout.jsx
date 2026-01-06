import React from 'react';
import HeaderLayout from './HeaderLayout';
import FooterLayout from './FooterLayout';

const MainLayout = ({ children }) => {
  return (
    <div className="min-h-screen flex flex-col bg-gray-50">
      <HeaderLayout />
      
      {/* Main Content */}
      <main className="flex-1">
        {children}
      </main>

      <FooterLayout />

      {/* Chat Widget removed as per design request */}
    </div>
  );
};

export default MainLayout;
