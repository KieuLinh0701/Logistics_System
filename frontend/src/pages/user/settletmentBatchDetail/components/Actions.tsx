import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined, PayCircleOutlined } from "@ant-design/icons";

interface Props {
  canPay: boolean;
  onExport: () => void;
  onPay: () => void;
}

const Actions: React.FC<Props> = ({ canPay, onExport, onPay }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PayCircleOutlined />}
        disabled={!canPay}
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