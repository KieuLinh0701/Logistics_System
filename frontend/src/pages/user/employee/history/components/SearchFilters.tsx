import React, {useState} from 'react';
import {Button, Col, DatePicker, Input, Row, Select, Tooltip} from 'antd';
import {CloseCircleOutlined, SearchOutlined} from '@ant-design/icons';
import dayjs, {type Dayjs} from 'dayjs';
import {
    translateUserActive,
    translateUserFilterSort,
    USER_ACTIVE,
    USER_FILTER_SORT
} from "../../../../../utils/userUtils.ts";

const {RangePicker} = DatePicker;

type FilterKeys = "sort" | "active";

interface SearchFiltersProps {
    search: string;
    dateRange: [Dayjs, Dayjs] | null;
    setSearch: (val: string) => void;
    filters: Record<FilterKeys, any>;
    setFilters: (key: FilterKeys, value: any) => void;
    setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
    onReset: () => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
                                                         search,
                                                         dateRange,
                                                         setSearch,
                                                         filters,
                                                         setFilters,
                                                         setDateRange,
                                                         onReset
                                                     }) => {
    const [hover, setHover] = useState(false);

    const handleDateRangeChange = (
        dates: [Dayjs | null, Dayjs | null] | null
    ) => {
        if (dates && dates[0] && dates[1]) {
            setDateRange([dates[0], dates[1]]);
        } else {
            setDateRange(null);
        }
    };

    return (
        <div className="search-filters-container">
            <Row gutter={16} className="search-filters-row">
                <Col span={24}>
                    <div className="list-page-actions">
                        <Tooltip title="Tìm theo theo mã nhân viên, họ tên, số điện thoại, email">
                            <Input
                                className="search-input"
                                placeholder="Tìm kiếm ..."
                                prefix={<SearchOutlined/>}
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                allowClear
                            />
                        </Tooltip>

                        <Select
                            value={filters.active}
                            onChange={(val) => setFilters("active", val)}
                            className="filter-select"
                            listHeight={400}
                        >
                            <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
                            {USER_ACTIVE.map((s) => <Select.Option key={s}
                                                                   value={s}>{translateUserActive(s)}</Select.Option>)}
                        </Select>

                        <Select
                            value={filters.sort}
                            onChange={(val) => setFilters("sort", val)}
                            className="filter-select"
                            listHeight={400}
                        >
                            {USER_FILTER_SORT.map((s) => <Select.Option key={s}
                                                                        value={s}>{translateUserFilterSort(s)}</Select.Option>)}
                        </Select>

                        <RangePicker
                            className="date-picker"
                            value={dateRange}
                            onChange={handleDateRangeChange}
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