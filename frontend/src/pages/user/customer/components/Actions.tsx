import React from "react";
import { Space, Button } from "antd";
import { PlusOutlined } from "@ant-design/icons";

interface Props {
  onAdd: () => void;
}

const Actions: React.FC<Props> = ({ onAdd }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAdd}
      >
        Thêm khách hàng mới
      </Button>
    </Space>
  );
};

export default Actions;