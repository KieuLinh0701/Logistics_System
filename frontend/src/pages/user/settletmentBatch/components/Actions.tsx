import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined, PayCircleOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
  onPayment: () => void;
  countIds: number;
  totalAmount: number;
}

const Actions: React.FC<Props> = ({ onExport, onPayment, countIds, totalAmount }) => {
  const formattedAmount = totalAmount.toLocaleString("vi-VN") + "₫";

  return (
    <Space align="center">
      <Button
        className="warning-button"
        icon={<PayCircleOutlined />}
        onClick={onPayment}
      >
        Thanh toán {countIds ? `(${countIds})` : ""} {totalAmount > 0 ? `~ ${formattedAmount}` : ""}
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