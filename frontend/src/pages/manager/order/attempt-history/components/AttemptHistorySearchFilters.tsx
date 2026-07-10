import React, {useState} from "react";
import {Button, Col, DatePicker, Input, Row, Select, Tooltip} from "antd";
import {CloseCircleOutlined, SearchOutlined} from "@ant-design/icons";
import dayjs from "dayjs";
import {
    ATTEMPT_CATEGORIES,
    ATTEMPT_FILTER_SORT,
    ATTEMPT_STATUSES,
    translateAttemptCategory,
    translateAttemptSort,
    translateAttemptStatus,
} from "../../../../../utils/attemptHistoryUtils";

type FilterKeys = "category" | "status" | "sort";

interface Props {
    searchText: string;
    setSearchText: (val: string) => void;
    dateRange: [dayjs.Dayjs, dayjs.Dayjs] | null;
    setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
    filters: Record<FilterKeys, string>;
    setFilters: (key: FilterKeys, value: string) => void;
    onReset: () => void;
}

const AttemptHistorySearchFilters: React.FC<Props> = ({
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
                        <Tooltip title={searchText || "Tìm theo mã đơn hàng, tên shipper hoặc SĐT shipper"}>
                            <Input
                                className="search-input"
                                placeholder="Tìm kiếm..."
                                value={searchText}
                                onChange={(e) => setSearchText(e.target.value)}
                                allowClear
                                prefix={<SearchOutlined/>}
                            />
                        </Tooltip>

                        <Select
                            value={filters.sort}
                            onChange={(val) => setFilters("sort", val)}
                            className="filter-select-fit"
                        >
                            {ATTEMPT_FILTER_SORT.map((t) => (
                                <Select.Option key={t} value={t}>
                                    {translateAttemptSort(t)}
                                </Select.Option>
                            ))}
                        </Select>

                        <Select
                            value={filters.category}
                            onChange={(val) => setFilters("category", val)}
                            className="filter-select"
                            listHeight={ATTEMPT_CATEGORIES.length * 40 + 50}
                        >
                            <Select.Option value="ALL">Tất cả loại xử lý</Select.Option>
                            {ATTEMPT_CATEGORIES.map((t) => (
                                <Select.Option key={t} value={t}>
                                    {translateAttemptCategory(t)}
                                </Select.Option>
                            ))}
                        </Select>

                        <Select
                            value={filters.status}
                            onChange={(val) => setFilters("status", val)}
                            className="filter-select"
                            listHeight={ATTEMPT_STATUSES.length * 40 + 50}
                        >
                            <Select.Option value="ALL">Tất cả kết quả</Select.Option>
                            {ATTEMPT_STATUSES.map((t) => (
                                <Select.Option key={t} value={t}>
                                    {translateAttemptStatus(t)}
                                </Select.Option>
                            ))}
                        </Select>

                        <DatePicker.RangePicker
                            className="date-picker"
                            value={dateRange as any}
                            onChange={(val) => setDateRange(val as any)}
                        />
                        <Button
                            type="default"
                            icon={<CloseCircleOutlined/>}
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

export default AttemptHistorySearchFilters;
