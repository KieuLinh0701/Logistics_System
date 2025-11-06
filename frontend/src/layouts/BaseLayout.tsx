import React, { useState } from "react";
import { Layout, Button } from "antd";
import { MenuUnfoldOutlined, MenuFoldOutlined } from "@ant-design/icons";
import { Outlet } from "react-router-dom";

const { Header: AntHeader, Sider, Content } = Layout;

interface BaseLayoutProps {
  header?: React.ReactNode;
  sidenav?: React.ReactNode;
  backgroundColor?: string;
}

const BaseLayout: React.FC<BaseLayoutProps> = ({
  header,
  sidenav,
  backgroundColor = "#f0f2f5",
}) => {
  const HEADER_HEIGHT = 64;
  const GAP = 8;
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout style={{ minHeight: "100vh", background: backgroundColor }}>
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
        {header}
      </AntHeader>

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
          {sidenav}
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

export default BaseLayout;