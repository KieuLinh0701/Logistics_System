import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";

import LoginForm from "./pages/Login";
import RegisterForm from "./pages/Register";
import Home from "./pages/Home/Home";
import { AuthRoute } from "./components/route/AuthRoute";
import { PrivateRoute } from "./components/route/PrivateRoute";
import DashboardLayout from "./layouts/DashboardLayout";
import ForgotPassword from "./pages/ForgotPassword";
import AccountSettings from "./pages/profile/AccountSettings";
import DashboardRouter from "./pages/DashboardRouter";
import NotificationList from "./pages/notification/NotificationList";
import NotificationDetail from "./pages/notification/NotificationDetail";
import ServiceTypes from "./pages/info/ServiceTypes";
import ShippingFee from "./pages/tracking/shippingFee/ShippingFee";
import OfficeSearch from "./pages/tracking/officeSearch/OfficeSearch";
import ShippingRates from "./pages/tracking/shippingRate/ShippingRates";
import "./styles/theme.css";
import CompanyInfo from "./pages/info/CompanyInfo";
import ContactForm from "./pages/info/ContactForm";
import PromotionList from "./pages/info/PromotionList";

import AdminUsers from "./pages/admin/Users";
import AdminOrders from "./pages/admin/Orders";
import AdminVehicles from "./pages/admin/Vehicles";
import AdminPostOffices from "./pages/admin/PostOffices";
import AdminServiceTypes from "./pages/admin/ServiceTypes";

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
          </Route>
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;