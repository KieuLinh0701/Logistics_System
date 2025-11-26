import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";

import LoginForm from "./pages/common/Login";
import RegisterForm from "./pages/common/Register";
import Home from "./pages/common/Home/Home";
import { AuthRoute } from "./components/route/AuthRoute";
import { PrivateRoute } from "./components/route/PrivateRoute";
import DashboardLayout from "./layouts/DashboardLayout";
import ForgotPassword from "./pages/common/ForgotPassword";
import AccountSettings from "./pages/common/profile/AccountSettings";
import DashboardRouter from "./pages/router/DashboardRouter";
import NotificationList from "./pages/common/notification/NotificationList";
import NotificationDetail from "./pages/common/notification/NotificationDetail";
import ServiceTypes from "./pages/common/info/ServiceTypes";
import ShippingFee from "./pages/common/tracking/shippingFee/ShippingFee";
import OfficeSearch from "./pages/common/tracking/officeSearch/OfficeSearch";
import ShippingRates from "./pages/common/tracking/shippingRate/ShippingRates";
import "./styles/theme.css";
import CompanyInfo from "./pages/common/info/CompanyInfo";
import ContactForm from "./pages/common/info/ContactForm";
import PromotionList from "./pages/common/info/PromotionList";

import AdminUsers from "./pages/admin/Users";
import AdminOrders from "./pages/admin/Orders";
import AdminVehicles from "./pages/admin/Vehicles";
import AdminPostOffices from "./pages/admin/PostOffices";
import AdminServiceTypes from "./pages/admin/ServiceTypes";
import AdminPromotions from "./pages/admin/Promotions";
import AdminFeeConfigurations from "./pages/admin/FeeConfigurations";
import UserOrderList from "./pages/user/order/list/UserOrderList";
import UserOrderCreate from "./pages/user/order/create/UserOrderCreate";
import UserProducts from "./pages/user/product/UserProducts";

const App: React.FC = () => {
  return (
    <ConfigProvider locale={viVN}>
      <Router>
        <Routes>
          {/* Redirect root */}
          <Route path="/" element={<Navigate to="/home" replace />} />

          {/* Public pages */}
          <Route path="/home" element={<Home />} />

          {/* Dịch vụ */}
          <Route path="/info/services" element={<ServiceTypes />} />
          <Route path="/info/company" element={<CompanyInfo />} />
          <Route path="/info/contact" element={<ContactForm />} />
          <Route path="/info/promotions" element={<PromotionList />} />

          {/* Tra cứu */}
          <Route path="/tracking/shipping-fee" element={<ShippingFee />} />
          <Route path="/tracking/office-search" element={<OfficeSearch />} />
          <Route path="/tracking/shipping-rates" element={<ShippingRates />} />

          <Route path="/login" element={<AuthRoute type="public"><LoginForm /></AuthRoute>} />
          <Route path="/register" element={<AuthRoute type="public"><RegisterForm /></AuthRoute>} />
          <Route path="/forgot-password" element={<AuthRoute type="public"><ForgotPassword /></AuthRoute>} />

          {/* Dynamic role routes */}
          <Route path="/" element={<PrivateRoute><DashboardLayout /></PrivateRoute>}>
            <Route path="/account/settings" element={<PrivateRoute><AccountSettings /></PrivateRoute>} />
            <Route path="/dashboard" element={<PrivateRoute><DashboardRouter /></PrivateRoute>} />
            <Route path="/notifications" element={<PrivateRoute><NotificationList /></PrivateRoute>} />
            <Route path="/notifications/:id" element={<PrivateRoute><NotificationDetail /></PrivateRoute>} />

            {/* Admin routes */}
            <Route path="/users" element={<PrivateRoute allowedRoles={['admin']}><AdminUsers /></PrivateRoute>} />
            <Route path="/postoffices" element={<PrivateRoute allowedRoles={['admin']}><AdminPostOffices /></PrivateRoute>} />
            <Route path="/service-types" element={<PrivateRoute allowedRoles={['admin']}><AdminServiceTypes /></PrivateRoute>} />
            <Route path="/orders" element={<PrivateRoute allowedRoles={['admin']}><AdminOrders /></PrivateRoute>} />
            <Route path="/vehicles" element={<PrivateRoute allowedRoles={['admin']}><AdminVehicles /></PrivateRoute>} />
            <Route path="/promotions" element={<PrivateRoute allowedRoles={['admin']}><AdminPromotions /></PrivateRoute>} />
            <Route path="/fee-configurations" element={<PrivateRoute allowedRoles={['admin']}><AdminFeeConfigurations /></PrivateRoute>} />

            {/* User routes */}
            <Route path="/orders/list" element={<PrivateRoute allowedRoles={['user']}><UserOrderList /></PrivateRoute>} />
            <Route path="/orders/create" element={<PrivateRoute allowedRoles={['user']}><UserOrderCreate /></PrivateRoute>} />
            <Route path="/products" element={<PrivateRoute allowedRoles={['user']}><UserProducts /></PrivateRoute>} />
          </Route>
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;