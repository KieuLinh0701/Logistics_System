import React, {useEffect, useRef, useState} from "react";
import {useSearchParams} from "react-router-dom";
import {Col, message, Row, Tag} from "antd";
import dayjs from "dayjs";
import OrderActions from "./components/Actions";
import SearchFilters from "./components/SearchFilters";
import OrderTable from "./components/Table";
import Title from "antd/es/typography/Title";
import type {ManagerOrderSearchRequest, ManagerUrgentOrderSearchRequest, Order,} from "../../../../types/order";
import orderApi from "../../../../api/orderApi";
import "../../../../styles/ListPage.css";
import {ShoppingOutlined} from "@ant-design/icons";
import ConfirmModal from "../../../common/ConfirmModal";
import type {BulkResponse} from "../../../../types/response.ts";
import BulkResult from "./components/BulkResult.tsx";

const ManagerUrgentOrderList = () => {
    const [searchParams, setSearchParams] = useSearchParams();

    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);

    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(false);

    const [search, setSearch] = useState("");
    const [filterSort, setFilterSort] = useState("NEWEST");
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);

    const [confirmModalOpen, setConfirmModalOpen] = useState(false);

    const [orderId, setOrderId] = useState<number | null>(null);

    const [selectedOrderIds, setSelectedOrderIds] = useState<number[] | []>([]);
    const latestRequestRef = useRef(0);
    const selectAllRequestRef = useRef(0);
    const [confirmOrdersOpen, setConfirmOrdersOpen] = useState(false);
    const [bulkModalOpen, setBulkModalOpen] = useState(false);
    const [bulkResult, setBulkResult] = useState<BulkResponse<string>>();

    const updateURL = () => {
        const params: any = {};

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
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");

        setPage(pageParam);
        if (s) setSearch(s);
        if (sort) setFilterSort(sort);

        if (startDate && endDate) {
            setDateRange([
                dayjs(startDate, "YYYY-MM-DD"),
                dayjs(endDate, "YYYY-MM-DD")
            ]);
        }
    }, [searchParams]);

    // --- Fetch Orders ---
    const fetchOrders = async (currentPage = page) => {
        try {
            setLoading(true);
            const requestId = ++latestRequestRef.current;
            const param: ManagerUrgentOrderSearchRequest = {
                page: currentPage,
                limit: limit,
                search: search,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await orderApi.listUrgentManagerOrders(param);

            if (requestId !== latestRequestRef.current) return;

            if (result.success && result.data) {
                const orderList = result.data?.list || [];
                setOrders(orderList);
                setTotal(result.data.pagination?.total || 0);
            } else {
                message.error(result.message || "Lỗi khi lấy danh sách đơn hàng");
            }
        } catch (error: any) {
            console.error(error.message || "Error fetching orders:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        try {
            const param: ManagerOrderSearchRequest = {
                page: page,
                limit: limit,
                search: search,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await orderApi.exportManagerUrgentOrders(param);


            if (!result.success) {
                console.error("Export thất bại:", result.error);
                message.error("Xuất file Excel thất bại");
            }

        } catch (error: any) {
            message.error("Xuất file Excel thất bại");
            console.error("Export thất bại:", error);
        }
    };

    const handleSelectAllFiltered = async (select: boolean) => {
        if (!select) {
            setSelectedOrderIds([]);
            return;
        }
        const requestId = ++selectAllRequestRef.current;
        try {
            const param: ManagerOrderSearchRequest = {
                page: page,
                limit: limit,
                search: search,
                sort: filterSort,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await orderApi.getAllUrgentManagerOrderIds(param);

            if (requestId !== selectAllRequestRef.current) return;

            if (result.success && result.data) {
                setSelectedOrderIds(result.data);
            } else {
                message.error(result.message || "Lấy toàn bộ ID thất bại");
            }
        } catch (err: any) {
            message.error(err.message || "Lỗi server khi lấy toàn bộ ID");
        }
    };

    const handleClearFilters = () => {
        setSearch("");
        setFilterSort("NEWEST");
        setDateRange(null);
        setPage(1);
    };

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'sort':
                setFilterSort(value);
                break;
        }
        setPage(1);
    };

    useEffect(() => {

        updateURL();
        fetchOrders(page);
    }, [page, limit, search, dateRange, filterSort]);

    const handleConfirm = (id: number) => {
        setOrderId(id);
        setConfirmModalOpen(true);
    };

    const confirmConfirmOrder = async () => {
        if (!orderId) return;

        setConfirmModalOpen(false);

        try {
            setLoading(true);

            const result = await orderApi.confirmManagerUrgentOrder(orderId);

            if (result.success) {
                message.success("Xác nhận đơn hàng thành công. Vui lòng phân phối đơn hàng này cho đơn vị vận chuyển trong thời gian sớm nhất.");
                fetchOrders(page);
            } else {
                message.error(result.message || "Có lỗi khi xác nhận đơn hàng!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi xác nhận đơn hàng!");
        } finally {
            setLoading(false);
            setOrderId(null);
        }
    };

    const handleConfirmOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận muốn bưu cục tiếp nhận và xử lý");
            return;
        }
        setConfirmOrdersOpen(true);
    };

    const confirmConfirmOrders = async () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận muốn bưu cục tiếp nhận và xử lý");
            return;
        }
        try {
            const result = await orderApi.confirmBulkManagerUrgentOrders(
                selectedOrderIds
            );

            const hasDetails = result.results && result.results.length > 0;

            if (hasDetails) {
                setBulkResult(result as any);
                setBulkModalOpen(true);
            }

            if (result.success) {
                message.success(result.message || "Cập nhật thành công");
                selectAllRequestRef.current++;
                setSelectedOrderIds([]);
                fetchOrders(page);
            } else {
                message.error(result.message || "Một số đơn hàng không thể xác nhận đã nhận các đơn hàng tại bưu cục");
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setSelectedOrderIds([]);
            setConfirmOrdersOpen(false);
        }
    };

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    search={search}
                    setSearch={setSearch}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                    filters={{
                        sort: filterSort,
                    }}
                    setFilters={handleFilterChange}
                    onReset={handleClearFilters}
                />

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <ShoppingOutlined className="title-icon"/>
                            Danh sách đơn hàng khẩn cấp
                        </Title>
                    </Col>
                    <Col>
                        <div className="list-page-actions">
                            <OrderActions
                                total={total}
                                onExport={handleExport}
                                onConfirm={handleConfirmOrders}
                                hasSelected={selectedOrderIds.length !== 0}
                            />
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

                <OrderTable
                    orders={orders}
                    onConfirm={handleConfirm}
                    page={page}
                    total={total}
                    loading={loading}
                    limit={limit}
                    onPageChange={(page, size) => {
                        setPage(page);
                        if (size) setLimit(size);
                    }}
                    selectedOrderIds={selectedOrderIds}
                    setSelectedOrderIds={setSelectedOrderIds}
                    onSelectAllFiltered={handleSelectAllFiltered}
                />

            </div>

            <ConfirmModal
                title="Xác nhận đơn hàng"
                message="Bạn có chắc muốn xác nhận đơn hàng này để bưu cục tiếp nhận và xử lý không?"
                open={confirmModalOpen}
                onOk={confirmConfirmOrder}
                onCancel={() => setConfirmModalOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title="Xác nhận đơn hàng"
                message="Bạn có chắc muốn xác nhận các đơn hàng này để bưu cục tiếp nhận và xử lý không?"
                open={confirmOrdersOpen}
                onOk={confirmConfirmOrders}
                onCancel={() => setConfirmOrdersOpen(false)}
                loading={loading}
            />

            {bulkResult && (
                <BulkResult
                    open={bulkModalOpen}
                    results={bulkResult}
                    onClose={() => setBulkModalOpen(false)}
                />
            )}
        </div>
    );
};

export default ManagerUrgentOrderList;