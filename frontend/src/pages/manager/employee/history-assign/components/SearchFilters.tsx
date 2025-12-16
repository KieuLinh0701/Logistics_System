import React, { useEffect, useState } from 'react';
import { Input, Select, DatePicker, Button, Row, Col, Tooltip } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { Dayjs } from 'dayjs';
import locationApi from '../../../../../api/locationApi';
import { SHIPPER_ASSIGNMENT_FILTER_SORT, translateShipperAssignmentFilterSort } from '../../../../../utils/shipperAssignmentUtils';

const { RangePicker } = DatePicker;
const { Option } = Select;

interface SearchFiltersProps {
  cityCode: number;
  searchText: string;
  filterWardCode: number | undefined;
  filterSort: string;
  dateRange: [Dayjs, Dayjs] | null;
  hover: boolean;
  onSearchChange: (value: string) => void;
  onFilterChange: (filter: string, value: string | number | undefined) => void;
  onDateRangeChange: (dates: [Dayjs, Dayjs] | null) => void;
  onClearFilters: () => void;
  onHoverChange: (hover: boolean) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
  cityCode,
  searchText,
  filterSort,
  filterWardCode,
  dateRange,
  hover,
  onSearchChange,
  onFilterChange,
  onDateRangeChange,
  onClearFilters,
  onHoverChange,
}) => {
  const [wards, setWards] = useState<{ code: number; name: string }[]>([]);

  useEffect(() => {
    const fetchWards = async () => {
      if (!cityCode) {
        setWards([]);
        return;
      }
      try {
        const res = await locationApi.getWardsByCity(cityCode);
        setWards(res);
      } catch (err) {
        console.error('Lỗi lấy phường/xã:', err);
        setWards([]);
      }
    };

    fetchWards();
  }, [cityCode]);

  const handleDateRangeChange = (dates: [Dayjs | null, Dayjs | null] | null) => {
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
              className="filter-select"
              value={filterWardCode}
              onChange={(val) => onFilterChange('ward', val)}
              placeholder="Tất cả phường/xã"
              allowClear
              showSearch
              optionFilterProp="children"
              filterOption={(input, option) => {
                if (!option?.children) return false;
                const childText = String(option.children); 
                return childText.toLowerCase().includes(input.toLowerCase());
              }}
            >
              <Select.Option value={undefined}>Tất cả phường/xã</Select.Option>
              {wards.map((w) => (
                <Select.Option key={w.code} value={w.code}>
                  {w.name}
                </Select.Option>
              ))}
            </Select>

            <Select
              className="filter-select-fit"
              value={filterSort}
              onChange={(val) => onFilterChange('sort', val)}
            >
              {SHIPPER_ASSIGNMENT_FILTER_SORT.map((s) => (
                <Option key={s} value={s}>
                  {translateShipperAssignmentFilterSort(s)}
                </Option>
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