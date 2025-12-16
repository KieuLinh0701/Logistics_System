import React from 'react';
import { Input, Select, DatePicker, Button, Row, Col, Tooltip } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import { SHIPPING_REQUEST_FILTER_SORT, SHIPPING_REQUEST_STATUS, SHIPPING_REQUEST_TYPES, translateShippingRequestFilterSort, translateShippingRequestStatus, translateShippingRequestType } from '../../../../../utils/shippingRequestUtils';

const { Option } = Select;
const { RangePicker } = DatePicker;

interface SearchFiltersProps {
  searchText: string;
  filterRrequestType: string;
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
  filterRrequestType,
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
            <Tooltip
              title="Tìm theo mã yêu cầu/ĐH, nội dung phản hồi/yêu cầu, mã người gửi, tên, email, số điện thoại người gửi"
              placement="topLeft"
            >
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
              value={filterRrequestType}
              onChange={(val) => onFilterChange('requestType', val)}
              className="filter-select"
            >
              <Option value="ALL">Tất cả loại yêu cầu</Option>
              {SHIPPING_REQUEST_TYPES.map((t) => (
                <Option key={t} value={t}>{translateShippingRequestType(t)}</Option>
              ))}
            </Select>

            <Select
              value={filterStatus}
              onChange={(val) => onFilterChange('status', val)}
              className="filter-select-fit"
            >
              <Option value="ALL">Tất cả trạng thái</Option>
              {SHIPPING_REQUEST_STATUS.map((s) => (
                <Option key={s} value={s}>{translateShippingRequestStatus(s)}</Option>
              ))}
            </Select>

            <Select
              value={sort}
              onChange={onSortChange}
              className="filter-select-fit"
            >
              {SHIPPING_REQUEST_FILTER_SORT.map((t) => (
                <Option key={t} value={t}>{translateShippingRequestFilterSort(t)}</Option>
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