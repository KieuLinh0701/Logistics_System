import React, { useState } from "react";
import { Row, Col, Input, Button, Select, DatePicker, Tooltip } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { SETTLEMENT_BATCH_FILTER_SORT, SETTLEMENT_BATCH_ORDER_FILTER_SORT, SETTLEMENT_BATCH_ORDER_STATUS, SETTLEMENT_BATCH_STATUSES, SETTLEMENT_BATCH_TYPES, translateSettlementBatchStatus, translateSettlementBatchType, translateSettlementFilterSort } from "../../../../utils/settlementBatchUtils";
import { ORDER_FILTER_COD, ORDER_PAYER_TYPES, ORDER_STATUS, translateOrderFilterCod, translateOrderFilterSort, translateOrderPayerType, translateOrderStatus } from "../../../../utils/orderUtils";

type FilterKeys = "sort" | "status" | "payer" | "cod"

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
            <Tooltip title="Tìm theo mã đơn hàng">
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
              listHeight={SETTLEMENT_BATCH_ORDER_FILTER_SORT.length * 40 + 50}
            >
              {SETTLEMENT_BATCH_ORDER_FILTER_SORT.map((t) => <Select.Option key={t} value={t}>{translateOrderFilterSort(t)}</Select.Option>)}
            </Select>

            <Select
              value={filters.status}
              onChange={(val) => setFilters("status", val)}
              className="filter-select-fit"
              listHeight={SETTLEMENT_BATCH_ORDER_STATUS.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {SETTLEMENT_BATCH_ORDER_STATUS.map((t) => <Select.Option key={t} value={t}>{translateOrderStatus(t)}</Select.Option>)}
            </Select>

             <Select
              value={filters.payer}
              onChange={(val) => setFilters("payer", val)}
              className="filter-select-fit"
              listHeight={ORDER_PAYER_TYPES.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả người thanh toán</Select.Option>
              {ORDER_PAYER_TYPES.map((t) => <Select.Option key={t} value={t}>{translateOrderPayerType(t)}</Select.Option>)}
            </Select>

            <Select
              value={filters.cod}
              onChange={(val) => setFilters("cod", val)}
              className="filter-select-fit"
              listHeight={ORDER_FILTER_COD.length * 40 + 50}
            >
              {ORDER_FILTER_COD.map((t) => <Select.Option key={t} value={t}>{translateOrderFilterCod(t)}</Select.Option>)}
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