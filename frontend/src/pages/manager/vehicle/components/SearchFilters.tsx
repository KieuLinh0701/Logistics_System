import React from 'react';
import { Input, Select, DatePicker, Button, Row, Col } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { translateVehicleFilterSort, translateVehicleStatus, translateVehicleType, VEHICLE_FILTER_SORT, VEHICLE_STATUSES, VEHICLE_TYPES } from '../../../../utils/vehicleUtils';

const { RangePicker } = DatePicker;

interface SearchFiltersProps {
  searchText: string;
  filterType: string;
  filterStatus: string;
  filterSort: string;
  dateRange: [Dayjs, Dayjs] | null;
  hover: boolean;
  onSearchChange: (value: string) => void;
  onFilterChange: (filter: string, value: string) => void;
  onDateRangeChange: (dates: [Dayjs, Dayjs] | null) => void;
  onClearFilters: () => void;
  onHoverChange: (hover: boolean) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
  searchText,
  filterType,
  filterStatus,
  filterSort,
  dateRange,
  hover,
  onSearchChange,
  onFilterChange,
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
            <Input
              className="search-input"
              placeholder="Tìm theo biển số xe, mô tả"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => onSearchChange(e.target.value)}
              allowClear
            />

            <Select
              className="filter-select-fit"
              value={filterType}
              onChange={(val) => onFilterChange('type', val)}
            >
              <Select.Option value="ALL">Tất cả loại xe</Select.Option>
              {VEHICLE_TYPES.map((t) => <Select.Option key={t} value={t}>{translateVehicleType(t)}</Select.Option>)}
            </Select>

            <Select
              className="filter-select"
              value={filterStatus}
              onChange={(val) => onFilterChange('status', val)}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {VEHICLE_STATUSES.map((s) => <Select.Option key={s} value={s}>{translateVehicleStatus(s)}</Select.Option>)}
            </Select>

            <Select
              className="filter-select"
              value={filterSort}
              onChange={(val) => onFilterChange('sort', val)}
            >
              {VEHICLE_FILTER_SORT.map((s) => <Select.Option key={s} value={s}>{translateVehicleFilterSort(s)}</Select.Option>)}
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