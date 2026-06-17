import React from 'react';
import {Col, DatePicker, Input, Row, Tooltip} from 'antd';
import {SearchOutlined} from '@ant-design/icons';
import dayjs, {type Dayjs} from 'dayjs';

const {RangePicker} = DatePicker;

interface SearchFiltersProps {
    search: string;
    dateRange: [Dayjs, Dayjs] | null;
    setSearch: (val: string) => void;
    setDateRange: (val: [dayjs.Dayjs, dayjs.Dayjs] | null) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
                                                         search,
                                                         dateRange,
                                                         setSearch,
                                                         setDateRange
                                                     }) => {
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
                        <Tooltip title="Tìm theo tên hoặc mã nhân viên">
                            <Input
                                className="search-input"
                                placeholder="Tìm kiếm theo tên hoặc mã nhân viên..."
                                prefix={<SearchOutlined/>}
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                allowClear
                            />
                        </Tooltip>

                        <RangePicker
                            className="date-picker"
                            value={dateRange}
                            onChange={handleDateRangeChange}
                        />
                    </div>
                </Col>
            </Row>
        </div>
    );
};

export default SearchFilters;