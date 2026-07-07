import React from "react";
import {Button, Space} from "antd";
import {PlayCircleOutlined} from "@ant-design/icons";
import type {DriverShipment} from "../../../../types/shipment";

interface ShipmentActionsProps {
  record: DriverShipment;
  onStart: (shipmentId: number) => void;
  onFinish: (shipmentId: number, status: "COMPLETED" | "CANCELLED") => void;
}

const ShipmentActions: React.FC<ShipmentActionsProps> = ({ record, onStart, onFinish }) => {
  if (record.status === "PENDING") {
    return (
      <Button
        style={{ marginTop: 8 }}
        type="default"
        icon={<PlayCircleOutlined />}
        onClick={() => onStart(record.id)}
        block
      >
        Bắt đầu
      </Button>
    );
  }

  if (record.status === "IN_TRANSIT") {
    return (
      <Space direction="vertical" style={{ width: "100%" }}>
        <Button
          type="primary"
          onClick={() => onFinish(record.id, "COMPLETED")}
          block
        >
          Hoàn tất
        </Button>
        <Button
          danger
          onClick={() => onFinish(record.id, "CANCELLED")}
          block
        >
          Hủy
        </Button>
      </Space>
    );
  }

  return null;
};

export default ShipmentActions;
