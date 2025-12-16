import React, { useState } from "react";
import { Row, Col, Input, Button, Select, } from "antd";
import { CloseCircleOutlined, SearchOutlined } from "@ant-design/icons";
import { ORDER_FILTER_COD, ORDER_FILTER_SORT, ORDER_PAYER_TYPES, translateOrderFilterCod, translateOrderFilterSort, translateOrderPayerType } from "../../../../../utils/orderUtils";

type FilterKeys = "payer" | "cod" | "sort";

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
            <Input
              className="search-input"
              placeholder="Tìm theo mã đơn"
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              allowClear
              prefix={<SearchOutlined />}
            />

            <Select
              value={filters.payer}
              onChange={(val) => setFilters("payer", val)}
              className="filter-select-fit"
            >
              <Select.Option value="ALL">Tất cả người thanh toán</Select.Option>
              {ORDER_PAYER_TYPES.map((p) => <Select.Option key={p} value={p}>{translateOrderPayerType(p)}</Select.Option>)}
            </Select>

            <Select
              value={filters.sort}
              onChange={(val) => setFilters("sort", val)}
              className="filter-select"
              listHeight={400}
            >
              {ORDER_FILTER_SORT.map((s) => <Select.Option key={s} value={s}>{translateOrderFilterSort(s)}</Select.Option>)}
            </Select>

            <Select
              value={filters.cod}
              onChange={(val) => setFilters("cod", val)}
              className="filter-select-fit"
            >
              {ORDER_FILTER_COD.map((t) => (
                <Option key={t} value={t}>{translateOrderFilterCod(t)}</Option>
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