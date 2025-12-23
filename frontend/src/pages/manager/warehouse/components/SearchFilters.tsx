import React from 'react';
import { Input, Select, DatePicker, Button, Row, Col } from 'antd';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import type { ServiceType } from '../../../../types/serviceType';
import { translateWarehouseFilterSort, WAREHOUSE_FILTER_SORT } from '../../../../utils/warehouseUtils';

const { Option } = Select;

interface SearchFiltersProps {
  searchText: string;
  filterServiceType: number | string;
  sort: string;
  hover: boolean;
  serviceTypes: ServiceType[];
  onSearchChange: (value: string) => void;
  onFilterChange: (filter: string, value: number | string) => void;
  onSortChange: (value: string) => void;
  onClearFilters: () => void;
  onHoverChange: (hover: boolean) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
  searchText,
  filterServiceType,
  sort,
  hover,
  serviceTypes,
  onSearchChange,
  onFilterChange,
  onSortChange,
  onClearFilters,
  onHoverChange,
}) => {

  return (
    <div className="search-filters-container">
      <Row gutter={16} className="search-filters-row">
        <Col span={24}>
          <div className="list-page-actions">
            <Input
              className="search-input"
              placeholder="Tìm theo mã đơn hàng"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => onSearchChange(e.target.value)}
              allowClear
            />

            <Select
              className="filter-select"
              value={filterServiceType}
              onChange={(val) => onFilterChange('serviceType', val)}
            >
              <Select.Option value="ALL">Tất cả dịch vụ</Select.Option>
              {serviceTypes.map((t) => <Select.Option key={t.id} value={t.id}>{t.name}</Select.Option>)}
            </Select>

            <Select
              className="filter-select"
              value={sort}
              onChange={onSortChange}
            >
              {WAREHOUSE_FILTER_SORT.map((s) => <Select.Option key={s} value={s}>{translateWarehouseFilterSort(s)}</Select.Option>)}
            </Select>

            <Button
              className="filter-button filter-button-icon-only"
              type="default"
              icon={<CloseCircleOutlined />}
              onClick={onClearFilters}
              onMouseEnter={() => onHoverChange(true)}
              onMouseLeave={() => onHoverChange(false)}
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