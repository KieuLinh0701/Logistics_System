import React from "react";
import {Descriptions, Modal} from "antd";
import type {ShippingRequestRow} from "../../../../types/shippingRequest";

interface ShippingRequestDetailsModalProps {
  open: boolean;
  request: ShippingRequestRow | null;
  typeText: (value?: string) => string;
  statusText: (value?: string) => string;
  onClose: () => void;
}

const ShippingRequestDetailsModal: React.FC<ShippingRequestDetailsModalProps> = ({
  open,
  request,
  typeText,
  statusText,
  onClose,
}) => {
  return (
    <Modal title="Chi tiet yeu cau" open={open} onCancel={onClose} footer={null} width={860}>
      {request && (
        <Descriptions column={2} bordered>
          <Descriptions.Item label="Ma yeu cau">{request.code}</Descriptions.Item>
          <Descriptions.Item label="Trang thai">{statusText(request.status)}</Descriptions.Item>
          <Descriptions.Item label="Loai yeu cau">{typeText(request.requestType)}</Descriptions.Item>
          <Descriptions.Item label="Buu cuc">{request.office?.name || "-"}</Descriptions.Item>
          <Descriptions.Item label="Nguoi gui">{request.userName || "Khach"}</Descriptions.Item>
          <Descriptions.Item label="Ngay tao">{request.createdAt ? new Date(request.createdAt).toLocaleString() : "-"}</Descriptions.Item>
          <Descriptions.Item label="Noi dung" span={2}>{request.content || "-"}</Descriptions.Item>
          <Descriptions.Item label="Nguoi lien he">{request.contactName || "-"}</Descriptions.Item>
          <Descriptions.Item label="Dien thoai">{request.contactPhoneNumber || "-"}</Descriptions.Item>
          <Descriptions.Item label="Email">{request.contactEmail || "-"}</Descriptions.Item>
          <Descriptions.Item label="Ma van don">{request.orderTrackingNumber || "-"}</Descriptions.Item>
          <Descriptions.Item label="Phan hoi" span={2}>{request.response || "-"}</Descriptions.Item>
          <Descriptions.Item label="Thoi gian phan hoi" span={2}>{request.responseAt ? new Date(request.responseAt).toLocaleString() : "-"}</Descriptions.Item>
        </Descriptions>
      )}
    </Modal>
  );
};

export default ShippingRequestDetailsModal;
