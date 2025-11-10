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
import Profile from "./pages/Profile";
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
          <Route
            path="/:role/*"
            element={
              <PrivateRoute>
                <DashboardLayout />
              </PrivateRoute>
            }
          >
            {/* Nested routes */}
            <Route path="profile" element={<Profile />} />
            <Route path="notifications" element={<NotificationList />} />
            <Route path="notifications/:id" element={<NotificationDetail />} />
          </Route>
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;