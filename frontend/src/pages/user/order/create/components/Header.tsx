import React from "react";
import Title from "antd/es/typography/Title";

const Header: React.FC = () => (
  <div>
    <Title level={3} className="title" style={{ marginBottom: 20}} >
      Tạo đơn hàng mới
    </Title>
  </div>
);

export default Header;