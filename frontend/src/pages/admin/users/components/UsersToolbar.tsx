import React, { useEffect, useState } from "react";
import { Button, Input, Select } from "antd";
import { PlusOutlined, ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import RecruitmentFilterPanel from "../../../hr/recruitment/components/RecruitmentFilterPanel";
import "../../../hr/recruitment/components/RecruitmentShared.css";
import { translateRoleName } from "../../../../utils/roleUtils";

interface UsersToolbarProps {
  onSearchChange?: (q?: string) => void;
  onRefresh: () => void;
  onCreate?: () => void;
  onStatusChange?: (s?: string) => void;
  onRoleChange?: (r?: number) => void;
  searchValue?: string | undefined;
  statusValue?: string | undefined;
  roleValue?: number | undefined;
  roles?: Array<{ id: number; name: string }>;
}

const UsersToolbar: React.FC<UsersToolbarProps> = ({ onSearchChange, onRefresh, onCreate, onStatusChange, onRoleChange, searchValue, statusValue, roleValue, roles }) => {
  const [value, setValue] = useState<string>("");

  useEffect(() => {
    const t = setTimeout(() => onSearchChange && onSearchChange(value || undefined), 300);
    return () => clearTimeout(t);
  }, [value]);

  React.useEffect(() => {
    setValue(searchValue || "");
  }, [searchValue]);

  return (
    <RecruitmentFilterPanel>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
        <div style={{ flex: 1, minWidth: 120 }}>
          <Input
            allowClear
            className="search-input"
            placeholder="Tìm kiếm"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: '100%' }}
          />
        </div>

        <div className="hr-recruitment-filter-actions">
          <Select allowClear className="hr-job-posting-status-filter" placeholder="Trạng thái" onChange={(v) => onStatusChange && onStatusChange(v)} style={{ width: 150 }} dropdownMatchSelectWidth={150} value={statusValue}>
            <Select.Option value="ACTIVE">Hoạt động</Select.Option>
            <Select.Option value="INACTIVE">Khóa</Select.Option>
          </Select>
          <Select
            allowClear
            className="hr-job-posting-role-filter"
            placeholder={roles && roles.length ? "Vai trò" : "Danh sách vai trò chưa có"}
            onChange={(v) => onRoleChange && onRoleChange(v === undefined ? undefined : Number(v))}
            style={{ width: 240 }}
            dropdownMatchSelectWidth={240}
            value={roleValue}
            disabled={!(roles && roles.length)}
          >
            {roles && roles.length && roles.map(r => (
              <Select.Option key={r.id} value={r.id}>{translateRoleName(r.name)}</Select.Option>
            ))}
          </Select>
          <Button icon={<ReloadOutlined />} onClick={onRefresh}>Làm mới</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>Thêm người dùng</Button>
        </div>
      </div>
    </RecruitmentFilterPanel>
  );
};

export default UsersToolbar;
