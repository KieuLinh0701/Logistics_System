import React from "react";
import Title from "antd/es/typography/Title";
import { ShoppingOutlined } from "@ant-design/icons";

interface Props {
  trackingNumber: string;
}

const Header: React.FC<Props> = ({ trackingNumber }) => (
  <div>
    <Title level={3}>
      <ShoppingOutlined className="title-icon order-detail-icon" />
      <span className="order-detail-title">Chi tiết đơn hàng</span> {trackingNumber !== null && <span className="order-detail-title-tracking-number">#{trackingNumber}</span>}
    </Title>
  </div>
);

export default Header;