// App.tsx
import React from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { ConfigProvider } from "antd";
import viVN from "antd/locale/vi_VN";

import LoginForm from "./pages/LoginForm";
import RegisterForm from "./pages/RegisterForm";
import Home from "./pages/Home/Home";
import Profile from "./pages/Profile";

import AdminLayout from "./layouts/AdminLayout";
import ManagerLayout from "./layouts/ManagerLayout";
import UserLayout from "./layouts/UserLayout";
import ShipperLayout from "./layouts/ShipperLayout";

import { PrivateRoute } from "./components/PrivateRoute";
import { AuthRoute } from "./components/AuthRoute";

const App: React.FC = () => {
  const user = JSON.parse(sessionStorage.getItem("user") || "null");

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

          {/* Private pages by role */}
          <Route path="/admin/*" element={
            <PrivateRoute roles={["admin"]}>
              <AdminLayout />
            </PrivateRoute>
          } />

          <Route path="/manager/*" element={
            <PrivateRoute roles={["manager"]}>
              <ManagerLayout />
            </PrivateRoute>
          } />

          <Route path="/user/*" element={
            <PrivateRoute roles={["user"]}>
              <UserLayout />
            </PrivateRoute>
          } />

          <Route path="/shipper/*" element={
            <PrivateRoute roles={["shipper"]}>
              <ShipperLayout />
            </PrivateRoute>
          } />

          {/* Example profile accessible to all logged-in users */}
          <Route path="/profile" element={
            user ? <Profile /> : <Navigate to="/login" replace />
          } />
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;