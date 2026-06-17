import React from "react";
import { Space, Button } from "antd";
import {FileExcelOutlined, PlusOutlined, PrinterOutlined, TruckOutlined} from "@ant-design/icons";

interface Props {
  onAdd: () => void;
  onPrint: () => void;
  onAddShipment: () => void;
  onExport: () => void;
  disabled: boolean;
  recordNumber: number;
  total: number;
}

const Actions: React.FC<Props> = ({ onAdd, onPrint, disabled, recordNumber, onAddShipment, onExport, total }) => {
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
        Thêm vào chuyến {recordNumber !== 0 ? `(${recordNumber})` : ""}
      </Button>
        <Button
            className="success-button"
            icon={<FileExcelOutlined />}
            onClick={onExport}
            disabled={total === 0}
        >
            Xuất Excel
        </Button>
    </Space>
  );
};

export default Actions;