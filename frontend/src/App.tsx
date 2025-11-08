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

          {/* Services */}
          {/* <Route path="/info/services/standard" element={<YourComponent />} />
          <Route path="/info/services/express" element={<YourComponent />} />
          <Route path="/info/services/flash" element={<YourComponent />} /> */}

          {/* Tracking */}
          {/* <Route path="/tracking/shipping-fee" element={<YourComponent />} />
          <Route path="/tracking/office-search" element={<YourComponent />} />
          <Route path="/tracking/order-tracking" element={<YourComponent />} />
          <Route path="/info/shipping-rates" element={<YourComponent />} /> */}

          {/* Other public info */}
          {/* <Route path="/info/promotions" element={<YourComponent />} />
          <Route path="/info/company" element={<YourComponent />} />
          <Route path="/info/contact" element={<YourComponent />} /> */}

          {/* Dynamic role routes */}
          <Route
            path="/:role/*"
            element={
              <PrivateRoute>
                <DashboardLayout />
              </PrivateRoute>}
          />
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;