import React from "react";
import { Button, Input, Select } from "antd";
import { ReloadOutlined, SearchOutlined } from "@ant-design/icons";

const { Option } = Select;

export type TabKey = "delivery" | "return" | "pickup";

interface MyOrdersToolbarProps {
  activeTab: TabKey;
  search: string;
  status: string | undefined;
  onSearchChange: (value: string) => void;
  onStatusChange: (value: string | undefined) => void;
  onRefresh: () => void;
}

const MyOrdersToolbar: React.FC<MyOrdersToolbarProps> = ({
  activeTab,
  search,
  status,
  onSearchChange,
  onStatusChange,
  onRefresh,
}) => {
  const getStatusOptions = () => {
    switch (activeTab) {
      case "delivery":
        return (
          <>
            <Option value="PICKED_UP">Đã lấy hàng</Option>
            <Option value="DELIVERING">Đang giao</Option>
            <Option value="DELIVERED">Đã giao</Option>
            <Option value="FAILED_DELIVERY">Giao thất bại</Option>
          </>
        );
      case "return":
        return (
          <>
            <Option value="RETURN_AT_ORIGIN_OFFICE">Chờ nhận hoàn trả</Option>
            <Option value="RETURNING">Đang hoàn trả</Option>
            <Option value="RETURN_RETRY">Hoàn lại</Option>
          </>
        );
      case "pickup":
      default:
        return null;
    }
  };

  return (
    <div className="my-orders-toolbar">
      <div className="my-orders-search-wrapper">
        <Input
          allowClear
          className="my-orders-search"
          placeholder="Tìm theo mã đơn, người nhận, SĐT"
          prefix={<SearchOutlined />}
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
        />
      </div>

      <div className="my-orders-toolbar-actions">
        {activeTab !== "pickup" && (
          <Select
            allowClear
            placeholder="Trạng thái"
            style={{ width: 180 }}
            value={status}
            onChange={onStatusChange}
          >
            {getStatusOptions()}
          </Select>
        )}

        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default MyOrdersToolbar;
