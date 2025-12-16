import React, { useState } from "react";
import { Row, Col, Input, Button, Select, DatePicker, Tooltip } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { OFFICE_MANAGER_ADDABLE_ROLES, translateRoleName } from "../../../../../utils/roleUtils";
import { EMPLOYEE_PERFORMANCE_FILTER_SORT, EMPLOYEE_SHIFTS, EMPLOYEE_STATUSES, translateEmployeePerformanceFilterSort, translateEmployeeShift, translateEmployeeStatus } from "../../../../../utils/employeeUtils";

type FilterKeys = "sort" | "role" | 'shift' | 'status';

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
            <Tooltip title="Tìm kiếm theo mã, tên hoặc số điện thoại nhân viên">
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
              className="filter-select-fit"
              value={filters.sort}
              onChange={(val) => setFilters("sort", val)}
              listHeight={EMPLOYEE_PERFORMANCE_FILTER_SORT.length * 40 + 50}
            >
              <Select.Option value="NONE">Không áp dụng sắp xếp</Select.Option>
              {EMPLOYEE_PERFORMANCE_FILTER_SORT.map((t) => (
                <Option key={t} value={t}>{translateEmployeePerformanceFilterSort(t)}</Option>
              ))}
            </Select>

            <Select
              className="filter-select-fit"
              value={filters.shift}
              onChange={(val) => setFilters('shift', val)}
            >
              <Select.Option value="ALL">Tất cả ca</Select.Option>
              {EMPLOYEE_SHIFTS.map((s) => <Select.Option key={s} value={s}>{translateEmployeeShift(s)}</Select.Option>)}

            </Select>

            <Select
              className="filter-select"
              value={filters.status}
              onChange={(val) => setFilters('status', val)}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {EMPLOYEE_STATUSES.map((s) => <Select.Option key={s} value={s}>{translateEmployeeStatus(s)}</Select.Option>)}
            </Select>

            <Select
              value={filters.role}
              onChange={(val) => setFilters("role", val)}
              className="filter-select"
            >
              <Select.Option value="ALL">Tất cả chức vụ</Select.Option>
              {OFFICE_MANAGER_ADDABLE_ROLES.map((t) => (
                <Option key={t} value={t}>{translateRoleName(t)}</Option>
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