import React from "react";
import { Button, Input, Select } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import RecruitmentFilterPanel from "../../../hr/recruitment/components/RecruitmentFilterPanel";

interface Option {
  label: string;
  value: string;
}

interface ServiceTypesToolbarProps {
  searchValue: string;
  filterStatus?: string;
  statusOptions: Option[];
  onSearchChange: (value: string) => void;
  onStatusChange: (value?: string) => void;
  onRefresh: () => void;
}

const ServiceTypesToolbar: React.FC<ServiceTypesToolbarProps> = ({
  searchValue,
  filterStatus,
  statusOptions,
  onSearchChange,
  onStatusChange,
  onRefresh,
}) => {
  return (
    <RecruitmentFilterPanel>
      <div className="service-types-toolbar-row" style={{ display: "flex", alignItems: "center", gap: 12, width: "100%" }}>
        <div style={{ flex: 1, minWidth: 220 }}>
          <Input
            allowClear
            className="search-input"
            placeholder="Tìm kiếm"
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: "100%" }}
          />
        </div>

        <div className="hr-recruitment-filter-actions service-types-toolbar-actions">
          <Select allowClear placeholder="Trạng thái" style={{ width: 170 }} value={filterStatus} onChange={onStatusChange}>
            {statusOptions.map((option) => (
              <Select.Option key={option.value} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>

          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            Làm mới
          </Button>
        </div>
      </div>
    </RecruitmentFilterPanel>
  );
};

export default ServiceTypesToolbar;
