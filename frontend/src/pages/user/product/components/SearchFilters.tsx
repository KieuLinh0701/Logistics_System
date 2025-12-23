import React from 'react';
import { Input, Select, DatePicker, Button, Row, Col } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { PRODUCT_FILTER_SORT, PRODUCT_FILTER_STOCK, PRODUCT_STATUS, PRODUCT_TYPES, translateProductFilterSort, translateProductFilterStock, translateProductStatus, translateProductType } from '../../../../utils/productUtils';
import type dayjs from 'dayjs';

const { Option } = Select;

interface SearchFiltersProps {
  search: string;
  filterType: string;
  filterStatus: string;
  filterStock: string;
  sort: string;
  dateRange: [dayjs.Dayjs, dayjs.Dayjs] | null;
  hover: boolean;
  onSearchChange: (value: string) => void;
  onFilterChange: (filter: string, value: string) => void;
  onSortChange: (value: string) => void;
  onDateRangeChange: (dates: [Dayjs, Dayjs] | null) => void;
  onClearFilters: () => void;
  onHoverChange: (hover: boolean) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
  search,
  filterType,
  filterStatus,
  filterStock,
  sort,
  dateRange,
  hover,
  onSearchChange,
  onFilterChange,
  onSortChange,
  onDateRangeChange,
  onClearFilters,
  onHoverChange,
}) => {

  const handleDateRangeChange = (
    dates: [Dayjs | null, Dayjs | null] | null,
  ) => {
    if (dates && dates[0] && dates[1]) {
      onDateRangeChange([dates[0], dates[1]]);
    } else {
      onDateRangeChange(null);
    }
  };

  return (
    <div className="search-filters-container">
      <Row className="search-filters-row" gutter={16}>
        <Col span={24}>
          <div className="list-page-actions">
            <Input
              className="search-input"
              placeholder="Tìm theo mã, tên sản phẩm"
              prefix={<SearchOutlined />}
              value={search}
              onChange={(e) => onSearchChange(e.target.value)}
              allowClear
            />

            <Select
              value={filterType}
              onChange={(val) => onFilterChange('type', val)}
              className="filter-select-fit"
              listHeight={PRODUCT_TYPES.length * 40 + 50}
            >
              <Option value="ALL">Tất cả loại</Option>
              {PRODUCT_TYPES.map((t) => (
                <Option key={t} value={t}>{translateProductType(t)}</Option>
              ))}
            </Select>

            <Select
              value={filterStatus}
              onChange={(val) => onFilterChange('status', val)}
              className="filter-select-fit"
              listHeight={PRODUCT_STATUS.length * 40 + 50}
            >
              <Option value="ALL">Tất cả trạng thái</Option>
              {PRODUCT_STATUS.map((s) => (
                <Option key={s} value={s}>{translateProductStatus(s)}</Option>
              ))}
            </Select>

            <Select
              value={filterStock}
              onChange={(val) => onFilterChange('stock', val)}
              className="filter-select-fit"
              listHeight={PRODUCT_FILTER_STOCK.length * 40 + 50}
            >
              {PRODUCT_FILTER_STOCK.map((s) => (
                <Option key={s} value={s}>{translateProductFilterStock(s)}</Option>
              ))}
            </Select>

            <Select
              value={sort}
              onChange={onSortChange}
              className="filter-select"
              listHeight={PRODUCT_FILTER_SORT.length * 40 + 50}
            >
              {PRODUCT_FILTER_SORT.map((s) => (
                <Option key={s} value={s}>{translateProductFilterSort(s)}</Option>
              ))}
            </Select>

            <DatePicker.RangePicker
              className="date-picker"
              value={dateRange as any}
              onChange={handleDateRangeChange}
            />

            <Button
              type="default"
              icon={<CloseCircleOutlined />}
              onClick={onClearFilters}
              onMouseEnter={() => onHoverChange(true)}
              onMouseLeave={() => onHoverChange(false)}
              className="filter-button filter-button-icon-only"
            >
              {hover && 'Bỏ lọc'}
            </Button>
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default SearchFilters;