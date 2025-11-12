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

const App: React.FC = () => {
  return (
    <ConfigProvider locale={viVN}>
      <Router>
        <Routes>
          {/* Redirect root */}
          <Route path="/" element={<Navigate to="/home" replace />} />

          {/* Public pages */}
          <Route path="/home" element={<Home />} />
          <Route path="/login" element={<AuthRoute type="public"><LoginForm /></AuthRoute>} />
          <Route path="/register" element={<AuthRoute type="public"><RegisterForm /></AuthRoute>} />
          <Route path="/forgot-password" element={<AuthRoute type="public"><ForgotPassword /></AuthRoute>} />

          {/* Dynamic role routes */}
          <Route path="/" element={<PrivateRoute><DashboardLayout /></PrivateRoute>}>
            <Route path="/account/settings" element={<PrivateRoute><AccountSettings /></PrivateRoute>} />
            <Route path="/dashboard" element={<PrivateRoute><DashboardRouter /></PrivateRoute>} />
            <Route path="/notifications" element={<PrivateRoute><NotificationList /></PrivateRoute>} />
            <Route path="/notifications/:id" element={<PrivateRoute><NotificationDetail /></PrivateRoute>} />
          </Route>
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;