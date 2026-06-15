import React, {useEffect, useRef, useState} from 'react';
import {Col, message, Row, Tag} from 'antd';
import Title from 'antd/es/typography/Title';
import {UserSwitchOutlined} from '@ant-design/icons';
import {useParams, useSearchParams} from "react-router-dom";
import dayjs from "dayjs";
import SearchFilters from "./components/SearchFilters.tsx";
import userApi from "../../../../api/userApi.ts";
import DataTable from "./components/Table.tsx";
import {getActiveValue} from "../../../../utils/userUtils.ts";
import type {ShopWorkHistory, UserShopWorkHistorySearchUserRequest} from "../../../../types/shopWorkHistory.ts";

const UserEmployeeHistory: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const userId = Number(id);
    const [shopWorkHistory, setShopWorkHistory] = useState<ShopWorkHistory[]>([]);
    const [loading, setLoading] = useState(false);
    const [search, setSearch] = useState("");
    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const [filterSort, setFilterSort] = useState("NEWEST");
    const [filterActive, setFilterActive] = useState("ALL");

    const updateURL = () => {
        const params: any = {};

        if (search) params.search = search;
        params.active = filterSort.toLowerCase();
        params.sort = filterActive.toLowerCase();
        if (page >= 1) params.page = page;
        if (dateRange) {
            params.start = dateRange[0].format("YYYY-MM-DD");
            params.end = dateRange[1].format("YYYY-MM-DD");
        }
        setSearchParams(params, {replace: true});
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search");
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const active = searchParams.get("active")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setPage(pageParam);
        if (s) setSearch(s);
        if (sort) setFilterSort(sort);
        if (active) setFilterActive(active);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, []);

    const fetchData = async (currentPage = page) => {
        if (!userId) { return }

        try {
            const requestId = ++latestRequestRef.current;
            setLoading(true);

            const param: UserShopWorkHistorySearchUserRequest = {
                page: currentPage,
                limit: limit,
                search: search,
                sort: filterSort,
                isCurrent: getActiveValue(filterActive)
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await userApi.listWorkHistoryByUserId(userId, param);

            if (requestId !== latestRequestRef.current) return;

            if (result && result.success && result.data) {
                setShopWorkHistory(result.data?.list || []);
                setTotal(result.data.pagination?.total || 0);
            } else {
                setShopWorkHistory([]);
                setTotal(0);
                message.error(result.message || "Lỗi khi lấy danh sách quyền");
            }
        } catch (error: any) {
            setShopWorkHistory([]);
            setTotal(0);
            message.error(error.message || "Có lỗi khi lấy danh sách quyền");
        } finally {
            setLoading(false);
        }
    };

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'sort':
                setFilterSort(value);
                break;
            case 'active':
                setFilterActive(value);
                break;
        }
        setPage(1);
    };

    const handleClearFilters = () => {
        setSearch("");
        setFilterActive("ALL");
        setFilterSort("NEWEST");
        setDateRange(null);
        setPage(1);
    };

    useEffect(() => {
        setPage(1);
    }, [search]);


    useEffect(() => {
        updateURL();
        fetchData(page);
    }, [
        page,
        limit,
        search,
        filterActive,
        filterSort,
        dateRange
    ]);

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    search={search}
                    setSearch={setSearch}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                    filters={{ sort: filterSort, active: filterActive }}
                    setFilters={handleFilterChange}
                    onReset={handleClearFilters}
                />

                <Row justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <UserSwitchOutlined className="title-icon"/>
                            Lịch sử phân quyền
                        </Title>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total}</Tag>

                <DataTable
                    data={shopWorkHistory}
                    page={page}
                    total={total}
                    loading={loading}
                    limit={limit}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                        fetchData(page);
                    }}
                />
            </div>
        </div>
    );
};

export default UserEmployeeHistory;