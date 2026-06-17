import React from "react";
import {Button, Input, Select} from "antd";
import {ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import type {JobApplicationStatus} from "../../../../../types/recruitment";
import {applicationStatusOptions} from "../../../../common/recruitment/recruitmentHelpers";
import RecruitmentFilterPanel from "../../components/RecruitmentFilterPanel";
import "../../components/RecruitmentShared.css";

interface SearchFiltersProps {
  status?: JobApplicationStatus;
  onStatusChange: (status?: JobApplicationStatus) => void;
  onRefresh: () => void;
  onSearchChange?: (value: string) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({ status, onStatusChange, onRefresh, onSearchChange }) => {
  return (
    <RecruitmentFilterPanel>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, width: '100%' }}>
        <div style={{ flex: 1, minWidth: 120 }}>
          <Input
            allowClear
            className="search-input"
            placeholder="Tìm kiếm"
            onChange={(e) => onSearchChange && onSearchChange(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: '100%' }}
          />
        </div>

        <div className="hr-recruitment-filter-actions">
          <Select<string | JobApplicationStatus>
            allowClear
            className="hr-recruitment-status-filter"
            placeholder="Lọc theo trạng thái"
            value={status}
            onChange={(value: string | JobApplicationStatus) => onStatusChange(value === "ALL" ? undefined : (value as JobApplicationStatus))}
          >
            <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
            {applicationStatusOptions.map((opt) => (
              <Select.Option key={opt.value} value={opt.value}>
                {opt.label}
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

export default SearchFilters;
