import React from "react";
import {Descriptions, Drawer} from "antd";
import type {JobApplication} from "../../../../../types/recruitment";
import StatusBadge from "./StatusBadge";
import "./ApplicationComponents.css";

interface ApplicationDetailModalProps {
  selected: JobApplication | null;
  onClose: () => void;
}

const ApplicationDetailModal: React.FC<ApplicationDetailModalProps> = ({ selected, onClose }) => {
  return (
    <Drawer className="hr-recruitment-drawer" title="Chi tiết hồ sơ ứng tuyển" open={!!selected} width={560} onClose={onClose}>
      {selected && (
        <Descriptions bordered column={1} size="middle">
          <Descriptions.Item label="Họ tên">{selected.fullName}</Descriptions.Item>
          <Descriptions.Item label="Số điện thoại">{selected.phone}</Descriptions.Item>
          <Descriptions.Item label="Email">{selected.email}</Descriptions.Item>
          <Descriptions.Item label="Địa chỉ">{selected.address}</Descriptions.Item>
          <Descriptions.Item label="Tin tuyển dụng">{selected.jobTitle}</Descriptions.Item>
          <Descriptions.Item label="Bưu cục">{selected.officeName || `#${selected.officeId}`}</Descriptions.Item>
          <Descriptions.Item label="Trạng thái">
            <StatusBadge status={selected.status} />
          </Descriptions.Item>
          <Descriptions.Item label="CV URL">
            <a className="hr-application-detail-link" href={selected.cvUrl} target="_blank" rel="noreferrer">
              {selected.cvUrl}
            </a>
          </Descriptions.Item>
          <Descriptions.Item label="Ngày nộp">{new Date(selected.createdAt).toLocaleString("vi-VN")}</Descriptions.Item>
        </Descriptions>
      )}
    </Drawer>
  );
};

export default ApplicationDetailModal;
