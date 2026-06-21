import React from "react";
import {Button, Dropdown} from "antd";
import {DeleteOutlined, DownOutlined, EditOutlined} from "@ant-design/icons";
import type {AdminUser} from "../../../../types/user";

interface UsersActionsProps {
  record: AdminUser;
  loading?: boolean;
  onEdit: (u: AdminUser) => void;
  onDelete: (id: number) => void;
  onViewLogs: (id: number) => void;
}

const UsersActions: React.FC<UsersActionsProps> = ({ record, loading, onEdit, onDelete, onViewLogs }) => {
  const items = [
    { key: 'edit', icon: <EditOutlined />, label: 'Sửa' },
    { key: 'delete', icon: <DeleteOutlined />, label: 'Xóa' },
  ];

  return (
    <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button
            className="action-button-link"
            type="link"
            onClick={() => onViewLogs(record.id)}
        >
            Xem log
        </Button>
      <Dropdown
        menu={{ items, onClick: ({ key }) => {
          if (key === 'edit') onEdit(record);
          if (key === 'delete') onDelete(record.id);
        } }}
        trigger={["click"]}
        placement="bottomRight"
      >
        <button type="button" className="dropdown-trigger-button" disabled={!!loading} style={{ color: '#000', border: '1px solid #000', padding: '6px 10px', borderRadius: 6, display: 'inline-flex', alignItems: 'center', gap: 6, background: 'transparent', opacity: loading ? 0.6 : 1, pointerEvents: loading ? 'none' : undefined }}>
          Thêm <DownOutlined />
        </button>
      </Dropdown>
    </div>
  );
};

export default UsersActions;