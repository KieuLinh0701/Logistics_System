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
  BarChartOutlined,
  ClockCircleOutlined,
  DashboardOutlined,
  DollarOutlined,
  HomeOutlined,
  InboxOutlined,
  LogoutOutlined,
  RocketOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from "@ant-design/icons";
import type { MenuProps } from "antd";
import authApi from "../../api/authApi";
import "./HeaderHome.css";
import logo from "../../assets/images/home/logo.png";

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

  const menuItems: MenuProps["items"] = [
    { key: "/", label: <Link to="/">Trang chủ</Link> },
    {
      key: "/info/services",
      label: "Dịch vụ",
      children: [
        {
          key: "/info/services/standard",
          label: <Link to="/info/services/standard">Giao hàng tiêu chuẩn</Link>,
          icon: <ClockCircleOutlined />,
        },
        {
          key: "/info/services/express",
          label: <Link to="/info/services/express">Giao hàng nhanh</Link>,
          icon: <RocketOutlined />,
        },
        {
          key: "/info/services/flash",
          label: <Link to="/info/services/flash">Hỏa tốc</Link>,
          icon: <ThunderboltOutlined />,
        },
      ],
    },
    {
      key: "tracking",
      label: "Tra cứu",
      children: [
        {
          key: "/tracking/shipping-fee",
          label: <Link to="/tracking/shipping-fee">Cước vận chuyển</Link>,
          icon: <DollarOutlined />,
        },
        {
          key: "/tracking/office-search",
          label: <Link to="/tracking/office-search">Bưu cục</Link>,
          icon: <HomeOutlined />,
        },
        {
          key: "/tracking/order-tracking",
          label: <Link to="/tracking/order-tracking">Vận đơn</Link>,
          icon: <InboxOutlined />,
        },
        {
          key: "/info/shipping-rates",
          label: <Link to="/info/shipping-rates">Bảng giá</Link>,
          icon: <BarChartOutlined />,
        },
      ],
    },
    { key: "/info/promotions", label: <Link to="/info/promotions">Khuyến mãi</Link> },
    { key: "/info/company", label: <Link to="/info/company">Về chúng tôi</Link> },
    { key: "/info/contact", label: <Link to="/info/contact">Liên hệ</Link> },
  ];

  return (
    <AntHeader className="header-home">
      <div className="header-container">
        <Row justify="space-between" align="middle" style={{ height: "100%" }}>
          <Col className="logo-section">
            <div className="logo-container">
              <img src={logo} alt="UTE Logistics" className="header-logo-image" />
              <Title level={2} className="header-logo-text">
                UTE Logistics
              </Title>
            </div>
          </Col>

          {/* Menu desktop */}
          <div className="desktop-menu">
            <Menu
              className="custom-menu"
              selectedKeys={[location.pathname]}
              mode="horizontal"
              selectable={false}
              theme="light"
              items={menuItems}
            />
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