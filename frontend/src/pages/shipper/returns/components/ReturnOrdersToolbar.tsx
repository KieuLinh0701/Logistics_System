import React from "react";
import { Button, Input, Select } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";

const { Option } = Select;

interface ReturnOrdersToolbarProps {
  search?: string;
  status?: string;
  onSearchChange: (value: string | undefined) => void;
  onStatusChange: (value: string | undefined) => void;
  onRefresh: () => void;
}

const ReturnOrdersToolbar: React.FC<ReturnOrdersToolbarProps> = ({
  search,
  status,
  onSearchChange,
  onStatusChange,
  onRefresh,
}) => {
  return (
    <div className="shipper-filter-panel">
      <div className="shipper-filter-grow">
        <Input
          allowClear
          className="search-input"
          placeholder="Tìm theo mã đơn, người gửi, người nhận, SĐT"
          prefix={<SearchOutlined />}
          value={search}
          onChange={(e) => onSearchChange(e.target.value || undefined)}
          style={{ width: "100%" }}
        />
      </div>
      <div className="shipper-filter-actions">
        <Select
          allowClear
          placeholder="Trạng thái"
          style={{ width: 180 }}
          value={status}
          onChange={onStatusChange}
        >
          <Option value="RETURN_AT_ORIGIN_OFFICE">Chờ nhận hoàn trả</Option>
          <Option value="RETURNING">Đang hoàn trả</Option>
          <Option value="RETURN_RETRY">Hoàn lại</Option>
        </Select>
        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default ReturnOrdersToolbar;
