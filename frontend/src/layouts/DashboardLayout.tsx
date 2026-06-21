import React, {useState} from "react";
import {Button, Layout} from "antd";
import {MenuFoldOutlined, MenuUnfoldOutlined,} from "@ant-design/icons";
import {Outlet} from "react-router-dom";
import Header from "../components/common/Header";
import SideNav from "../components/common/SideNav.tsx";
import "./DashboardLayout.css";

const { Header: AntHeader, Sider, Content } = Layout;

const DashboardLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout className="dashboard-layout">

      <AntHeader className="fixed-header">
        <Header />
      </AntHeader>

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
          style={{
            overflow: 'hidden',
            position: 'fixed',
            height: 'calc(100vh - 80px)'
          }}
        >
          <div className="sidenav-container">
            <SideNav/>
          </div>

        </Sider>

        <div 
          style={{
            position: 'fixed',
            bottom: '8px',
            left: '8px',
            width: collapsed ? '60px' : '240px',
            transition: 'all 0.2s',
            zIndex: 1000
          }}
        >
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            className="toggle-button"
            style={{
              width: '100%',
              borderRadius: collapsed ? '6px' : '0 0 6px 6px'
            }}
          />
        </div>

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