import React, {useState} from "react";
import {Button, Col, DatePicker, Input, Row, Select, Tooltip} from "antd";
import {CloseCircleOutlined, SearchOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import {ORDER_FILTER_SORT, translateOrderFilterSort} from "../../../../../utils/orderUtils";

type FilterKeys = "sort";

interface Props {
  search: string;
  setSearch: (val: string) => void;
  dateRange: [dayjs.Dayjs, dayjs.Dayjs] | null;
  setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
  filters: Record<FilterKeys, any>;
  setFilters: (key: FilterKeys, value: any) => void;
  onReset: () => void;
}

const SearchFilters: React.FC<Props> = ({
  search,
  setSearch,
  dateRange,
  setDateRange,
  filters,
  setFilters,
  onReset,
}) => {
  const [hover, setHover] = useState(false);

  return (
    <div className="search-filters-container">
      <Row className="search-filters-row" gutter={16}>
        <Col span={24}>
          <div className="list-page-actions">
            <Tooltip
              title="Tìm theo mã đơn hàng hoặc địa chỉ lấy hàng"
              placement="top"
            >
              <Input
                className="search-input"
                placeholder="Tìm kiếm..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                allowClear
                prefix={<SearchOutlined />}
              />
            </Tooltip>
            <Select
              value={filters.sort}
              onChange={(val) => setFilters("sort", val)}
              className="filter-select"
              listHeight={400}
            >
              {ORDER_FILTER_SORT.map((s) => <Select.Option key={s} value={s}>{translateOrderFilterSort(s)}</Select.Option>)}
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