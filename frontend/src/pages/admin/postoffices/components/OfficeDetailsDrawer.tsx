import React from "react";
import { Descriptions, Drawer, Tag } from "antd";
import type { AdminOffice } from "../../../../types/office";

interface Option {
  label: string;
  value: string;
}

interface OfficeDetailsDrawerProps {
  open: boolean;
  selectedOffice: (AdminOffice & { displayAddress?: string }) | null;
  officeTypeOptions: Option[];
  officeStatusOptions: Option[];
  onClose: () => void;
}

const OfficeDetailsDrawer: React.FC<OfficeDetailsDrawerProps> = ({
  open,
  selectedOffice,
  officeTypeOptions,
  officeStatusOptions,
  onClose,
}) => {
  const formatAddress = (office: AdminOffice) => {
    if (!office.detail && !office.wardCode && !office.cityCode) return "Chưa có";
    const parts: string[] = [];
    if (office.detail) parts.push(office.detail);
    if (office.wardCode) parts.push(`Phường ${office.wardCode}`);
    if (office.cityCode) parts.push(`TP ${office.cityCode}`);
    return parts.join(" - ");
  };

  return (
    <Drawer title="Chi tiết bưu cục" placement="right" width={600} open={open} onClose={onClose}>
      {selectedOffice && (
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Mã">{selectedOffice.code}</Descriptions.Item>
          <Descriptions.Item label="Tên">{selectedOffice.name}</Descriptions.Item>
          <Descriptions.Item label="Email">{selectedOffice.email || "N/A"}</Descriptions.Item>
          <Descriptions.Item label="SĐT">{selectedOffice.phoneNumber || "N/A"}</Descriptions.Item>
          <Descriptions.Item label="Loại">
            {officeTypeOptions.find((item) => item.value === selectedOffice.type)?.label || selectedOffice.type || "N/A"}
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <Tag color={selectedOffice.status === "ACTIVE" ? "green" : "default"}>
              {officeStatusOptions.find((item) => item.value === selectedOffice.status)?.label || selectedOffice.status || "N/A"}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="Địa chỉ">{selectedOffice.displayAddress || formatAddress(selectedOffice)}</Descriptions.Item>
          <Descriptions.Item label="Tọa độ">
            {selectedOffice.latitude}, {selectedOffice.longitude}
          </Descriptions.Item>
          <Descriptions.Item label="Giờ hoạt động">
            {selectedOffice.openingTime} - {selectedOffice.closingTime}
          </Descriptions.Item>
          <Descriptions.Item label="Sức chứa">{selectedOffice.capacity || "N/A"}</Descriptions.Item>
          {selectedOffice.notes && <Descriptions.Item label="Ghi chú">{selectedOffice.notes}</Descriptions.Item>}
        </Descriptions>
      )}
    </Drawer>
  );
};

export default OfficeDetailsDrawer;
