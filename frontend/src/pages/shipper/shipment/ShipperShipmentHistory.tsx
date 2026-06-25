import React, {useEffect, useRef, useState} from 'react';
import {Col, message, Row, Tag} from 'antd';
import dayjs from 'dayjs';
import SearchFilters from './components/SearchFilters';
import Actions from './components/Actions';
import RequestTable from './components/Table';
import {useNavigate, useSearchParams} from 'react-router-dom';
import Title from 'antd/es/typography/Title';
import {ExportOutlined, TruckOutlined} from '@ant-design/icons';
import "./ShipperShipmentHistory.css"
import type {ManagerShipment, ShipperShipmentSearchRequest} from '../../../types/shipment';
import shipmentApi from '../../../api/shipmentApi';

const ShipperShipmentHistory: React.FC = () => {
    const latestRequestRef = useRef(0);
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();

    const [shipments, setShipments] = useState<ManagerShipment[] | []>([]);
    const [loading, setLoading] = useState(false)

    const [total, setTotal] = useState(0)
    const [page, setPage] = useState(1);
    const [limit, setLimit] = useState(10);

    const [hover, setHover] = useState(false);

    const [searchText, setSearchText] = useState('');
    const [filterStatus, setFilterStatus] = useState<string>('ALL');
    const [filterSort, setFilterSort] = useState('NEWEST');
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const updateURL = () => {
        const params: any = {};

        if (searchText) params.search = searchText;
        if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
        params.sort = filterSort.toLowerCase();
        if (page) params.page = page;

        if (dateRange) {
            params.start = dateRange[0].format("YYYY-MM-DD");
            params.end = dateRange[1].format("YYYY-MM-DD");
        }
        setSearchParams(params, {replace: true});
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search");
        const st = searchParams.get("status")?.toLocaleUpperCase();
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setPage(pageParam);
        if (s) setSearchText(s);
        if (st) setFilterStatus(st);
        if (sort) setFilterSort(sort);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, [searchParams]);

    const fetchShipments = async (currentPage = page) => {
        try {
            setLoading(true);
            const requestId = ++latestRequestRef.current;
            const param: ShipperShipmentSearchRequest = {
                page: currentPage,
                limit: limit,
                search: searchText,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await shipmentApi.listShipperShipments(param);
            if (requestId !== latestRequestRef.current) return;
            if (result.success && result.data) {
                const list = result.data?.list || [];
                setShipments(list);
                setTotal(result.data.pagination?.total || 0);
            } else {
                message.error(result.message || "Lỗi khi lấy danh sách chuyến hàng");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi lấy danh sách chuyến hàng");
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        try {
            const param: ShipperShipmentSearchRequest = {
                page: page,
                limit: limit,
                search: searchText,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await shipmentApi.exportShipperShipments(param);


            if (!result.success) {
                console.error("Export thất bại:", result.error);
                message.error("Xuất file Excel thất bại");
            }

        } catch (error: any) {
            message.error("Xuất file Excel thất bại");
            console.error("Export thất bại:", error);
        }
    };

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'search':
                setSearchText(value);
                break;
            case 'sort':
                setFilterSort(value);
                break;
            case 'status':
                setFilterStatus(value);
                break;
                break;
        }
        setPage(1);
    };

    const handleClearFilters = () => {
        setSearchText('');
        setFilterSort('ALL');
        setFilterStatus('ALL');
        setFilterSort('NEWEST');
        setDateRange(null);
        setPage(1);
    };

    const handleViewDetailShipment = async (shipment: ManagerShipment) => {
        navigate(`/shipper/shipments/history/${shipment.id}/orders`)
    };

    useEffect(() => {
        updateURL();
        fetchShipments(page);
    }, [page, limit, searchText, filterSort, filterStatus, dateRange]);

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    searchText={searchText}
                    filterStatus={filterStatus}
                    sort={filterSort}
                    dateRange={dateRange}
                    hover={hover}
                    onSearchChange={setSearchText}
                    onFilterChange={handleFilterChange}
                    onSortChange={setFilterSort}
                    onDateRangeChange={setDateRange}
                    onClearFilters={handleClearFilters}
                    onHoverChange={setHover}
                />

                <Row className="list-page-header" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <TruckOutlined className="title-icon" />
                            Danh sách chuyến hàng
                        </Title>
                    </Col>
                    <Col>
                        <Actions
                            onExport={handleExport}
                            total={total}
                        />
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} chuyến hàng</Tag>

                <RequestTable
                    data={shipments}
                    currentPage={page}
                    pageSize={limit}
                    total={total}
                    onDetail={handleViewDetailShipment}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                    }}
                />
            </div>

        </div>
    );
};

export default ShipperShipmentHistory;