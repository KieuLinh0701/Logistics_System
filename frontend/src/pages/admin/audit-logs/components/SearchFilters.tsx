import React from 'react';
import {Button, Col, DatePicker, Input, Row, Select, Tooltip} from 'antd';
import {CloseCircleOutlined, CloseOutlined, FilterOutlined, SearchOutlined} from '@ant-design/icons';
import type {Dayjs} from 'dayjs';
import {
    AUDIT_LOG_ACTION,
    AUDIT_LOG_FILTER_SORT,
    AUDIT_LOG_STATUS,
    translateAuditLogAction,
    translateAuditLogFilterSort,
    translateAuditLogStatus
} from "../../../../utils/auditLogUtils.ts";
import {ENTITY_TYPE, translateEntityType} from "../../../../utils/entityTypeUtils.ts";

const {RangePicker} = DatePicker;

interface SearchFiltersProps {
    searchText: string;
    filterAction: string;
    filterEntity: string;
    filterStatus: string;
    filterSort: string;
    dateRange: [Dayjs, Dayjs] | null;
    hover: boolean;
    onSearchChange: (value: string) => void;
    onFilterChange: (filter: string, value: string) => void;
    onDateRangeChange: (dates: [Dayjs, Dayjs] | null) => void;
    onClearFilters: () => void;
    onHoverChange: (hover: boolean) => void;
    showAdvancedFilters: boolean;
    setShowAdvancedFilters: (val: boolean) => void;
}

const SearchFilters: React.FC<SearchFiltersProps> = ({
                                                         searchText,
                                                         filterAction,
                                                         filterEntity,
                                                         filterStatus,
                                                         filterSort,
                                                         dateRange,
                                                         hover,
                                                         onSearchChange,
                                                         onFilterChange,
                                                         onDateRangeChange,
                                                         onClearFilters,
                                                         onHoverChange,
                                                         showAdvancedFilters,
                                                         setShowAdvancedFilters,
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
                            title="Tìm kiếm theo mã đối tượng, tên nhân viên, SĐT nhân viên, mã bưu cục, tên bưu cục hoặc hành động">
                            <Input
                                className="search-input"
                                placeholder="Tìm kiếm..."
                                prefix={<SearchOutlined/>}
                                value={searchText}
                                onChange={(e) => onSearchChange(e.target.value)}
                                allowClear
                            />
                        </Tooltip>

                        <Select
                            className="filter-select-fit"
                            value={filterSort}
                            onChange={(val) => onFilterChange('sort', val)}
                        >
                            {AUDIT_LOG_FILTER_SORT.map((s) => <Select.Option key={s}
                                                                             value={s}>{translateAuditLogFilterSort(s)}</Select.Option>)}
                        </Select>

                        <RangePicker
                            className="date-picker"
                            value={dateRange}
                            onChange={handleDateRangeChange}
                        />

                        <Button
                            type="default"
                            onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                            className="filter-button filter-button-with-text"
                            icon={showAdvancedFilters ? <CloseOutlined/> : <FilterOutlined/>}
                        >
                            {showAdvancedFilters ? "Ẩn lọc nâng cao" : "Lọc nâng cao"}
                        </Button>

                        <Button
                            type="default"
                            icon={<CloseCircleOutlined/>}
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

            {showAdvancedFilters && (
                <Row className="advanced-filters-row">
                    <Col span={24}>
                        <div style={{display: "flex", flexWrap: "wrap", gap: 8}}>
                            <Select
                                className="advanced-filter-select"
                                value={filterAction}
                                onChange={(val) => onFilterChange('action', val)}
                            >
                                <Select.Option value="ALL">Tất cả hành động</Select.Option>
                                {AUDIT_LOG_ACTION.map((s) => <Select.Option key={s}
                                                                            value={s}>{translateAuditLogAction(s)}</Select.Option>)}
                            </Select>

                            <Select
                                className="advanced-filter-select"
                                value={filterEntity}
                                onChange={(val) => onFilterChange('entity', val)}
                            >
                                <Select.Option value="ALL">Tất cả đối tượng</Select.Option>
                                {ENTITY_TYPE.map((s) => <Select.Option key={s}
                                                                            value={s}>{translateEntityType(s)}</Select.Option>)}
                            </Select>

                            <Select
                                className="advanced-filter-select"
                                value={filterStatus}
                                onChange={(val) => onFilterChange('status', val)}
                            >
                                <Select.Option value="ALL">Tất cả trạng thái</Select.Option>
                                {AUDIT_LOG_STATUS.map((s) => <Select.Option key={s}
                                                                            value={s}>{translateAuditLogStatus(s)}</Select.Option>)}
                            </Select>
                        </div>
                    </Col>
                </Row>
            )}
        </div>
    );
};

export default SearchFilters;