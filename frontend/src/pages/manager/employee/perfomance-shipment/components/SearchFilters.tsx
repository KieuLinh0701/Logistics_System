import React, { useState } from "react";
import { Row, Col, Button, Select, DatePicker, Input } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { SHIPMENT_FILTER_SORT, SHIPMENT_STATUSES, translateShipmentFilterSort, translateShipmentStatus } from "../../../../../utils/shipmentUtils";
type FilterKeys = "sort" | "status";

interface Props {
  searchText: string;
  setSearchText: (val: string) => void;
  dateRange: [dayjs.Dayjs, dayjs.Dayjs] | null;
  setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
  filters: Record<FilterKeys, string>;
  setFilters: (key: FilterKeys, value: string) => void;
  onReset: () => void;
}

const { Option } = Select;

const SearchFilters: React.FC<Props> = ({
  searchText,
  setSearchText,
  dateRange,
  setDateRange,
  filters,
  setFilters,
  onReset,
}) => {
  const [hover, setHover] = useState(false);
  return (
    <div className="search-filters-container">
      <Row gutter={16} className="search-filters-row">
        <Col span={24}>
          <div className="list-page-actions">
            <Input
              className="search-input"
              placeholder="Tìm kiếm theo mã chuyến xe"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
              prefix={<SearchOutlined />}
            />

            <Select
              className="filter-select-fit"
              value={filters.sort}
              onChange={(val) => setFilters("sort", val)}
              listHeight={SHIPMENT_FILTER_SORT.length * 40 + 50}
            >
              <Select.Option value="NONE">Không áp dụng sắp xếp</Select.Option>
              {SHIPMENT_FILTER_SORT.map((t) => (
                <Option key={t} value={t}>{translateShipmentFilterSort(t)}</Option>
              ))}
            </Select>

            <Select
              className="filter-select"
              value={filters.status}
              onChange={(val) => setFilters("status", val)}
            >
              <Select.Option value="All">Tất cả trạng thái</Select.Option>
              {SHIPMENT_STATUSES.map((s) => (
                <Select.Option key={s} value={s}>
                  {translateShipmentStatus(s)}
                </Select.Option>
              ))}
            </Select>

            <DatePicker.RangePicker
              className="date-picker"
              value={dateRange as any}
              onChange={(val) => setDateRange(val as any)}
            />

            <Button
              type="default"
              icon={<CloseCircleOutlined />}
              onClick={onReset}
              onMouseEnter={() => setHover(true)}
              onMouseLeave={() => setHover(false)}
              className="filter-button filter-button-icon-only"
            >
              {hover && "Bỏ lọc"}
            </Button>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default SearchFilters;