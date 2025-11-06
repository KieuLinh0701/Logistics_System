import React, { useState } from "react";
import { Layout, Button } from "antd";
import {
  MenuUnfoldOutlined,
  MenuFoldOutlined,
} from "@ant-design/icons";
import { Outlet } from "react-router-dom";
import Header from "../components/common/Header";
import Sidenav from "../components/common/sidenav/Sidenav";

const { Header: AntHeader, Sider, Content } = Layout;

const DashboardLayout: React.FC = () => {
  const HEADER_HEIGHT = 64;
  const GAP = 8;

  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout style={{ minHeight: "100vh", background: "#f0f2f5" }}>
      {/* Header cố định */}
      <AntHeader
        style={{
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          zIndex: 1000,
          background: "#fff",
          height: HEADER_HEIGHT,
          boxShadow: "0 2px 8px #f0f1f2",
          padding: 0,
        }}
      >
        <Header />
      </AntHeader>

      {/* Layout chính */}
      <Layout style={{ padding: GAP, marginTop: HEADER_HEIGHT }}>
        <Sider
          width={240}
          collapsible
          collapsed={collapsed}
          collapsedWidth={60}
          trigger={null}
          breakpoint="lg" 
          onBreakpoint={(broken) => setCollapsed(broken)} 
          style={{
            position: "fixed",
            top: HEADER_HEIGHT + GAP,
            left: GAP,
            height: `calc(100vh - ${HEADER_HEIGHT + GAP * 2}px)`,
            background: "#fff",
            borderRadius: 6,
            overflow: "auto",
          }}
        >
          <div style={{ paddingBottom: 40 }}>
            <Sidenav color="#fff" />
          </div>

          {/* Nút toggle cố định đáy */}
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{
              position: "absolute",
              bottom: 0,
              left: 0,
              width: "100%",
              textAlign: "center",
              background: "#fff",
              borderTop: "1px solid #f0f0f0",
              borderRadius: 0,
            }}
          />
        </Sider>

        {/* Nội dung */}
        <Layout
          style={{
            marginLeft: collapsed ? 60 + GAP : 220 + GAP,
            background: "#fff",
            padding: GAP,
            borderRadius: 6,
            flex: 1,
            transition: "all 0.2s",
          }}
        >
          <Content
            style={{
              background: "#fff",
              padding: GAP,
              borderRadius: 6,
              flex: 1,
              overflowY: "auto",
            }}
          >
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  );
};

export default DashboardLayout;