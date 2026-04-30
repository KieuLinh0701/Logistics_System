import React from "react";
import { Button, Input, Select } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import RecruitmentFilterPanel from "../../../hr/recruitment/components/RecruitmentFilterPanel";
import type { Option } from "./types";

interface FeeConfigurationsToolbarProps {
  search: string;
  feeType?: string;
  active?: boolean;
  feeTypeOptions: Option[];
  onSearchChange: (value: string) => void;
  onFeeTypeChange: (value?: string) => void;
  onActiveChange: (value?: boolean) => void;
  onRefresh: () => void;
}

const FeeConfigurationsToolbar: React.FC<FeeConfigurationsToolbarProps> = ({
  search,
  feeType,
  active,
  feeTypeOptions,
  onSearchChange,
  onFeeTypeChange,
  onActiveChange,
  onRefresh,
}) => {
  return (
    <RecruitmentFilterPanel>
      <div style={{ display: "flex", alignItems: "center", gap: 12, width: "100%" }}>
        <div style={{ flex: 1, minWidth: 220 }}>
          <Input
            allowClear
            className="search-input"
            placeholder="Tim theo loai phi, ghi chu..."
            value={search}
            prefix={<SearchOutlined />}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>

        <div className="hr-recruitment-filter-actions fee-configurations-toolbar-actions">
          <Select
            allowClear
            placeholder="Loai phi"
            style={{ width: 170 }}
            value={feeType}
            onChange={onFeeTypeChange}
          >
            {feeTypeOptions.map((option) => (
              <Select.Option key={option.value} value={option.value}>
                {option.label}
              </Select.Option>
            ))}
          </Select>

          <Select
            allowClear
            placeholder="Trang thai"
            style={{ width: 150 }}
            value={active}
            onChange={onActiveChange}
          >
            <Select.Option value={true}>Hoat dong</Select.Option>
            <Select.Option value={false}>Tam dung</Select.Option>
          </Select>

          <Button icon={<ReloadOutlined />} onClick={onRefresh}>
            Lam moi
          </Button>
        </div>
      </div>
    </RecruitmentFilterPanel>
  );
};

export default FeeConfigurationsToolbar;
