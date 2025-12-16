import React from "react";
import { Space, Button } from "antd";
import { PlusOutlined, PrinterOutlined, TruckOutlined } from "@ant-design/icons";

interface Props {
  onAdd: () => void;
  onPrint: () => void;
  onAddShipment: () => void;
  disabled: boolean;
  recordNumber: number;
}

const Actions: React.FC<Props> = ({ onAdd, onPrint, disabled, recordNumber, onAddShipment }) => {
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
      <Button
        className="warning-button"
        icon={<TruckOutlined />}
        onClick={onAddShipment}
        disabled={!disabled}
      >
        Tạo chuyến giao hàng {recordNumber !== 0 ? `(${recordNumber})` : ""}
      </Button>
    </Space>
  );
};

export default Actions;