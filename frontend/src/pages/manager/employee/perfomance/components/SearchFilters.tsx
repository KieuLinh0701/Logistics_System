import React, { useState } from "react";
import { Row, Col, Input, Button, Select, Tooltip } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import { OFFICE_MANAGER_ADDABLE_ROLES, translateRoleName } from "../../../../../utils/roleUtils";
import { EMPLOYEE_SHIFTS, EMPLOYEE_STATUSES, translateEmployeeShift, translateEmployeeStatus } from "../../../../../utils/employeeUtils";

type FilterKeys = "role" | 'shift' | 'status';

interface Props {
  searchText: string;
  setSearchText: (val: string) => void;
  filters: Record<FilterKeys, string>;
  setFilters: (key: FilterKeys, value: string) => void;
  onReset: () => void;
}

const { Option } = Select;

const SearchFilters: React.FC<Props> = ({
  searchText,
  setSearchText,
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
              className="filter-select"
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