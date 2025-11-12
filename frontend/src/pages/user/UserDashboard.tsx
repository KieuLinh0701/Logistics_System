import React from "react";
import { Typography } from "antd";

const { Title } = Typography;

const UserDashboard: React.FC = () => {
  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Dashboard</Title>
      <p>Xin chào, bạn đang đăng nhập với role: <strong>User</strong></p>
    </div>
  );
};

export default UserDashboard;