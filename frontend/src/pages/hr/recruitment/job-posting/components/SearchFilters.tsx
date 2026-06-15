import React from "react";
import {Button, Input, Select} from "antd";
import {PlusOutlined, ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import type {JobPostingStatus} from "../../../../../types/recruitment";
import {jobStatusOptions} from "../../../../common/recruitment/recruitmentHelpers";
import RecruitmentFilterPanel from "../../components/RecruitmentFilterPanel";
import "../../components/RecruitmentShared.css";
import "./JobPostingComponents.css";

interface SearchFiltersProps {
  status?: JobPostingStatus;
  onStatusChange: (status?: JobPostingStatus) => void;
  onRefresh: () => void;
  onCreate: () => void;
  onSearchChange?: (value: string) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({ status, onStatusChange, onRefresh, onCreate, onSearchChange }) => {
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
          <Select
            allowClear
            className="hr-job-posting-status-filter"
            placeholder="Lọc theo trạng thái"
            options={jobStatusOptions}
            value={status}
            onChange={(value) => onStatusChange(value)}
          />
          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            Làm mới
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
            Tạo tin tuyển dụng
          </Button>
        </div>
      </div>
    </RecruitmentFilterPanel>
  );
};

export default SearchFilters;
