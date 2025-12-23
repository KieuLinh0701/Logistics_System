import React from "react";
import { Space, Button } from "antd";
import { PlusOutlined, PrinterOutlined, TruckOutlined } from "@ant-design/icons";

interface Props {
  onAdd: () => void;
  onPrint: () => void;
  disabled: boolean;
  recordNumber: number;
}

const Actions: React.FC<Props> = ({ onAdd, onPrint, disabled, recordNumber }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAdd}
        disabled={disabled}
      >
        Tạo đơn hàng
      </Button>
      <Button
        className="success-button"
        icon={<PrinterOutlined />}
        onClick={onPrint}
        disabled={!disabled}>
        In phiếu {recordNumber !== 0 ? `(${recordNumber})` : ""}
      </Button>
    </Space>
  );
};

export default Actions;