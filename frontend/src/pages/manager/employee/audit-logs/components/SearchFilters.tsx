import React from 'react';
import { Input, Select, DatePicker, Button, Row, Col, Tooltip } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { EMPLOYEE_FILTER_SORT, EMPLOYEE_SHIFTS, EMPLOYEE_STATUSES, translateEmployeeFilterSort, translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';
import { OFFICE_MANAGER_ADDABLE_ROLES, translateRoleName } from '../../../../../utils/roleUtils';

const { RangePicker } = DatePicker;

interface SearchFiltersProps {
  searchText: string;
  filterShift: string;
  filterStatus: string;
  filterSort: string;
  filterRole: string;
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
  filterShift,
  filterStatus,
  filterSort,
  filterRole,
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
            <Tooltip title="Tìm kiếm theo mã nhân viên, họ tên, email hoặc số điện thoại">
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
              className="filter-select-fit"
              value={filterShift}
              onChange={(val) => onFilterChange('shift', val)}
            >
              <Select.Option value="ALL">Tất cả ca</Select.Option>
              {EMPLOYEE_SHIFTS.map((s) => <Select.Option key={s} value={s}>{translateEmployeeShift(s)}</Select.Option>)}
            </Select>

            <Select
              className="filter-select"
              value={filterStatus}
              onChange={(val) => onFilterChange('status', val)}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {EMPLOYEE_STATUSES.map((s) => <Select.Option key={s} value={s}>{translateEmployeeStatus(s)}</Select.Option>)}
            </Select>

            <Select
              className="filter-select"
              value={filterRole}
              onChange={(val) => onFilterChange('role', val)}
            >
              <Select.Option value="ALL">Tất cả chức vụ</Select.Option>
              {OFFICE_MANAGER_ADDABLE_ROLES.map((r) => <Select.Option key={r} value={r}>{translateRoleName(r)}</Select.Option>)}
            </Select>

            <Select
              className="filter-select-fit"
              value={filterSort}
              onChange={(val) => onFilterChange('sort', val)}
            >
              {EMPLOYEE_FILTER_SORT.map((s) => <Select.Option key={s} value={s}>{translateEmployeeFilterSort(s)}</Select.Option>)}
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