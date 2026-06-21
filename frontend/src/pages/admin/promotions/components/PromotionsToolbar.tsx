import React from "react";
import {Button, Input, Select} from "antd";
import {ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import RecruitmentFilterPanel from "../../../hr/recruitment/components/RecruitmentFilterPanel";

interface Option {
  label: string;
  value: string;
}

interface PromotionsToolbarProps {
  searchValue: string;
  filterStatus?: string;
  filterIsGlobal?: boolean;
  statusOptions: Option[];
  onSearchChange: (value: string) => void;
  onStatusChange: (value?: string) => void;
  onIsGlobalChange: (value?: boolean) => void;
  onRefresh: () => void;
}

const PromotionsToolbar: React.FC<PromotionsToolbarProps> = ({
  searchValue,
  filterStatus,
  filterIsGlobal,
  statusOptions,
  onSearchChange,
  onStatusChange,
  onIsGlobalChange,
  onRefresh,
}) => {
  return (
    <RecruitmentFilterPanel>
      <div style={{ display: "flex", alignItems: "center", gap: 12, width: "100%" }}>
        <div style={{ flex: 1, minWidth: 220 }}>
          <Input
            allowClear
            className="search-input"
            placeholder="Tìm kiếm mã hoặc tiêu đề"
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: "100%" }}
          />
        </div>

        <div className="hr-recruitment-filter-actions promotions-toolbar-actions">
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

          <Select
            allowClear
            placeholder="Loại khuyến mãi"
            style={{ width: 180 }}
            value={filterIsGlobal}
            onChange={onIsGlobalChange}
          >
            <Select.Option value={true}>Chung/Điều kiện</Select.Option>
            <Select.Option value={false}>Theo user</Select.Option>
          </Select>

          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            Làm mới
          </Button>
        </div>
      </div>
    </RecruitmentFilterPanel>
  );
};

export default PromotionsToolbar;
