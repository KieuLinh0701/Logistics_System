import React from "react";
import {Button, Input} from "antd";
import {ReloadOutlined, SearchOutlined} from "@ant-design/icons";

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
    <div className="failed-deliveries-toolbar">
      <div className="failed-deliveries-search-wrapper">
        <Input
          allowClear
          placeholder="Tìm theo mã đơn, người nhận, SĐT"
          prefix={<SearchOutlined />}
          value={search}
          onChange={(e) => onSearchChange(e.target.value || undefined)}
        />
      </div>
      <div className="failed-deliveries-toolbar-actions">
        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default FailedDeliveriesToolbar;
