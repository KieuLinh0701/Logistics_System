import React from 'react';
import {Button, Col, DatePicker, Input, Row, Select, Tooltip} from 'antd';
import {CloseCircleOutlined, SearchOutlined} from '@ant-design/icons';
import type {Dayjs} from 'dayjs';
import {
    SHIPMENT_FILTER_SORT,
    SHIPMENT_STATUSES,
    translateShipmentFilterSort,
    translateShipmentStatus
} from '../../../../utils/shipmentUtils';

const { Option } = Select;
const { RangePicker } = DatePicker;

interface SearchFiltersProps {
  searchText: string;
  filterStatus: string;
  sort: string;
  dateRange: [Dayjs, Dayjs] | null;
  hover: boolean;
  onSearchChange: (value: string) => void;
  onFilterChange: (filter: string, value: string) => void;
  onSortChange: (value: string) => void;
  onDateRangeChange: (dates: [Dayjs, Dayjs] | null) => void;
  onClearFilters: () => void;
  onHoverChange: (hover: boolean) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
  searchText,
  filterStatus,
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
    dates: [Dayjs | null, Dayjs | null] | null
  ) => {
    if (dates && dates[0] && dates[1]) {
      onDateRangeChange([dates[0], dates[1]]);
    } else {
      onDateRangeChange(null);
    }
  };

  return (
    <div className="search-filters-container">
      <Row gutter={16} className="search-filters-row">
        <Col span={24}>
          <div className="list-page-actions">
            <Tooltip title="Tìm theo mã chuyến hàng">
              <Input
                className="search-input"
                placeholder="Tìm theo mã chuyến hàng"
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => onSearchChange(e.target.value)}
                allowClear
              />
            </Tooltip>

            <Select
              value={filterStatus}
              onChange={(val) => onFilterChange('status', val)}
              className="filter-select"
            >
              <Option value="ALL">Tất cả trạng thái</Option>
              {SHIPMENT_STATUSES.map((s) => (
                <Option key={s} value={s}>{translateShipmentStatus(s)}</Option>
              ))}
            </Select>

            <Select
              value={sort}
              onChange={onSortChange}
              className="filter-select-fit"
            >
              {SHIPMENT_FILTER_SORT.map((t) => (
                <Option key={t} value={t}>{translateShipmentFilterSort(t)}</Option>
              ))}
            </Select>

            <RangePicker
              className="date-picker"
              value={dateRange}
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