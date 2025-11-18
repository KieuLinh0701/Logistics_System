import React, { useState } from "react";
import { Layout, Button } from "antd";
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
} from "@ant-design/icons";
import { Outlet } from "react-router-dom";
import Header from "../components/common/Header";
import Sidenav from "../components/common/Sidenav";
import "./DashboardLayout.css";

const { Header: AntHeader, Sider, Content } = Layout;

const DashboardLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout className="dashboard-layout">
      {/* Header cố định */}
      <AntHeader className="fixed-header">
        <Header />
      </AntHeader>

      {/* Layout chính */}
      <Layout className="main-content-layout">
        <Sider
          width={240}
          collapsible
          collapsed={collapsed}
          collapsedWidth={60}
          trigger={null}
          breakpoint="lg" 
          onBreakpoint={(broken) => setCollapsed(broken)} 
          className="sider"
        >
          <div className="sidenav-container">
            <Sidenav color="#fff" />
          </div>

          {/* Nút toggle cố định đáy */}
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="toggle-button"
          />
        </Sider>

        {/* Nội dung */}
        <Layout className={`content-wrapper ${collapsed ? 'collapsed' : 'expanded'}`}>
          <Content className="content-area">
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
};

export default DashboardLayout;