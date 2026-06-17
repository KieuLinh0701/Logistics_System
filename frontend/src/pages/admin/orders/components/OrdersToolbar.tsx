import React from "react";
import { Button, Input, Select } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";
import RecruitmentFilterPanel from "../../../hr/recruitment/components/RecruitmentFilterPanel";

interface Option {
  label: string;
  value: string;
}

interface OrdersToolbarProps {
  searchValue: string;
  filterStatus?: string;
  statusOptions: Option[];
  onSearchChange: (value: string) => void;
  onStatusChange: (value?: string) => void;
  onRefresh: () => void;
}

const OrdersToolbar: React.FC<OrdersToolbarProps> = ({
  searchValue,
  filterStatus,
  statusOptions,
  onSearchChange,
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
            placeholder="Tìm kiếm mã vận đơn, người gửi, người nhận"
            value={searchValue}
            onChange={(e) => onSearchChange(e.target.value)}
            prefix={<SearchOutlined />}
            style={{ width: "100%" }}
          />
        </div>

        <div className="hr-recruitment-filter-actions orders-toolbar-actions">
          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 190 }}
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

export default OrdersToolbar;
