import React from "react";
import { Button, Input } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";

interface FailedDeliveriesToolbarProps {
  search?: string;
  onSearchChange: (value: string | undefined) => void;
  onRefresh: () => void;
}

const FailedDeliveriesToolbar: React.FC<FailedDeliveriesToolbarProps> = ({
  search,
  onSearchChange,
  onRefresh,
}) => {
  return (
    <div className="shipper-filter-panel">
      <div className="shipper-filter-grow">
        <Input
          allowClear
          className="search-input"
          placeholder="Tìm theo mã đơn, người nhận, SĐT"
          prefix={<SearchOutlined />}
          value={search}
          onChange={(e) => onSearchChange(e.target.value || undefined)}
          style={{ width: "100%" }}
        />
      </div>
      <div className="shipper-filter-actions">
        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default FailedDeliveriesToolbar;
