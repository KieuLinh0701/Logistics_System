import React from "react";
import Title from "antd/es/typography/Title";
import { Row } from "antd";
import { ShoppingOutlined } from "@ant-design/icons";

const Header: React.FC = () => (
  <Row className="list-page-header" justify="space-between" align="middle">
    <Title level={3} className="list-page-title-main">
      <ShoppingOutlined className="title-icon" />
      Tạo đơn hàng mới
    </Title>
  </Row>
);

export default Header;