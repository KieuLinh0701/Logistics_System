import React from "react";
import {
  Layout,
  Menu,
  Button,
  Row,
  Col,
  Typography,
  Avatar,
  Dropdown,
} from "antd";
import { Link, useLocation, useNavigate } from "react-router-dom";
import {
  DashboardOutlined,
  LogoutOutlined,
  UserOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import authApi from "../../api/authApi";
import "./HeaderHome.css";

const { Header: AntHeader } = Layout;
const { Title } = Typography;

const HeaderHome: React.FC = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const user = sessionStorage.getItem("user")
    ? JSON.parse(sessionStorage.getItem("user") as string)
    : null;

  const handleLogout = () => {
    authApi.logout();
    window.location.href = "/login";
  };

  const getDashboardPath = (role: string) => {
    switch (role) {
      case "admin": return "/admin/dashboard";
      case "manager": return "/manager/dashboard";
      case "user": return "/user/dashboard";
      case "driver": return "/driver/dashboard";
      case "shipper": return "/shipper/dashboard";
      default: return "/home";
    }
  };

  const avatarMenu: MenuProps["items"] = [
    {
      key: "dashboard",
      label: (
        <span className="dropdown-item">
          <DashboardOutlined style={{ color: "#1C3D90" }} />
          Trang quản lý
        </span>
      ),
      onClick: () => navigate(getDashboardPath(user.role)),
    },
    { type: "divider" },
    {
      key: "logout",
      label: (
        <span className="dropdown-item">
          <LogoutOutlined />
          Đăng xuất
        </span>
      ),
      onClick: handleLogout,
    },
  ];

  return (
    <AntHeader className="header-home">
      <div className="header-container">
        <Row justify="space-between" align="middle" style={{ height: "100%" }}>
          <Col flex="200px">
            <Title level={2} className="header-logo">
              UTE Logistics
            </Title>
          </Col>

          {/* Menu desktop */}
          <div className="desktop-menu">
            <Menu
              className="custom-menu"
              selectedKeys={[location.pathname]}
              mode="horizontal"
              selectable={false}
              theme="light"
            >
              <Menu.Item key="/"><Link to="/">Trang chủ</Link></Menu.Item>
              <Menu.Item key="/info/services"><Link to="/info/services">Dịch vụ</Link></Menu.Item>
              <Menu.SubMenu key="tracking" title="Tra cứu">
                <Menu.Item key="/tracking/shipping-fee">
                  <Link to="/tracking/shipping-fee">Cước vận chuyển</Link>
                </Menu.Item>
                <Menu.Item key="/tracking/office-search">
                  <Link to="/tracking/office-search">Bưu cục</Link>
                </Menu.Item>
                <Menu.Item key="/tracking/order-tracking">
                  <Link to="/tracking/order-tracking">Vận đơn</Link>
                </Menu.Item>
                <Menu.Item key="/info/shipping-rates">
                  <Link to="/info/shipping-rates">Bảng giá</Link>
                </Menu.Item>
              </Menu.SubMenu>
              <Menu.Item key="/info/promotions"><Link to="/info/promotions">Khuyến mãi</Link></Menu.Item>
              <Menu.Item key="/info/company"><Link to="/info/company">Về chúng tôi</Link></Menu.Item>
              <Menu.Item key="/info/contact"><Link to="/info/contact">Liên hệ</Link></Menu.Item>
            </Menu>
          </div>

          {/* Avatar hoặc nút đăng nhập/đăng ký */}
          <Col className="auth-section">
            {user ? (
              <Dropdown menu={{ items: avatarMenu }} placement="bottomRight" trigger={["click"]}>
                <div className="user-avatar">
                  <Avatar size="default" className="avatar-icon" icon={<UserOutlined />} />
                  <span className="avatar-name">
                    {user?.firstName && user?.lastName
                      ? `${user.firstName} ${user.lastName}`
                      : "User"}
                  </span>
                </div>
              </Dropdown>
            ) : (
              <>
                <Link to="/login">
                  <Button className="btn-login">Đăng nhập</Button>
                </Link>
                <Link to="/register">
                  <Button className="btn-register">Đăng ký</Button>
                </Link>
              </>
            )}
          </Col>
        </Row>
      </div>
    </AntHeader>
  );
};

export default HeaderHome;