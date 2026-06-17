import React from "react";
import {Dropdown, Menu} from "antd";
import {CloseOutlined, DeleteOutlined, DownOutlined, EditOutlined} from "@ant-design/icons";
import type {JobPosting, JobPostingStatus} from "../../../../../types/recruitment";
import "./JobPostingComponents.css";

interface ActionsProps {
  record: JobPosting;
  onEdit: (job: JobPosting) => void;
  onToggleStatus: (job: JobPosting, nextStatus: JobPostingStatus) => void;
  onDelete: (id: number) => void;
}

const Actions: React.FC<ActionsProps> = ({ record, onEdit, onToggleStatus, onDelete }) => {
  const nextStatus: JobPostingStatus = record.status === "OPEN" ? "CLOSED" : "OPEN";

  const menu = (
    <Menu
      className="action-dropdown-menu"
      onClick={({ key }) => {
        if (key === "edit") onEdit(record);
        if (key === "toggle") onToggleStatus(record, nextStatus);
        if (key === "delete") {
          const ok = window.confirm("Xóa tin tuyển dụng này?");
          if (ok) onDelete(record.id);
        }
      }}
    >
      <Menu.Item key="edit" icon={<EditOutlined />}>
        Sửa
      </Menu.Item>
      <Menu.Item key="toggle" icon={<CloseOutlined />}>
        {record.status === "OPEN" ? "Đóng" : "Mở"}
      </Menu.Item>
      <Menu.Item key="delete" icon={<DeleteOutlined />}>
        Xóa
      </Menu.Item>
    </Menu>
  );

  return (
    <div className="hr-job-posting-actions" style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
      <Dropdown overlay={menu} trigger={["click"]} placement="bottomRight">
        <a className="dropdown-trigger-button" style={{ color: '#000', border: '1px solid #000', padding: '6px 10px', borderRadius: 6, display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          Thêm <DownOutlined />
        </a>
      </Dropdown>
    </div>
  );
};

export default Actions;
