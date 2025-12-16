import React from 'react';
import { Input, DatePicker, Row, Col } from 'antd';
import {SearchOutlined } from '@ant-design/icons';

interface SearchFiltersProps {
  searchText: string;
  onSearchChange: (value: string) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
  searchText,
  onSearchChange,
}) => {

  return (
    <div className="search-filters-container">
      <Row gutter={16} className="search-filters-row">
        <Col span={24}>
          <div className="list-page-actions">
            <Input
              className="search-input"
              placeholder="Tìm kiếm theo mã nhân viên, tên, email hoặc số điện thoại nhân viên"
              prefix={<SearchOutlined />}
              value={searchText}
              onChange={(e) => onSearchChange(e.target.value)}
              allowClear
            />
          </div>
        </Col>
      </Row>
    </div>
  );
};

export default SearchFilters;