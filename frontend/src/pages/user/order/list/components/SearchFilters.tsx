import React, { useState } from "react";
import { Row, Col, Input, Button, Select, DatePicker, Tooltip } from "antd";
import { CloseCircleOutlined, CloseOutlined, FilterOutlined, SearchOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import type { ServiceType } from "../../../../../types/serviceType";
import { ORDER_FILTER_COD, ORDER_FILTER_SORT, ORDER_PAYER_TYPES, ORDER_PAYMENT_STATUS, ORDER_PICKUP_TYPES, ORDER_STATUS, translateOrderFilterCod, translateOrderFilterSort, translateOrderPayerType, translateOrderPaymentStatus, translateOrderPickupType, translateOrderStatus } from "../../../../../utils/orderUtils";

type FilterKeys = "status" | "payer" | "paymentStatus" | "serviceType" | "cod" | "sort" | "pickupType";

interface Props {
  search: string;
  setSearch: (val: string) => void;
  dateRange: [dayjs.Dayjs, dayjs.Dayjs] | null;
  setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
  showAdvancedFilters: boolean;
  setShowAdvancedFilters: (val: boolean) => void;
  filters: Record<FilterKeys, any>;
  setFilters: (key: FilterKeys, value: any) => void;
  serviceTypes: ServiceType[];
  onReset: () => void;
}

const SearchFilters: React.FC<Props> = ({
  search,
  setSearch,
  dateRange,
  setDateRange,
  showAdvancedFilters,
  setShowAdvancedFilters,
  filters,
  setFilters,
  serviceTypes,
  onReset,
}) => {
  const [hover, setHover] = useState(false);

  return (
    <div className="search-filters-container">
      <Row className="search-filters-row" gutter={16}>
        <Col span={24}>
          <div className="list-page-actions">
            <Tooltip title="Tìm theo mã đơn, tên khách hàng, số điện thoại khách hàng và ghi chú">
              <Input
                className="search-input"
                placeholder="Tìm kiếm..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                allowClear
                prefix={<SearchOutlined />}
              />
            </Tooltip>
            <Select
              value={filters.sort}
              onChange={(val) => setFilters("sort", val)}
              className="filter-select"
              listHeight={400}
            >
              {ORDER_FILTER_SORT.map((s) => <Select.Option key={s} value={s}>{translateOrderFilterSort(s)}</Select.Option>)}
            </Select>
            <Select
              value={filters.status}
              onChange={(val) => setFilters("status", val)}
              className="advanced-filter-select"
              listHeight={ORDER_STATUS.length * 40 + 50}
            >
              <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
              {ORDER_STATUS.map((s) => <Select.Option key={s} value={s}>{translateOrderStatus(s)}</Select.Option>)}
            </Select>
            <DatePicker.RangePicker
              className="date-picker"
              value={dateRange as any}
              onChange={(val) => setDateRange(val as any)}
            />
            <Button
              type="default"
              onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
              className="filter-button filter-button-with-text"
              icon={showAdvancedFilters ? <CloseOutlined /> : <FilterOutlined />}
            >
              {showAdvancedFilters ? "Ẩn lọc nâng cao" : "Lọc nâng cao"}
            </Button>
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

      {showAdvancedFilters && (
        <Row className="advanced-filters-row">
          <Col span={24}>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
              <Select
                value={filters.pickupType}
                onChange={(val) => setFilters("pickupType", val)}
                className="advanced-filter-select"
                listHeight={ORDER_PICKUP_TYPES.length * 40 + 50}
              >
                <Select.Option value="ALL">Tất cả hình thức lấy hàng</Select.Option>
                {ORDER_PICKUP_TYPES.map((s) => <Select.Option key={s} value={s}>{translateOrderPickupType(s)}</Select.Option>)}
              </Select>

              <Select
                value={filters.payer}
                onChange={(val) => setFilters("payer", val)}
                className="advanced-filter-select"
                listHeight={ORDER_PAYER_TYPES.length * 40 + 50}
              >
                <Select.Option value="ALL">Tất cả người thanh toán</Select.Option>
                {ORDER_PAYER_TYPES.map((p) => <Select.Option key={p} value={p}>{translateOrderPayerType(p)}</Select.Option>)}
              </Select>

              <Select
                value={filters.paymentStatus}
                onChange={(val) => setFilters("paymentStatus", val)}
                className="advanced-filter-select"
                listHeight={ORDER_PAYMENT_STATUS.length * 40 + 50}
              >
                <Select.Option value="ALL">Tất cả trạng thái thanh toán</Select.Option>
                {ORDER_PAYMENT_STATUS.map((p) => <Select.Option key={p} value={p}>{translateOrderPaymentStatus(p)}</Select.Option>)}
              </Select>

              <Select
                value={filters.serviceType}
                onChange={(val) => setFilters("serviceType", val)}
                className="advanced-filter-select"
                listHeight={serviceTypes.length * 40 + 50}
              >
                <Select.Option value={null}>Tất cả dịch vụ</Select.Option>
                {serviceTypes.map(st => (
                  <Select.Option key={st.id} value={st.id}>
                    {st.name}
                  </Select.Option>
                ))}
              </Select>

              <Select
                value={filters.cod}
                onChange={(val) => setFilters("cod", val)}
                className="advanced-filter-select"
                listHeight={ORDER_FILTER_COD.length * 40 + 50}
              >
                {ORDER_FILTER_COD.map((c) => <Select.Option key={c} value={c}>{translateOrderFilterCod(c)}</Select.Option>)}
              </Select>
            </div>
          </Col>
        </Row>
      )}
    </div>
  );
};

export default SearchFilters;