import React, {useEffect, useRef, useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {Col, message, Row, Tag} from "antd";
import dayjs from "dayjs";
import OrderActions from "./components/Actions";
import SearchFilters from "./components/SearchFilters";
import OrderTable from "./components/Table";
import Title from "antd/es/typography/Title";
import type {Order, StatusCount, UserOrderSearchRequest} from "../../../../types/order";
import orderApi from "../../../../api/orderApi";
import "../../../../styles/ListPage.css";
import type {ServiceType} from "../../../../types/serviceType";
import serviceTypeApi from "../../../../api/serviceTypeApi";
import {ShoppingOutlined} from "@ant-design/icons";
import ConfirmPublicModal from "../detail/components/ConfirmPublicModal";
import ConfirmCancelModal from "../detail/components/ConfirmCancelModal";
import ConfirmDeleteModal from "../detail/components/ConfirmDeleteModal";
import ConfirmModal from "../../../common/ConfirmModal";
import userApi from "../../../../api/userApi";
import StatusBar from "../../../../components/order/StatusBar.tsx";
import BulkResult from "../../../manager/order/list/components/BulkResult.tsx";
import type {BulkResponse} from "../../../../types/response.ts";
import type {ManagerOrderShipment} from "../../../../types/shipment.ts";
import shipmentOrderApi from "../../../../api/shipmentOrderApi.ts";

const UserOrderList = () => {
    const navigate = useNavigate();
    const latestRequestRef = useRef(0);
    const selectAllRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();

    const [limit, setLimit] = useState(10);
    const [page, setPage] = useState(1);
    const [total, setTotal] = useState<number>(0);

    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(false);

    const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);

    const [search, setSearch] = useState("");
    const [filterStatus, setFilterStatus] = useState("ALL");
    const [filterSort, setFilterSort] = useState("NEWEST");
    const [filterPayer, setFilterPayer] = useState("ALL");
    const [filterPaymentStatus, setFilterPaymentStatus] = useState("ALL");
    const [filterServiceType, setFilterServiceType] = useState<number | null>(null);
    const [filterCOD, setFilterCOD] = useState("ALL");
    const [filterPickupType, setFilterPickupType] = useState("ALL");
    const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
    const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

    const [cancelModalOpen, setCancelModalOpen] = useState(false);
    const [publicModalOpen, setPublicModalOpen] = useState(false);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [readyOrdersOpen, setReadyOrdersOpen] = useState(false);
    const [cancelOrdersOpen, setCancelOrdersOpen] = useState(false);
    const [deleteOrdersOpen, setDeleteOrdersOpen] = useState(false);
    const [transitToOfficeOrdersOpen, setTransitToOfficeOrdersOpen] = useState(false);
    const [publicOrdersOpen, setPublicOrdersOpen] = useState(false);
    const [modalConfirmOpen, setModalConfirmOpen] = useState(false);
    const [transitToOfficeModalOpen, setTransitToOfficeModalOpen] = useState(false);
    const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);

    const [userLocked, setUserLocked] = useState<boolean>(false);

    const [orderId, setOrderId] = useState<number | null>(null);

    const [statusCounts, setStatusCounts] = useState<StatusCount[]>([]);

    const [bulkModalOpen, setBulkModalOpen] = useState(false);
    const [bulkResult, setBulkResult] = useState<BulkResponse<ManagerOrderShipment>>();

    const updateURL = () => {
        const params: any = {};

        if (search) params.search = search;
        if (filterStatus !== "ALL") params.status = filterStatus.toLowerCase();
        if (filterPayer !== "ALL") params.payer = filterPayer.toLowerCase();
        if (filterPaymentStatus !== "ALL") params.payment = filterPaymentStatus.toLowerCase();
        if (filterServiceType !== null) params.service = filterServiceType;
        if (filterCOD !== "ALL") params.cod = filterCOD.toLowerCase();
        if (filterPickupType !== "ALL") params.pickupType = filterPickupType.toLowerCase();
        params.sort = filterSort.toLowerCase();
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
        const status = searchParams.get("status")?.toLocaleUpperCase();
        const payer = searchParams.get("payer")?.toLocaleUpperCase();
        const payment = searchParams.get("payment")?.toLocaleUpperCase();
        const service = searchParams.get("service");
        const cod = searchParams.get("cod")?.toLocaleUpperCase();
        const sort = searchParams.get("sort")?.toLocaleUpperCase();
        const startDate = searchParams.get("start");
        const endDate = searchParams.get("end");
        const pick = searchParams.get("pickupType")?.toLocaleUpperCase();

        setPage(pageParam);
        if (s) setSearch(s);
        if (status) setFilterStatus(status);
        if (payer) setFilterPayer(payer);
        if (payment) setFilterPaymentStatus(payment);

        if (service) setFilterServiceType(Number(service));

        if (cod) setFilterCOD(cod);
        if (sort) setFilterSort(sort);

        if (pick) setFilterPickupType(pick);

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
            const requestId = ++latestRequestRef.current;
            setLoading(true);
            console.log('page', page);

            const param: UserOrderSearchRequest = {
                page: currentPage,
                limit: limit,
                search: search,
                payer: filterPayer !== "ALL" ? filterPayer : undefined,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                serviceTypeId: filterServiceType !== null ? filterServiceType : undefined,
                paymentStatus: filterPaymentStatus !== "ALL" ? filterPaymentStatus : undefined,
                cod: filterCOD !== "ALL" ? filterCOD : undefined,
                sort: filterSort,
                pickupType: filterPickupType !== "ALL" ? filterPickupType : undefined,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await orderApi.listUserOrders(param);

            if (requestId !== latestRequestRef.current) return;

            if (result.success && result.data) {
                const orderList = result.data?.list || [];
                setOrders(orderList);
                setTotal(result.data.pagination?.total || 0);
            } else {
                message.error(result.message || "Lỗi khi lấy danh sách đơn hàng");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi lấy danh sách đơn hàng");
        } finally {
            setLoading(false);
        }
    };

    const fetchStatusCounts = async () => {
        const result = await orderApi.getUserOrderStatusCounts();
        if (result.success && result.data) {
            setStatusCounts(result.data);
        }
    };

    // --- Cancel Order ---
    const handleCancelOrder = (id: number) => {
        setOrderId(id);
        setCancelModalOpen(true);
    };

    const confirmCancelOrder = async () => {
        setLoading(true);
        try {
            if (!orderId) return;
            const result = await orderApi.cancelUserOrder(orderId);
            if (result.success && result.data) {
                message.success("Hủy đơn hàng thành công");
                fetchOrders();
                fetchStatusCounts();
            } else {
                message.error(result.message || "Hủy đơn thất bại");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi server khi hủy đơn hàng");
        } finally {
            setCancelModalOpen(false);
            setLoading(false);
        }
    };

    const handlePublicOrder = (id: number) => {
        if (userLocked) {
            message.error("Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi chuyển đơn hàng sang xử lý.");
            return;
        }
        setOrderId(id);
        setPublicModalOpen(true);
    };

    const confirmPublicOrder = async () => {
        setLoading(true);
        try {
            if (!orderId) return;
            const result = await orderApi.publicUserOrder(orderId);

            if (result.success && result.data) {
                message.success("Đã chuyển đơn hàng sang xử lý thành công");

                fetchOrders();
                fetchStatusCounts();
            } else {
                message.error(result.message || "Chuyển trạng thái thất bại");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi chuyển trạng thái đơn hàng");
        } finally {
            setPublicModalOpen(false);
            setLoading(false);
        }
    };

    const handleDeleteOrder = (id: number) => {
        setOrderId(id);
        setDeleteModalOpen(true);
    };

    const confirmDeleteOrder = async () => {
        setLoading(true);
        try {
            if (!orderId) return;
            const result = await orderApi.deleteUserOrder(orderId);

            if (result.success) {
                message.success("Xóa đơn hàng thành công");
                fetchOrders();
                fetchStatusCounts();
            } else {
                message.error(result.message || "Xóa đơn hàng thất bại");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi xóa đơn hàng");
        } finally {
            setDeleteModalOpen(false);
            setLoading(false);
        }
    };

    const handlePrintOrder = (id: number) => {
        if (!id) return;

        navigate(`/orders/print?orderIds=${id}`);
    };

    const handlePrintSelectedOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để in phiếu vận đơn");
            return;
        }

        navigate(`/orders/print?orderIds=${selectedOrderIds.join(",")}`);
    };

    const handleExport = async () => {
        try {
            setLoading(true);
            const param: UserOrderSearchRequest = {
                page: page,
                limit: limit,
                search: search,
                payer: filterPayer !== "ALL" ? filterPayer : undefined,
                status: filterStatus !== "ALL" ? filterStatus : undefined,
                serviceTypeId: filterServiceType !== null ? filterServiceType : undefined,
                paymentStatus: filterPaymentStatus !== "ALL" ? filterPaymentStatus : undefined,
                cod: filterCOD !== "ALL" ? filterCOD : undefined,
                sort: filterSort,
                pickupType: filterPickupType !== "ALL" ? filterPickupType : undefined,
            };
            if (dateRange) {
                param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
            }

            const result = await orderApi.exportUserOrders(param);


            if (!result.success) {
                console.error("Export thất bại:", result.error);
                message.error("Xuất file Excel thất bại");
            }

        } catch (error: any) {
            message.error("Xuất file Excel thất bại");
            console.error("Export thất bại:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleSelectAllFiltered = async (select: boolean) => {
        if (select) {
            try {
                const param: UserOrderSearchRequest = {
                    page: 1,
                    limit: limit,
                    search: search,
                    payer: filterPayer !== "ALL" ? filterPayer : undefined,
                    status: filterStatus !== "ALL" ? filterStatus : undefined,
                    serviceTypeId: filterServiceType !== null ? filterServiceType : undefined,
                    paymentStatus: filterPaymentStatus !== "ALL" ? filterPaymentStatus : undefined,
                    cod: filterCOD !== "ALL" ? filterCOD : undefined,
                    pickupType: filterPickupType !== "ALL" ? filterPickupType : undefined,
                    sort: filterSort,
                };
                if (dateRange) {
                    param.startDate = dateRange[0].startOf("day").format("YYYY-MM-DDTHH:mm:ss");
                    param.endDate = dateRange[1].endOf("day").format("YYYY-MM-DDTHH:mm:ss");
                }

                const result = await orderApi.getAllUserOrderIds(param);

                if (result.success && result.data) {
                    setSelectedOrderIds(result.data);
                } else {
                    message.error(result.message || "Lấy toàn bộ ID thất bại");
                }
            } catch (err: any) {
                message.error(err.message || "Lỗi server khi lấy toàn bộ ID");
            }
        } else {
            setSelectedOrderIds([]);
        }
    };

    const handleEditOrder = (id: number, tracking: string) => {
        if (tracking !== null) {
            navigate(`/orders/tracking/${tracking}/edit`);
        } else {
            navigate(`/orders/id/${id}/edit`);
        }
    };

    const handleAdd = () => {
        if (userLocked) {
            message.error("Phiên đối soát của bạn đã quá hạn thanh toán, tài khoản tạm khóa. Vui lòng hoàn tất thanh toán các phiên trước khi tạo đơn hàng mới.");
            return;
        }
        navigate(`/orders/create`);
    };

    const handleReadyOrder = (id: number) => {
        setOrderId(id);
        setModalConfirmOpen(true);
    };

    const handleTransitToOffice = (id: number) => {
        setOrderId(id);
        setTransitToOfficeModalOpen(true);
    };

    const handleReadyOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận đã hoàn thành việc đóng gói và sẵn sàng để đơn vị vận chuyển đến lấy");
            return;
        }
        setReadyOrdersOpen(true);
    };

    const handleCancelOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận hủy");
            return;
        }
        setCancelOrdersOpen(true);
    };

    const handleDeleteOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận xóa");
            return;
        }
        setDeleteOrdersOpen(true);
    };

    const handlePublicOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận muốn chuyển qua trạng thái công khai");
            return;
        }
        setPublicOrdersOpen(true);
    };

    const handleTransitToOfficeOrders = () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận chuyển sang trạng thái đang đưa đến bưu cục");
            return;
        }
        setTransitToOfficeOrdersOpen(true);
    };

    const confirmReadyOrders = async () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận đã hoàn thành việc đóng gói và sẵn sàng để đơn vị vận chuyển đến lấy");
            return;
        }
        try {
            const result = await orderApi.readyBulkUserOrders(
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
                fetchOrders();
            } else {
                message.error(result.message || "Một số đơn hàng không thể cập nhật trạng thái");
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setSelectedOrderIds([]);
            setReadyOrdersOpen(false);
        }
    };

    const confirmTransitToOfficeOrders = async () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xác nhận đang trên đường giao đến bưu cục");
            return;
        }
        try {
            const result = await orderApi.transitToOfficeBulkUserOrders(
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
                fetchOrders();
            } else {
                message.error(result.message || "Một số đơn hàng không cập nhật trạng thái");
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setSelectedOrderIds([]);
            setTransitToOfficeOrdersOpen(false);
        }
    };

    const confirmCancelOrders = async () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để hủy");
            return;
        }
        try {
            const result = await orderApi.cancelBulkUserOrders(
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
                fetchOrders();
            } else {
                message.error(result.message || "Một số đơn hàng không cập nhật trạng thái");
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setSelectedOrderIds([]);
            setCancelOrdersOpen(false);
        }
    };

    const confirmDeleteOrders = async () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để xóa");
            return;
        }
        try {
            const result = await orderApi.deleteBulkUserOrders(
                selectedOrderIds
            );

            const hasDetails = result.results && result.results.length > 0;

            if (hasDetails) {
                setBulkResult(result as any);
                setBulkModalOpen(true);
            }

            if (result.success) {
                message.success(result.message || "Xóa thành công");
                selectAllRequestRef.current++;
                setSelectedOrderIds([]);
                fetchOrders();
            } else {
                message.error(result.message || "Một số đơn hàng không thể xóa");
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setSelectedOrderIds([]);
            setDeleteOrdersOpen(false);
        }
    };

    const confirmPublicOrders = async () => {
        if (!selectedOrderIds.length) {
            message.warning("Vui lòng chọn đơn hàng để chuyển đơn hàng này sang trạng thái công khai");
            return;
        }
        try {
            const result = await orderApi.publicBulkUserOrders(
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
                fetchOrders();
            } else {
                message.error(result.message || "Một số đơn hàng không thể chuyển sang trạng thái công khai");
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setSelectedOrderIds([]);
            setPublicOrdersOpen(false);
        }
    };

    const confirmReadyOrder = async () => {
        if (!orderId) return;

        setModalConfirmOpen(false);

        try {
            setLoading(true);

            const result = await orderApi.setUserOrderReadyForPickup(orderId);

            if (result.success) {
                message.success("Chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy' thành công.");
                fetchOrders();
                fetchStatusCounts();
            } else {
                message.error(result.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy'!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Sẵn sàng để lấy'!");
        } finally {
            setLoading(false);
            setOrderId(null);
        }
    };

    const confirmTransitToOfficeOrder = async () => {
        if (!orderId) return;

        setTransitToOfficeModalOpen(false);

        try {
            setLoading(true);

            const result = await orderApi.setUserOrderTransitToOffice(orderId);

            if (result.success) {
                message.success("Chuyển đơn hàng sang trạng thái 'Đang chuyển về bưu cục' thành công.");
                fetchOrders();
                fetchStatusCounts();
            } else {
                message.error(result.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Đang chuyển về bưu cục'!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi chuyển đơn hàng sang trạng thái 'Đang chuyển về bưu cục'!");
        } finally {
            setLoading(false);
            setOrderId(null);
        }
    };

    const handleClearFilters = () => {
        setSearch("");
        setFilterStatus("ALL");
        setFilterServiceType(null);
        setFilterSort("NEWEST");
        setFilterPayer("ALL");
        setFilterPaymentStatus("ALL");
        setFilterCOD("ALL");
        setDateRange(null);
        setFilterPickupType("ALL");
        setPage(1);
    };

    const handleFilterChange = (filter: string, value: string) => {
        switch (filter) {
            case 'status':
                setFilterStatus(value);
                break;
            case 'payer':
                setFilterPayer(value);
                break;
            case 'paymentStatus':
                setFilterPaymentStatus(value);
                break;
            case 'serviceType':
                setFilterServiceType(Number(value));
                break;
            case 'cod':
                setFilterCOD(value);
                break;
            case 'sort':
                setFilterSort(value);
                break;
            case 'pickupType':
                setFilterPickupType(value);
                break;
        }
        setPage(1);
    };

    useEffect(() => {
        const fetchData = async () => {
            setLoading(true);
            try {
                // Kiểm tra user khóa
                const lockRes = await userApi.checkUserLocked();
                if (lockRes.success && lockRes.data != null) {
                    setUserLocked(lockRes.data);
                } else {
                    message.error(lockRes.message || "Lỗi khi kiểm tra trạng thái khóa");
                }

                // Lấy danh sách dịch vụ
                const serviceRes = await serviceTypeApi.getActiveServiceTypes();
                if (serviceRes.success && serviceRes.data) {
                    setServiceTypes(serviceRes.data);
                } else {
                    message.error(serviceRes.message || "Lỗi khi lấy danh sách dịch vụ");
                }

            } catch (error: any) {
                message.error(error.message || "Đã xảy ra lỗi khi tải dữ liệu");
            } finally {
                setLoading(false);
            }
        };

        fetchStatusCounts();
        fetchData();
    }, []);

    useEffect(() => {
        updateURL();
        fetchOrders(page);
    }, [
        page,
        limit,
        search,
        filterStatus,
        filterServiceType,
        filterPayer,
        filterPaymentStatus,
        filterCOD,
        dateRange,
        filterSort,
        filterPickupType
    ]);

    return (
        <div className="list-page-layout">
            <div className="list-page-content">
                <SearchFilters
                    search={search}
                    setSearch={setSearch}
                    dateRange={dateRange}
                    setDateRange={setDateRange}
                    showAdvancedFilters={showAdvancedFilters}
                    setShowAdvancedFilters={setShowAdvancedFilters}
                    filters={{
                        payer: filterPayer,
                        paymentStatus: filterPaymentStatus,
                        serviceType: filterServiceType,
                        cod: filterCOD,
                        sort: filterSort,
                        pickupType: filterPickupType
                    }}
                    setFilters={handleFilterChange}
                    serviceTypes={serviceTypes}
                    onReset={handleClearFilters}
                />

                <StatusBar
                    statusCounts={statusCounts}
                    activeStatus={filterStatus}
                    onStatusChange={(val) => handleFilterChange("status", val)}
                />

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <ShoppingOutlined className="title-icon"/>
                            Danh sách đơn hàng
                        </Title>
                    </Col>
                    <Col>
                        <div className="list-page-actions">
                            <OrderActions
                                onAdd={handleAdd}
                                onPrint={handlePrintSelectedOrders}
                                onExport={handleExport}
                                onReadyBulk={handleReadyOrders}
                                hasSelection={selectedOrderIds.length !== 0}
                                selectedCount={selectedOrderIds.length}
                                onCancel={handleCancelOrders}
                                onDelete={handleDeleteOrders}
                                onPublic={handlePublicOrders}
                                onTransitToOffice={handleTransitToOfficeOrders}
                                total={total}
                            />
                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

                <OrderTable
                    orders={orders}
                    onCancel={handleCancelOrder}
                    onPublic={handlePublicOrder}
                    onDelete={handleDeleteOrder}
                    onPrint={handlePrintOrder}
                    onEdit={handleEditOrder}
                    onReady={handleReadyOrder}
                    onTransitToOffice={handleTransitToOffice}
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

            <ConfirmCancelModal
                open={cancelModalOpen}
                onOk={confirmCancelOrder}
                onCancel={() => setCancelModalOpen(false)}
                loading={loading}
            />

            <ConfirmPublicModal
                open={publicModalOpen}
                onOk={confirmPublicOrder}
                onCancel={() => setPublicModalOpen(false)}
                loading={loading}
            />

            <ConfirmDeleteModal
                open={deleteModalOpen}
                onOk={confirmDeleteOrder}
                onCancel={() => setDeleteModalOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn đơn hàng này đã sẵn sàng để bàn giao cho đơn vị vận chuyển không?'
                open={modalConfirmOpen}
                onOk={confirmReadyOrder}
                onCancel={() => setModalConfirmOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn đơn hàng này đang chuyển về bưu cục đã chọn trước đó không?'
                open={transitToOfficeModalOpen}
                onOk={confirmTransitToOfficeOrder}
                onCancel={() => setTransitToOfficeModalOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn các đơn hàng này đã sẵn sàng để bàn giao cho đơn vị vận chuyển không?'
                open={readyOrdersOpen}
                onOk={confirmReadyOrders}
                onCancel={() => setReadyOrdersOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn muốn hủy các đơn hàng này không?'
                open={cancelOrdersOpen}
                onOk={confirmCancelOrders}
                onCancel={() => setCancelOrdersOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn muốn xóa các đơn hàng này không?'
                open={deleteOrdersOpen}
                onOk={confirmDeleteOrders}
                onCancel={() => setDeleteOrdersOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn muốn chuyển các đơn hàng này sang trạng thái công khai không?'
                open={publicOrdersOpen}
                onOk={confirmPublicOrders}
                onCancel={() => setPublicOrdersOpen(false)}
                loading={loading}
            />

            <ConfirmModal
                title='Xác nhận đơn hàng'
                message='Bạn có chắc chắn muốn chuyển các đơn hàng này sang trạng thái đang vận chuyển đến bưu cục đích không?'
                open={transitToOfficeOrdersOpen}
                onOk={confirmTransitToOfficeOrders}
                onCancel={() => setTransitToOfficeOrdersOpen(false)}
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

export default UserOrderList;