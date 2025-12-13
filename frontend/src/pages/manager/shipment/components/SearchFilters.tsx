import React from 'react';
import { Input, Select, DatePicker, Button, Row, Col, Tooltip } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { SHIPMENT_FILTER_SORT, SHIPMENT_STATUSES, SHIPMENT_TYPES, translateShipmentFilterSort, translateShipmentStatus, translateShipmentType } from '../../../../utils/shipmentUtils';

const { Option } = Select;
const { RangePicker } = DatePicker;

interface SearchFiltersProps {
  searchText: string;
  filterType: string;
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
  filterType,
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
            <Tooltip title="Tìm theo mã chuyến hàng, biển số xe, mã/tên/SĐT/email nhân viên nhận chuyến">
              <Input
                className="search-input"
                placeholder="Tìm kiếm..."
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => onSearchChange(e.target.value)}
                allowClear
              />
            </Tooltip>

            <Select
              value={filterType}
              onChange={(val) => onFilterChange('type', val)}
              className="filter-select"
            >
              <Option value="ALL">Tất cả loại chuyến</Option>
              {SHIPMENT_TYPES.map((t) => (
                <Option key={t} value={t}>{translateShipmentType(t)}</Option>
              ))}
            </Select>

            <Select
              value={filterStatus}
              onChange={(val) => onFilterChange('status', val)}
              className="filter-select-fit"
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