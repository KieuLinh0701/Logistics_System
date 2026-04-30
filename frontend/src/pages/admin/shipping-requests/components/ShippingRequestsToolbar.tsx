import React from "react";
import { Button, Input, Select } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import RecruitmentFilterPanel from "../../../hr/recruitment/components/RecruitmentFilterPanel";

interface Option {
  label: string;
  value: string;
}

interface ShippingRequestsToolbarProps {
  searchValue: string;
  filterType?: string;
  filterStatus?: string;
  typeOptions: Option[];
  statusOptions: Option[];
  onSearchChange: (value: string) => void;
  onTypeChange: (value?: string) => void;
  onStatusChange: (value?: string) => void;
  onRefresh: () => void;
}

const ShippingRequestsToolbar: React.FC<ShippingRequestsToolbarProps> = ({
  searchValue,
  filterType,
  filterStatus,
  typeOptions,
  statusOptions,
  onSearchChange,
  onTypeChange,
  onStatusChange,
  onRefresh,
}) => {
  return (
    <RecruitmentFilterPanel>
      <div style={{ display: "flex", alignItems: "center", gap: 12, width: "100%" }}>
        <div style={{ flex: 1, minWidth: 220 }}>
          <Input
            allowClear
            className="search-input"
            placeholder="Tìm theo mã yêu cầu, nội dung, người gửi"
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: "100%" }}
          />
        </div>

        <div className="hr-recruitment-filter-actions shipping-requests-toolbar-actions">
          <Select
            allowClear
            placeholder="Loại yêu cầu"
            style={{ width: 180 }}
            value={filterType}
            onChange={onTypeChange}
          >
            {typeOptions.map((option) => (
              <Select.Option key={option.value} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>

          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 170 }}
            value={filterStatus}
            onChange={onStatusChange}
          >
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

export default ShippingRequestsToolbar;
