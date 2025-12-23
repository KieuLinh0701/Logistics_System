import React from "react";
import Title from "antd/es/typography/Title";
import { Row } from "antd";
import { ShoppingOutlined } from "@ant-design/icons";

interface Props {
  trackingNumber?: string;
}

const Header: React.FC<Props> = ({ trackingNumber }) => {
  return (
    <Row className="list-page-header" justify="space-between" align="middle">
      <Title level={3} className="list-page-title-main">
        <ShoppingOutlined className="title-icon" />
        Chỉnh sửa đơn hàng {trackingNumber ? `#${trackingNumber}` : ''}
      </Title>
    </Row>
  );
};

export default Header;