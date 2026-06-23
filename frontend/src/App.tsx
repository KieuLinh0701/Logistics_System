import React from "react";
import {BrowserRouter as Router, Navigate, Route, Routes} from "react-router-dom";
import {ConfigProvider} from "antd";
import viVN from "antd/locale/vi_VN";

import LoginForm from "./pages/common/Login";
import RegisterForm from "./pages/common/Register";
import Home from "./pages/common/Home/Home";
import {AuthRoute} from "./components/route/AuthRoute";
import {PrivateRoute} from "./components/route/PrivateRoute";
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
import OrderTracking from "./pages/common/tracking/OrderTracking";

import AdminUsers from "./pages/admin/users/UsersPage";
import AdminOrders from "./pages/admin/orders/OrdersPage";
import AdminPostOffices from "./pages/admin/postoffices/PostOfficesPage";
import AdminServiceTypes from "./pages/admin/service-types/ServiceTypesPage";
import AdminFeeConfigurations from "./pages/admin/fee-configurations/FeeConfigurationsPage";
import ReportsPage from "./pages/admin/reports/ReportsPage";
import AdminAuditLogsByUser from "./pages/admin/audit-logs-user/AdminAuditLogsByUser.tsx";

// Manager
import ManagerOffice from "./pages/manager/office/ManagerOffice";
import ManagerShippingRequests from "./pages/manager/order/request/ManagerShippingRequest";
import ManagerEmployeeList from "./pages/manager/employee/list/ManagerEmployeeList";
import ManagerEmployeePerformance from "./pages/manager/employee/perfomance/ManagerEmployeePerformance";
import ManagerShipmentOrders from "./pages/manager/shipment/ManagerShipmentOrders";
import ManagerEmployeePerfomanceShipment
    from "./pages/manager/employee/perfomance-shipment/ManagerEmployeePerfomanceShipment";
import ManagerShipperAssign from "./pages/manager/employee/assign/ManagerShipperAssigns";
import ManagerShipments from "./pages/manager/shipment/ManagerShipments";
import ManagerShipperAssignmentHistory from "./pages/manager/employee/history-assign/ManagerShipperAssignmentHistories";
import ManagerIncidentReports from "./pages/manager/order/incident/ManagerIncidentReports";
import UserOrderDetail from "./pages/user/order/detail/UserOrderDetail";
import ManagerAuditLogsByEmployee from "./pages/manager/employee/audit-logs/ManagerAuditLogsByEmployee.tsx";
import ManagerAiRouteOptimization from "./pages/manager/ai-route/ManagerAiRouteOptimization";
import ManagerUrgentOrderList from "./pages/manager/order/urgent-pickup/ManagerUrgentOrderList.tsx";

import OrderListRouter from "./pages/router/OrderListRouter";
import VehiclesRouter from "./pages/router/VehiclesRouter";
import OrderCreateRouter from "./pages/router/OrderCreateRouter";
import OrderDetailRouter from "./pages/router/OrderDetailRouter";
import WaybillPrintRouter from "./pages/router/WaybillPrintRouter";
import SettlementRouter from "./pages/router/SettlementRouter";
import SettlementDetailRouter from "./pages/router/SettlementDetailRouter";
import OrderEditRouter from "./pages/router/OrderEditRouter";
import AuditLogsRouter from "./pages/router/AuditLogsRouter.tsx";
import AuditLogsDetailRouter from "./pages/router/AuditLogsDetailRouter.tsx";

// User
import UserRoleList from "./pages/user/grouppermission/list/UserRoleList.tsx";
import UserEmployeeByRoleIdList from "./pages/user/grouppermission/employee/UserEmployeeByRoleIdList.tsx";
import UserProducts from "./pages/user/product/UserProducts";
import UserCustomers from "./pages/user/customer/UserCustomers.tsx";
import UserBankAccounts from "./pages/user/bankAcount/UserBankAccounts";
import UserOrderEdit from "./pages/user/order/edit/UserOrderEdit";
import UserShippingRequests from "./pages/user/order/request/UserShippingRequests";
import UserEmployeeList from "./pages/user/employee/list/UserEmployeeList.tsx";
import UserEmployeeHistory from "./pages/user/employee/history/UserEmployeeHistory.tsx";

// Shipper
import ShipperOrders from "./pages/shipper/Orders";
import ShipperUnassignedOrders from "./pages/shipper/UnassignedOrders";
import ShipperOrderDetail from "./pages/shipper/OrderDetail";
import ShipperDeliveryRoute from "./pages/shipper/DeliveryRoute";
import ShipperDeliveryHistory from "./pages/shipper/DeliveryHistory";
import ShipperCODManagement from "./pages/shipper/CODManagement";
import ShipperIncidentReport from "./pages/shipper/IncidentReport";
import ShippingRequests from "./pages/shipper/shippingRequests/ShippingRequests";
import ShipperBarcodeScanner from "./pages/shipper/BarcodeScanner";
import FailedDeliveryOrders from "./pages/shipper/FailedDeliveryOrders";

// Driver
import DriverShipments from "./pages/driver/Shipments";
import DriverRoute from "./pages/driver/Route";
import DriverHistory from "./pages/driver/History";
import PromotionList from "./pages/common/info/PromotionList";
import AdminPromotions from "./pages/admin/promotions/PromotionsPage";
import ShippingRequestsAdmin from "./pages/admin/shipping-requests/ShippingRequestsPage";
import MyLeavePage from "./pages/leave/MyLeavePage";
import LeaveManagementPage from "./pages/leave/LeaveManagementPage";
import SupportChatPage from "./pages/chat/SupportChatPage";
import ChatWidget from "./pages/chat/ChatWidget";
import ContactManagerPage from "./pages/chat/ContactManagerPage";
import InternalEmployeeChatPage from "./pages/chat/InternalEmployeeChatPage";

// Recruitment
import RecruitmentPage from "./pages/common/recruitment/RecruitmentPage";
import JobDetailPage from "./pages/common/recruitment/JobDetailPage";
import ApplyJobPage from "./pages/common/recruitment/ApplyJobPage";
import JobPostingManagementPage from "./pages/hr/recruitment/job-posting/JobPostingManagementPage";
import ApplicationReviewPage from "./pages/hr/recruitment/application/ApplicationReviewPage";

const App: React.FC = () => {
    return (
        <ConfigProvider locale={viVN}>
            <Router>
                <Routes>
                    {/* Redirect root */}
                    <Route path="/" element={<Navigate to="/home" replace/>}/>

                    {/* Public pages */}
                    <Route path="/home" element={<Home/>}/>
                    {/* Dịch vụ */}
                    <Route path="/info/services" element={<ServiceTypes/>}/>
                    <Route path="/info/company" element={<CompanyInfo/>}/>
                    <Route path="/info/contact" element={<ContactForm/>}/>
                    <Route path="/info/promotions" element={<PromotionList/>}/>

                    {/* Tra cứu */}
                    <Route path="/tracking/shipping-fee" element={<ShippingFee/>}/>
                    <Route path="/tracking/office-search" element={<OfficeSearch/>}/>
                    <Route path="/tracking/shipping-rates" element={<ShippingRates/>}/>
                    <Route path="/tracking/order-tracking" element={<OrderTracking/>}/>
                    <Route path="/tracking/order-tracking/:trackingNumber" element={<OrderTracking/>}/>

                    {/* Recruitment public routes */}
                    <Route path="/jobs" element={<RecruitmentPage/>}/>
                    <Route path="/jobs/:id" element={<JobDetailPage/>}/>
                    <Route path="/jobs/:id/apply" element={<ApplyJobPage/>}/>

                    <Route path="/login" element={<AuthRoute type="public"><LoginForm/></AuthRoute>}/>
                    <Route path="/register" element={<AuthRoute type="public"><RegisterForm/></AuthRoute>}/>
                    <Route path="/forgot-password" element={<AuthRoute type="public"><ForgotPassword/></AuthRoute>}/>

                    {/* Dynamic role routes */}
                    <Route path="/" element={<PrivateRoute><DashboardLayout/></PrivateRoute>}>
                        <Route path="/account/settings" element={<PrivateRoute><AccountSettings/></PrivateRoute>}/>
                        <Route path="/dashboard" element={<PrivateRoute><DashboardRouter/></PrivateRoute>}/>
                        <Route path="/notifications" element={<PrivateRoute><NotificationList/></PrivateRoute>}/>
                        <Route path="/notifications/:id" element={<PrivateRoute><NotificationDetail/></PrivateRoute>}/>

                        {/* Admin routes */}
                        <Route path="/users"
                               element={<PrivateRoute allowedPermissionGroups={["group_admin"]}><AdminUsers/></PrivateRoute>}/>
                        <Route path="/users/:id/logs" element={<PrivateRoute
                            allowedPermissionGroups={["group_admin"]}><AdminAuditLogsByUser/></PrivateRoute>}/>
                        <Route path="/postoffices"
                               element={<PrivateRoute allowedPermissionGroups={["group_admin"]}><AdminPostOffices/></PrivateRoute>}/>
                        <Route path="/service-types"
                               element={<PrivateRoute allowedPermissionGroups={["group_admin"]}><AdminServiceTypes/></PrivateRoute>}/>
                        <Route path="/orders"
                               element={<PrivateRoute allowedPermissionGroups={["group_admin"]}><AdminOrders/></PrivateRoute>}/>
                        <Route path="/shipping-requests" element={<PrivateRoute
                            allowedPermissionGroups={["group_admin"]}><ShippingRequestsAdmin/></PrivateRoute>}/>
                        <Route path="/promotions"
                               element={<PrivateRoute allowedPermissionGroups={["group_admin"]}><AdminPromotions/></PrivateRoute>}/>
                        <Route path="/fee-configurations" element={<PrivateRoute
                            allowedPermissionGroups={["group_admin"]}><AdminFeeConfigurations/></PrivateRoute>}/>
                        <Route path="/reports"
                               element={<PrivateRoute allowedPermissionGroups={["group_admin"]}><ReportsPage/></PrivateRoute>}/>

                        <Route path="/support/tickets" element={<PrivateRoute
                            allowedPermissionGroups={["group_admin", "group_manager"]}><SupportChatPage/></PrivateRoute>}/>
                        <Route path="/support/tickets/:id" element={<PrivateRoute
                            allowedPermissionGroups={["group_admin", "group_manager"]}><SupportChatPage/></PrivateRoute>}/>

                        <Route
                            path="/recruitment/hr/jobs"
                            element={<PrivateRoute
                                allowedPermissionGroups={["group_admin", "group_manager"]}><JobPostingManagementPage/></PrivateRoute>}
                        />
                        <Route
                            path="/recruitment/hr/applications"
                            element={<PrivateRoute
                                allowedPermissionGroups={["group_admin", "group_manager"]}><ApplicationReviewPage/></PrivateRoute>}
                        />

                        {/* Admin & Manager routes */}
                        <Route path="/vehicles" element={<PrivateRoute
                            allowedPermissionGroups={["group_admin", "group_manager"]}><VehiclesRouter/></PrivateRoute>}/>

                        {/* User & Manager routes */}
                        <Route path="/orders/list" element={<PrivateRoute
                            allowedPermissionGroups={["group_user", "group_manager", "user_order_view"]}><OrderListRouter/></PrivateRoute>}/>
                        <Route path="/orders/create" element={<PrivateRoute
                            allowedPermissionGroups={["group_user", "group_manager", "user_order_create"]}><OrderCreateRouter/></PrivateRoute>}/>
                        <Route path="/orders/print" element={<PrivateRoute
                            allowedPermissionGroups={["group_user", "group_manager", "user_order_print_bulk", "user_order_print_single"]}><WaybillPrintRouter/></PrivateRoute>}/>
                        <Route path="/orders/tracking/:trackingNumber/edit" element={<PrivateRoute
                            allowedPermissionGroups={["group_user", "group_manager", "user_order_edit"]}><OrderEditRouter/></PrivateRoute>}/>
                        <Route path="/orders/tracking/:trackingNumber" element={<PrivateRoute
                            allowedPermissionGroups={["group_user", "group_manager", "user_order_detail"]}><OrderDetailRouter/></PrivateRoute>}/>
                        <Route path="/settlements" element={<PrivateRoute
                            allowedPermissionGroups={["group_user", "group_manager", "user_cod_session_view"]}><SettlementRouter/></PrivateRoute>}/>
                        <Route path="/settlements/:id" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager", "group_user", "user_cod_detail"]}><SettlementDetailRouter/></PrivateRoute>}/>
                        <Route path="/logs" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager", "group_user", "user_audit_log_view", "group_admin"]}><AuditLogsRouter/></PrivateRoute>}/>
                        <Route path="/employees/:id/logs"
                               element={<PrivateRoute allowedPermissionGroups={["group_manager", "group_user", "user_audit_log_detail_view"]}><AuditLogsDetailRouter/></PrivateRoute>}/>

                        {/* User routes */}
                        <Route path="/orders/requests"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_support_view"]}><UserShippingRequests/></PrivateRoute>}/>
                        <Route path="/orders/id/:orderId/edit"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_order_edit"]}><UserOrderEdit/></PrivateRoute>}/>
                        <Route path="/orders/id/:orderId"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_order_detail"]}><UserOrderDetail/></PrivateRoute>}/>
                        <Route path="/products"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_product_view"]}><UserProducts/></PrivateRoute>}/>
                        <Route path="/employees"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_employee_view"]}><UserEmployeeList/></PrivateRoute>}/>
                        <Route path="/employees/:id/work-history"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_employee_history_view"]}><UserEmployeeHistory/></PrivateRoute>}/>
                        <Route path="/roles"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_permission_group_view"]}><UserRoleList/></PrivateRoute>}/>
                        <Route path="/roles/:id/employees"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_permission_group_user_view"]}><UserEmployeeByRoleIdList/></PrivateRoute>}/>
                        <Route path="/customers"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_customer_view"]}><UserCustomers/></PrivateRoute>}/>
                        <Route path="/bank-accounts"
                               element={<PrivateRoute allowedPermissionGroups={["group_user", "user_bank_view"]}><UserBankAccounts/></PrivateRoute>}/>

                        {/* Manager */}
                        <Route path="/office" element={<PrivateRoute 
                            allowedPermissionGroups={["group_manager"]}><ManagerOffice/></PrivateRoute>}/>
                        <Route path="/supports" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerShippingRequests/></PrivateRoute>}/>
                        <Route path="/employees/list" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerEmployeeList/></PrivateRoute>}/>
                        <Route path="/employees/performance" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerEmployeePerformance/></PrivateRoute>}/>
                        <Route path="/employees/performance/:employeeId/shipments" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerEmployeePerfomanceShipment/></PrivateRoute>}/>
                        <Route path="/employees/performance/:employeeId/shipments/:shipmentId/orders"
                            element={<PrivateRoute allowedPermissionGroups={["group_manager"]}><ManagerShipmentOrders/></PrivateRoute>}/>
                        <Route path="/shipments" element={<PrivateRoute 
                            allowedPermissionGroups={["group_manager"]}><ManagerShipments/></PrivateRoute>}/>
                        <Route path="/manager/ai-routes" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerAiRouteOptimization/></PrivateRoute>}/>
                        <Route path="/shipments/:shipmentId/orders" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerShipmentOrders/></PrivateRoute>}/>
                        <Route path="/employees/assign-area" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerShipperAssign/></PrivateRoute>}/>
                        <Route path="/employees/assign-history" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerShipperAssignmentHistory/></PrivateRoute>}/>
                        <Route path="/orders/incidents" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerIncidentReports/></PrivateRoute>}/>
                        <Route path="/leaves" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><LeaveManagementPage/></PrivateRoute>}/>
                        <Route path="/manager/internal-chat" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><InternalEmployeeChatPage/></PrivateRoute>}/>
                        <Route path="/employees/:id/logs" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerAuditLogsByEmployee/></PrivateRoute>}/>
                        <Route path="/orders/urgent" element={<PrivateRoute
                            allowedPermissionGroups={["group_manager"]}><ManagerUrgentOrderList/></PrivateRoute>}/>

                        {/* Shipper routes */}
                        <Route path="/shipper/orders-unassigned" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><ShipperUnassignedOrders/></PrivateRoute>}/>
                        <Route path="/shipper/orders"
                               element={<PrivateRoute allowedPermissionGroups={["group_shipper"]}><ShipperOrders/></PrivateRoute>}/>
                        <Route path="/shipper/orders/:id"
                               element={<PrivateRoute allowedPermissionGroups={["group_shipper"]}><ShipperOrderDetail/></PrivateRoute>}/>
                        <Route path="/shipper/scan-barcode" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><ShipperBarcodeScanner/></PrivateRoute>}/>
                        <Route path="/route" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><ShipperDeliveryRoute/></PrivateRoute>}/>
                        <Route path="/shipper/shipping-requests" element={<PrivateRoute 
                            allowedPermissionGroups={["group_shipper"]}><ShippingRequests/></PrivateRoute>}/>
                        <Route path="/shipper/failed-deliveries" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><FailedDeliveryOrders/></PrivateRoute>}/>
                        <Route path="/history" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><ShipperDeliveryHistory/></PrivateRoute>}/>
                        <Route path="/cod" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><ShipperCODManagement/></PrivateRoute>}/>
                        <Route path="/report" element={<PrivateRoute 
                            allowedPermissionGroups={["group_shipper"]}><ShipperIncidentReport/></PrivateRoute>}/>
                        <Route path="/shipper/contact-manager" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><ContactManagerPage/></PrivateRoute>}/>
                        <Route path="/employee/leaves" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper", "group_driver"]}><MyLeavePage/></PrivateRoute>}/>
                        <Route path="/shipper/settings/vehicle" element={<PrivateRoute
                            allowedPermissionGroups={["group_shipper"]}><Navigate to="/account/settings?tab=vehicle" replace /></PrivateRoute>}/>

                        {/* Driver routes */}
                        <Route path="/driver/shipments"
                               element={<PrivateRoute allowedPermissionGroups={["group_driver"]}><DriverShipments/></PrivateRoute>}/>
                        <Route path="/driver/route"
                               element={<PrivateRoute allowedPermissionGroups={["group_driver"]}><DriverRoute/></PrivateRoute>}/>
                        <Route path="/driver/history"
                               element={<PrivateRoute allowedPermissionGroups={["group_driver"]}><DriverHistory/></PrivateRoute>}/>
                        <Route path="/driver/contact-manager" element={<PrivateRoute
                            allowedPermissionGroups={["group_driver"]}><ContactManagerPage/></PrivateRoute>}/>
                    </Route>
                </Routes>
                <ChatWidget/>
            </Router>
        </ConfigProvider>
    );
};

export default App;