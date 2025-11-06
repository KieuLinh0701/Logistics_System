import React from "react";
import { Layout, Button, Space, Typography } from "antd";
import { useNavigate } from "react-router-dom";

const { Header: AntHeader } = Layout;
const { Text } = Typography;

const Header: React.FC = () => {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem("user") || "{}");

  const handleLogout = () => {
    localStorage.removeItem("user"); 
    navigate("/login");
  };

  return (
    <AntHeader
      style={{
        background: "#1C3D90",
        padding: "0 24px",
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
      }}
    >
      {/* Tên website */}
      <Text style={{ color: "#fff", fontSize: 18, fontWeight: 600 }}>
        UTELogistics
      </Text>

      {/* Phần nút bên phải */}
      <Space>
        {user?.role && (
          <Button
            type="primary"
            ghost
            style={{ borderColor: "#fff", color: "#fff" }}
            onClick={() => navigate(`/${user.role}/orders/create`)}
          >
            Tạo đơn hàng
          </Button>
        )}
        <Button type="default" onClick={handleLogout}>
          Logout
        </Button>
      </Space>
    </AntHeader>
  );
};

export default Header;