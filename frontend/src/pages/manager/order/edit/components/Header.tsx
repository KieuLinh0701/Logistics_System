import React from "react";
import Title from "antd/es/typography/Title";
import { Row } from "antd";
import { ShoppingOutlined } from "@ant-design/icons";
import { translateOrderCreatorType } from "../../../../../utils/orderUtils";

interface Props {
  trackingNumber?: string;
  createdByType?: string;
}

const Header: React.FC<Props> = ({ trackingNumber, createdByType }) => {
  return (
    <Row className="list-page-header" justify="space-between" align="middle">
      <Title level={3} className="list-page-title-main">
        <ShoppingOutlined className="title-icon" />
        Chỉnh sửa đơn hàng {trackingNumber && `#${trackingNumber}`}
        {createdByType && (
          <span className="order-source">
            {" "}– {translateOrderCreatorType(createdByType)}
          </span>
        )}
      </Title>
    </Row>
  );
};

export default Header;