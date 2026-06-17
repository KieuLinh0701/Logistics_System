import React from "react";
import { Dropdown } from "antd";
import { DownOutlined, CheckOutlined, CloseOutlined } from "@ant-design/icons";
import type { JobApplication, JobApplicationStatus } from "../../../../../types/recruitment";
import "./ApplicationComponents.css";

interface ActionsProps {
  record: JobApplication;
  onView: (application: JobApplication) => void;
  onReviewing: (id: number) => void;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
}

const canMoveToReviewing = (status: JobApplicationStatus) => status === "PENDING";
const canApproveOrReject = (status: JobApplicationStatus) => status === "REVIEWING";

const Actions: React.FC<ActionsProps> = ({ record, onView, onReviewing, onApprove, onReject }) => {
  const items = [
    {
      key: "review",
      icon: <DownOutlined />,
      label: "Chuyển sang Đang xem xét",
      disabled: !canMoveToReviewing(record.status),
    },
    {
      key: "approve",
      icon: <CheckOutlined />,
      label: "Duyệt",
      disabled: !canApproveOrReject(record.status),
    },
    {
      key: "reject",
      icon: <CloseOutlined />,
      label: "Từ chối",
      disabled: !canApproveOrReject(record.status),
    },
  ];

  return (
    <div className="hr-application-actions" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <span
        onClick={() => onView(record)}
        style={{ color: '#52c41a', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: 6 }}
        title="Xem"
      >
        <span style={{ color: '#52c41a' }}>Xem</span>
      </span>

      <Dropdown
        menu={{ items, onClick: ({ key }) => {
          if (key === "review") onReviewing(record.id);
          if (key === "approve") onApprove(record.id);
          if (key === "reject") onReject(record.id);
        } }}
        trigger={["click"]}
        placement="bottomRight"
      >
        <button type="button" className="dropdown-trigger-button" style={{ color: '#000', border: '1px solid #000', padding: '6px 10px', borderRadius: 6, display: 'inline-flex', alignItems: 'center', gap: 6, background: 'transparent' }}>
          Thêm <DownOutlined />
        </button>
      </Dropdown>
    </div>
  );
};

export default Actions;
