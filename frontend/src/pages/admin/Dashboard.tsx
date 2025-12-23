import React, { useEffect, useState } from "react";
import { Card, Row, Col, Statistic, Typography } from "antd";
import { UserOutlined, ShoppingOutlined, CarOutlined, ShopOutlined } from "@ant-design/icons";
import dashboardApi from "../../api/dashboardApi";

const { Title } = Typography;

const AdminDashboard: React.FC = () => {
  const [counts, setCounts] = useState({ usersTotal: 0, ordersTotal: 0, vehiclesTotal: 0, officesTotal: 0 });

  useEffect(() => {
    (async () => {
      try {
        const res = await dashboardApi.getAdminCounts();
        setCounts(res);
      } catch (e) {
        
      }
    })();
  }, []);

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>
          Báo cáo & Thống kê
        </Title>
      </div>

      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic
              title="Tổng người dùng"
              value={counts.usersTotal}
              prefix={<UserOutlined />}
              valueStyle={{ color: "#1890ff" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic
              title="Tổng đơn hàng"
              value={counts.ordersTotal}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: "#3f8600" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic
              title="Tổng phương tiện"
              value={counts.vehiclesTotal}
              prefix={<CarOutlined />}
              valueStyle={{ color: "#cf1322" }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}>
            <Statistic
              title="Tổng bưu cục"
              value={counts.officesTotal}
              prefix={<ShopOutlined />}
              valueStyle={{ color: "#722ed1" }}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default AdminDashboard;



