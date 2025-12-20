import React, { useState } from "react";
import { Row, Col, Input, Button, Select, DatePicker, Tooltip } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { SETTLEMENT_BATCH_FILTER_SORT, SETTLEMENT_BATCH_STATUSES, SETTLEMENT_BATCH_TYPES, translateSettlementBatchStatus, translateSettlementBatchType, translateSettlementFilterSort } from "../../../../utils/settlementBatchUtils";

type FilterKeys = "sort" | "status" | "type"

interface Props {
  searchText: string;
  setSearchText: (val: string) => void;
  dateRange: [dayjs.Dayjs, dayjs.Dayjs] | null;
  setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
  filters: Record<FilterKeys, string>;
  setFilters: (key: FilterKeys, value: string) => void;
  onReset: () => void;
}

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
            <Tooltip title="Tìm theo mã đối soát, mã đơn hàng">
              <Input
                className="search-input"
                placeholder="Tìm kiếm..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                allowClear
                prefix={<SearchOutlined />}
              />
            </Tooltip>

            <Select
              value={filters.sort}
              onChange={(val) => setFilters("sort", val)}
              className="filter-select"
              listHeight={SETTLEMENT_BATCH_FILTER_SORT.length * 40 + 50}
            >
              {SETTLEMENT_BATCH_FILTER_SORT.map((t) => <Select.Option key={t} value={t}>{translateSettlementFilterSort(t)}</Select.Option>)}
            </Select>

            <Select
              value={filters.status}
              onChange={(val) => setFilters("status", val)}
              className="filter-select"
              listHeight={SETTLEMENT_BATCH_STATUSES.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {SETTLEMENT_BATCH_STATUSES.map((t) => <Select.Option key={t} value={t}>{translateSettlementBatchStatus(t)}</Select.Option>)}
            </Select>

             <Select
              value={filters.type}
              onChange={(val) => setFilters("type", val)}
              className="filter-select"
              listHeight={SETTLEMENT_BATCH_TYPES.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả loại</Select.Option>
              {SETTLEMENT_BATCH_TYPES.map((t) => <Select.Option key={t} value={t}>{translateSettlementBatchType(t)}</Select.Option>)}
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