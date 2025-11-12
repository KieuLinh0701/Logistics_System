import React from "react";
import { Typography } from "antd";

const { Title } = Typography;

const ManagerDashboard: React.FC = () => {
  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Dashboard</Title>
      <p>Xin chào, bạn đang đăng nhập với role: <strong>Manager</strong></p>
    </div>
  );
};

export default ManagerDashboard;