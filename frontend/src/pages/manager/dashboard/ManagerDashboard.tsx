import { Row, Col, message } from "antd";
import { useEffect, useState } from "react";
import dashboardApi from "../../../api/dashboardApi";
import { OrderStatsOverview } from "./components/OrderStatusOverview";
import DateFilter from "./components/DateFilter";
import type { ManagerDashboardOverviewResponse } from "../../../types/dashboard";
import { VehicleTypeChart } from "./components/VehicleTypeChart";
import "./ManagerDashboard.css"
import { VehicleStatsOverview } from "./components/VehicleStatsOverview";
import { PaymentBatchStatsOverview } from "./components/PaymentBatchStatsOverview";
import { EmployeeStatusOverview } from "./components/EmployeeStatusOverview";
import { EmployeeTypeChart } from "./components/EmployeeTypeChart";
import { IncidentStatsOverview } from "./components/IncidentStatsOverview";
import { ShipmentStatsOverview } from "./components/ShipmentStatsOverview";
import { ShippingRequestStatsOverview } from "./components/ShippingRequestStatsOverview";

const ManagerDashboard: React.FC = () => {
  const [data, setData] = useState<ManagerDashboardOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOverView = async () => {
      setLoading(true);
      try {
        const result = await dashboardApi.getManagerOverview();
        if (result.success) setData(result.data);
        else message.warning(result.message || "Không thể tải dữ liệu tổng quan. Vui lòng thử lại sau.");
      } catch (error: any) {
        message.error(error.message || "Đã xảy ra lỗi hệ thống. Vui lòng thử lại.");
      } finally {
        setLoading(false);
      }
    };

    fetchOverView();
  }, []);

  if (loading) return <div>Loading dashboard...</div>;

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <DateFilter />
        <div className="manager-dashboard-note">
          Tổng quan hoạt động hệ thống theo thời gian thực, hỗ trợ quản lý và điều phối công việc hiệu quả hơn.
        </div>

        <Row gutter={[16, 16]}>
          <Col span={24}>
            <OrderStatsOverview data={data!.orders} />
          </Col>
        </Row>

        <Row gutter={[16, 16]} className="manager-dashboard-divide">
          <Col span={24}>
            <PaymentBatchStatsOverview data={data!.payments} />
          </Col>
        </Row>

        <Row gutter={[16, 16]} className="manager-dashboard-divide">
          <Col span={24}>
            <ShippingRequestStatsOverview data={data!.shippingRequests} />
          </Col>
        </Row>

        <Row gutter={[16, 16]} className="manager-dashboard-divide">
          <Col span={24}>
            <IncidentStatsOverview data={data!.incidents} />
          </Col>
        </Row>

        <Row gutter={[16, 16]} className="manager-dashboard-divide">
          <Col span={24}>
            <ShipmentStatsOverview data={data!.shipments} />
          </Col>
        </Row>

        <Row gutter={[16, 16]} className="manager-dashboard-divide">
          <Col xs={24} md={12} lg={12}>
            <EmployeeStatusOverview data={data!.employees} />
          </Col>

          <Col xs={24} md={12} lg={12} className="manager-dashboard-divide">
            <EmployeeTypeChart data={data!.employeeShiftCounts} />
          </Col>
        </Row>

        <Row gutter={[16, 16]} className="manager-dashboard-divide">
          <Col xs={24} md={12} lg={12}>
            <VehicleStatsOverview data={data!.vehicles} />
          </Col>

          <Col xs={24} md={12} lg={12} className="manager-dashboard-divide">
            <VehicleTypeChart data={data!.vehicleCounts} />
          </Col>
        </Row>
      </div>
    </div>
  );
};

export default ManagerDashboard;