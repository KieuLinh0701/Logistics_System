import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined, PayCircleOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
  onPay: () => void;
}

const Actions: React.FC<Props> = ({ onExport, onPay }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PayCircleOutlined />}
        onClick={onPay}
      >
        Thanh toán
      </Button>
      <Button
        className="success-button"
        icon={<FileExcelOutlined />}
        onClick={onExport}
      >
        Xuất Excel
      </Button>
    </Space>
  );
};

export default Actions;