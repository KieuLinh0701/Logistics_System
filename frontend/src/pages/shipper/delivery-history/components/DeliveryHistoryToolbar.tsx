import React from "react";
import {Button, DatePicker, Input, Select} from "antd";
import {ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import type dayjs from "dayjs";
import "../../../../styles/ListPage.css";

const { Option } = Select;
const { RangePicker } = DatePicker;

interface FilterParams {
  status?: string;
  dateRange?: [dayjs.Dayjs, dayjs.Dayjs] | null;
  search?: string;
}

interface DeliveryHistoryToolbarProps {
  filters: FilterParams;
  onFilterChange: (key: keyof FilterParams, value: any) => void;
  onReset: () => void;
}

const DeliveryHistoryToolbar: React.FC<DeliveryHistoryToolbarProps> = ({
  filters,
  onFilterChange,
  onReset,
}) => {
  return (
    <div className="shipper-filter-panel">
      <div className="shipper-filter-grow">
        <Input
          allowClear
          className="search-input"
          placeholder="Tìm kiếm theo mã đơn, tên người nhận..."
          prefix={<SearchOutlined />}
          style={{ width: "100%" }}
          value={filters.search}
          onChange={(e) => onFilterChange("search", e.target.value || undefined)}
        />
      </div>
      <div className="shipper-filter-actions">
        <Select
          allowClear
          placeholder="Lọc theo trạng thái"
          style={{ width: 200 }}
          value={filters.status}
          onChange={(value) => onFilterChange("status", value || undefined)}
        >
          <Option value="DELIVERED">Đã giao</Option>
          <Option value="DELIVERY_RETRY">Chờ giao lại</Option>
          <Option value="AT_DEST_OFFICE">Đã nộp về bưu cục</Option>
          <Option value="DELIVERY_FAILED_FINAL">Giao thất bại</Option>
          <Option value="RETURNING">Đang hoàn trả</Option>
          <Option value="RETURN_AT_ORIGIN_OFFICE">Đã hoàn về bưu cục gốc</Option>
          <Option value="RETURN_RETRY">Hoàn lại</Option>
          <Option value="RETURN_FAILED_FINAL">Hoàn thất bại</Option>
          <Option value="RETURNED">Đã hoàn</Option>
        </Select>
        <RangePicker
          value={filters.dateRange ?? null}
          onChange={(dates) => onFilterChange("dateRange", dates)}
        />
        <Button icon={<ReloadOutlined />} onClick={onReset}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default DeliveryHistoryToolbar;
