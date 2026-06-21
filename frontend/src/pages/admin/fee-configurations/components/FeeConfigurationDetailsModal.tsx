import React from "react";
import {Descriptions, Drawer} from "antd";
import type {FeeConfiguration} from "../../../../types/feeConfiguration";

interface FeeConfigurationDetailsModalProps {
  open: boolean;
  record: FeeConfiguration | null;
  feeTypeLabel: (value: string) => string;
  onClose: () => void;
}

const FeeConfigurationDetailsModal: React.FC<
  FeeConfigurationDetailsModalProps
> = ({ open, record, feeTypeLabel, onClose }) => {
  return (
    <Drawer
      title="Chi tiết cấu hình phí"
      placement="right"
      width={620}
      open={open}
      onClose={onClose}
      destroyOnClose
    >
      {record && (
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Loại phí">
            {feeTypeLabel(record.feeType)}
          </Descriptions.Item>

          <Descriptions.Item label="Trạng thái">
            {record.active ? "Hoạt động" : "Tạm dừng"}
          </Descriptions.Item>

          <Descriptions.Item label="Loại dịch vụ">
            {record.serviceTypeName || "Tất cả"}
          </Descriptions.Item>

          <Descriptions.Item label="Cách tính">
            {record.calculationType === "PERCENTAGE"
              ? "Phần trăm"
              : "Cố định"}
          </Descriptions.Item>

          <Descriptions.Item label="Giá trị phí">
            {record.calculationType === "PERCENTAGE"
              ? `${record.feeValue}%`
              : `${record.feeValue.toLocaleString("vi-VN")}đ`}
          </Descriptions.Item>

          <Descriptions.Item label="Áp dụng từ">
            {record.minOrderFee !== undefined &&
            record.minOrderFee !== null
              ? `${record.minOrderFee.toLocaleString("vi-VN")}đ`
              : "-"}
          </Descriptions.Item>

          <Descriptions.Item label="Áp dụng đến">
            {record.maxOrderFee !== undefined &&
            record.maxOrderFee !== null
              ? `${record.maxOrderFee.toLocaleString("vi-VN")}đ`
              : "-"}
          </Descriptions.Item>

          <Descriptions.Item label="Ghi chú">
            {record.notes || "-"}
          </Descriptions.Item>
        </Descriptions>
      )}
    </Drawer>
  );
};

export default FeeConfigurationDetailsModal;