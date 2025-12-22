import React, { useState } from "react";
import { Row, Col, Input, Button, Select, DatePicker, Tooltip } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import { INCIDENT_FILTER_SORT, INCIDENT_PRIORITYS, INCIDENT_STATUSES, INCIDENT_TYPES, translateIncidentPriority, translateIncidentStatus, translateIncidentType } from "../../../../../utils/incidentUtils";
import { translateEmployeeFilterSort } from "../../../../../utils/employeeUtils";

type FilterKeys = "type" | "sort" | "status" | 'priority';

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
            <Tooltip title={searchText || "Tìm theo mã sự cố, mã đơn hàng, tiêu đề/mô tả sự cố, tên/SĐT người gửi, tên/SĐT người xử lý"}>
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
              className="filter-select-fit"
            >
              {INCIDENT_FILTER_SORT.map((t) => <Select.Option key={t} value={t}>{translateEmployeeFilterSort(t)}</Select.Option>)}
            </Select>

            <Select
              value={filters.priority}
              onChange={(val) => setFilters("priority", val)}
              className="filter-select"
              listHeight={INCIDENT_PRIORITYS.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả độ ưu tiên</Select.Option>
              {INCIDENT_PRIORITYS.map((t) => <Select.Option key={t} value={t}>{translateIncidentPriority(t)}</Select.Option>)}
            </Select>

            <Select
              value={filters.type}
              onChange={(val) => setFilters("type", val)}
              className="filter-select"
              listHeight={INCIDENT_TYPES.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả loại sự cố</Select.Option>
              {INCIDENT_TYPES.map((t) => <Select.Option key={t} value={t}>{translateIncidentType(t)}</Select.Option>)}
            </Select>

            <Select
              value={filters.status}
              onChange={(val) => setFilters("status", val)}
              className="filter-select"
              listHeight={INCIDENT_STATUSES.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {INCIDENT_STATUSES.map((t) => <Select.Option key={t} value={t}>{translateIncidentStatus(t)}</Select.Option>)}
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