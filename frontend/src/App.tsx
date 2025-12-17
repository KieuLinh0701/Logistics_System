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
import AdminPostOffices from "./pages/admin/PostOffices";
import AdminServiceTypes from "./pages/admin/ServiceTypes";
import UserProducts from "./pages/user/product/UserProducts";
import UserBankAccounts from "./pages/user/bankAcount/UserBankAccounts";
import ShippingFeeBody from "./pages/common/tracking/shippingFee/ShippingFeeBody";
import ShippingRatesBody from "./pages/common/tracking/shippingRate/ShippingRatesBody";
import OfficeSearchBody from "./pages/common/tracking/officeSearch/OfficeSearchBody";
import UserOrderEdit from "./pages/user/order/edit/UserOrderEdit";
import UserShippingRequests from "./pages/user/order/request/UserShippingRequests";
import OrderListRouter from "./pages/router/OrderListRouter";
import VehiclesRouter from "./pages/router/VehiclesRouter";
import ManagerOffice from "./pages/manager/office/ManagerOffice";
import ManagerShippingRequests from "./pages/manager/order/request/ManagerShippingRequest";
import ManagerEmployeeList from "./pages/manager/employee/list/ManagerEmployeeList";
import ManagerEmployeePerformance from "./pages/manager/employee/perfomance/ManagerEmployeePerformance";
import ManagerEmployeePerfomanceShipment from "./pages/manager/employee/perfomance-shipment/ManagerEmployeePerfomanceShipment";
import OrderCreateRouter from "./pages/router/OrderCreateRouter";
import OrderDetailRouter from "./pages/router/OrderDetailRouter";
import WaybillPrintRouter from "./pages/router/WaybillPrintRouter";
import UserOrderDetail from "./pages/user/order/detail/UserOrderDetail";
import ManagerShipperAssign from "./pages/manager/employee/assign/ManagerShipperAssigns";
import ManagerShipments from "./pages/manager/shipment/ManagerShipments";
import ManagerShipperAssignmentHistory from "./pages/manager/employee/history-assign/ManagerShipperAssignmentHistories";
import ManagerIncidentReports from "./pages/manager/order/incident/ManagerIncidentReports";
import ManagerPaymentSubmissionBatchs from "./pages/manager/paymentSubmissionBatch/ManagerPaymentSubmissionBatchs";
import ManagerPaymentSubmissions from "./pages/manager/paymentSubmission/ManagerPaymentSubmissions";
import OrderTracking from "./pages/common/tracking/OrderTracking";
import SettlementRouter from "./pages/router/SettlementRouter";
import SettlementDetailRouter from "./pages/router/SettlementDetailRouter";

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
          <Route path="/tracking/order-tracking" element={<OrderTracking />} />
          <Route path="/tracking/order-tracking/:trackingNumber" element={<OrderTracking />} />

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

            {/* Admin & Manager routes */}
            <Route path="/vehicles" element={<PrivateRoute allowedRoles={['admin', 'manager']}><VehiclesRouter /></PrivateRoute>} />

            {/* User & Manager routes */}
            <Route path="/orders/list" element={<PrivateRoute allowedRoles={['user', 'manager']}><OrderListRouter /></PrivateRoute>} />
            <Route path="/orders/create" element={<PrivateRoute allowedRoles={['user', 'manager']}><OrderCreateRouter /></PrivateRoute>} />
            <Route path="/orders/print" element={<PrivateRoute allowedRoles={['user', 'manager']}><WaybillPrintRouter /></PrivateRoute>} />
            <Route path="/settlements" element={<PrivateRoute allowedRoles={['user', 'manager']}><SettlementRouter /></PrivateRoute>} />
            <Route path="/settlements/:id" element={<PrivateRoute allowedRoles={['manager', 'user']}><SettlementDetailRouter /></PrivateRoute>} />
            
            <Route path="/orders/tracking/:trackingNumber/edit" element={<PrivateRoute allowedRoles={['user']}><UserOrderEdit /></PrivateRoute>} />
            <Route path="/orders/tracking/:trackingNumber" element={<PrivateRoute allowedRoles={['user', 'manager']}><OrderDetailRouter /></PrivateRoute>} />

            {/* User routes */}
            <Route path="/orders/requests" element={<PrivateRoute allowedRoles={['user']}><UserShippingRequests /></PrivateRoute>} />
            <Route path="/orders/id/:orderId/edit" element={<PrivateRoute allowedRoles={['user']}><UserOrderEdit /></PrivateRoute>} />
            <Route path="/orders/id/:orderId" element={<PrivateRoute allowedRoles={['user']}><UserOrderDetail /></PrivateRoute>} />
            <Route path="/products" element={<PrivateRoute allowedRoles={['user']}><UserProducts /></PrivateRoute>} />
            <Route path="/bank-accounts" element={<PrivateRoute allowedRoles={['user']}><UserBankAccounts /></PrivateRoute>} />
            <Route path="/shipping-fee" element={<PrivateRoute allowedRoles={['user']}><ShippingFeeBody /></PrivateRoute>} />
            <Route path="/office-search" element={<PrivateRoute allowedRoles={['user']}><OfficeSearchBody /></PrivateRoute>} />
            <Route path="/shipping-rates" element={<PrivateRoute allowedRoles={['user']}><ShippingRatesBody /></PrivateRoute>} />

            {/* Manager */}
            <Route path="/office" element={<PrivateRoute allowedRoles={['manager']}><ManagerOffice /></PrivateRoute>} />
            <Route path="/supports" element={<PrivateRoute allowedRoles={['manager']}><ManagerShippingRequests /></PrivateRoute>} />
            <Route path="/employees/list" element={<PrivateRoute allowedRoles={['manager']}><ManagerEmployeeList /></PrivateRoute>} />
            <Route path="/employees/performance" element={<PrivateRoute allowedRoles={['manager']}><ManagerEmployeePerformance /></PrivateRoute>} />
            {/* <Route path="employees/performance/:employeeCode/shipments" element={<PrivateRoute allowedRoles={['manager']}><ManagerEmployeePerfomanceShipment /></PrivateRoute>} /> */}
            {/* <Route path="employees/performance/:employeeCode/shipments/:shipmentCode/orders" element={<ShipmentOrders />} /> */}
            <Route path="/shipments" element={<PrivateRoute allowedRoles={['manager']}><ManagerShipments /></PrivateRoute>} />
            <Route path="/employees/assign-area" element={<PrivateRoute allowedRoles={['manager']}><ManagerShipperAssign /></PrivateRoute>} />
            <Route path="/employees/assign-history" element={<PrivateRoute allowedRoles={['manager']}><ManagerShipperAssignmentHistory /></PrivateRoute>} />
            <Route path="/orders/incidents" element={<PrivateRoute allowedRoles={['manager']}><ManagerIncidentReports /></PrivateRoute>} />
          </Route>
        </Routes>
      </Router>
    </ConfigProvider>
  );
};

export default App;