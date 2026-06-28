import React, {useEffect, useRef, useState} from "react";
import {Button, Col, Input, message, Popconfirm, Row, Space, Table, Tag, Tooltip} from "antd";
import {
    CheckCircleOutlined,
    CloseCircleOutlined,
    DeleteOutlined,
    FileExcelOutlined,
    PlayCircleOutlined,
    PlusOutlined,
    SaveOutlined,
    SearchOutlined,
    ShoppingOutlined
} from "@ant-design/icons";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import type {ManagerOrderShipment} from "../../../types/shipment";
import type {BulkResponse} from "../../../types/response";
import shipmentApi from "../../../api/shipmentApi";
import shipmentOrderApi from "../../../api/shipmentOrderApi";
import BulkResult from "./components/BulkResult";
import {translateOrderPayerType, translateOrderPaymentStatus, translateOrderStatus} from "../../../utils/orderUtils";
import type {ColumnsType} from "antd/es/table";
import Title from "antd/es/typography/Title";
import "./ManagerShipments.css"
import {
    canConfirmDestinationOrdersManagerShipment,
    canDeleteOrdersManagerShipment,
    canEditOrdersManagerShipment
} from "../../../utils/shipmentUtils.ts";
import orderApi from "../../../api/orderApi.ts";
import ConfirmModal from "../../common/ConfirmModal.tsx";

const ManagerShipmentOrders: React.FC = () => {
    const {shipmentId} = useParams<{ shipmentId: string }>();
    const [shipmentStatus, setShipmentStatus] = useState<string>("");
    const [shipmentType, setShipmentType] = useState<string>("");
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();

    const [orders, setOrders] = useState<ManagerOrderShipment[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const limit = 10;
    const [total, setTotal] = useState(0);
    const [searchText, setSearchText] = useState("");

    const [tempAddedOrders, setTempAddedOrders] = useState<ManagerOrderShipment[]>([]);
    const [tempRemovedOrders, setTempRemovedOrders] = useState<number[]>([]);
    const [newTrackingNumber, setNewTrackingNumber] = useState("");
    const [addingLoading, setAddingLoading] = useState(false);

    const [bulkModalOpen, setBulkModalOpen] = useState(false);
    const [bulkResult, setBulkResult] = useState<BulkResponse<ManagerOrderShipment>>();
    const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);

    const [addressMap, setAddressMap] = useState<Record<string, string>>({});
    const [title, setTitle] = useState("");
    const [modalConfirmDestinationOfficeOpen, setModalConfirmDestinationOfficeOpen] = useState(false);
    const [modalConfirmDestinationOpen, setModalConfirmDestinationOpen] = useState(false);
    const [orderId, setOrderId] = useState<number | null>(null);
    const latestRequestRef = useRef(0);
    const selectAllRequestRef = useRef(0);

    const updateURL = () => {
        const params: any = {};

        if (searchText) params.search = searchText;
        if (page) params.page = page;

        setSearchParams(params, {replace: true});
    };

    useEffect(() => {
        const pageParam = Number(searchParams.get("page")) || 1;
        const s = searchParams.get("search") ?? "";

        setPage(pageParam);
        setSearchText(s);
    }, [searchParams]);

    // Lấy danh sách đơn hàng của chuyến
    const fetchOrders = async (currentPage = 1) => {
        if (!shipmentId) return;

        try {
            setLoading(true);
            const requestId = ++latestRequestRef.current;

            const res = await shipmentApi.getManagerOrdersByShipmentId(Number(shipmentId), {
                page: currentPage,
                limit,
                search: searchText,
            });

            if (requestId !== latestRequestRef.current) return;

            if (res.success && res.data) {
                setOrders(res.data.orders.list || []);
                setTotal(res.data.orders.pagination?.total || 0);
                setShipmentStatus(res.data.status);
                setShipmentType(res.data.type);
            } else {
                message.error(res.message || "Lỗi khi lấy đơn hàng");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi lấy đơn hàng");
        } finally {
            setLoading(false);
        }
    };

    const handleExport = async () => {
        try {
            const result = await shipmentApi.exportManagerOrdersByShipmentId(
                Number(shipmentId),
                {
                    page: page,
                    limit,
                    search: searchText,
                });

            if (!result.success) {
                console.error("Export thất bại:", result.error);
                message.error("Xuất file Excel thất bại");
            }

        } catch (error: any) {
            message.error("Xuất file Excel thất bại");
            console.error("Export thất bại:", error);
        }
    };

    useEffect(() => {
        updateURL();
        fetchOrders(page);
    }, [shipmentId, page, limit, searchText]);

    const handleTempRemove = (orderId: number) => {
        setTempRemovedOrders(prev => [...prev, orderId]);
    };

    const handleDiscardChanges = () => {
        setTempRemovedOrders([]);
        setTempAddedOrders([]);
        fetchOrders(page);
    };

    const handleAddOrder = async () => {
        if (!newTrackingNumber.trim() || !shipmentId) return;

        setAddingLoading(true);
        try {
            const result = await shipmentOrderApi.checkManagerOrderForShipment(
                Number(shipmentId),
                newTrackingNumber.trim()
            );

            if (result.results) {
                setBulkResult(result);
                setTitle("Kết quả thêm đơn hàng vào chuyến");
                setBulkModalOpen(true);

                const successfulOrders = result.results
                    .filter(r => r.success && r.result != null)
                    .map(r => r.result);

                if (successfulOrders.length > 0) {
                    const existingTrackingNumbers = new Set(
                        tempAddedOrders.map(o => o.trackingNumber)
                    );

                    const newOrders = successfulOrders.filter(
                        o => !existingTrackingNumbers.has(o.trackingNumber)
                    );

                    if (newOrders.length > 0) {
                        setTempAddedOrders(prev => [...prev, ...newOrders]);
                    }

                    const duplicates = successfulOrders.filter(
                        o => existingTrackingNumbers.has(o.trackingNumber)
                    );

                    if (duplicates.length > 0) {
                        message.warning(
                            `Đơn đã tồn tại: ${duplicates.map(d => d.trackingNumber).join(", ")}`
                        );
                    }
                }
            } else {
                message.error(result.message || "Không thể kiểm tra đơn hàng");
            }
        } catch (error: any) {
            message.error(error.message || "Đã xảy ra lỗi. Vui lòng thử lại.");
        } finally {
            setAddingLoading(false);
            setNewTrackingNumber("");
        }
    };

    const confirmDestinationOrders = async (confirmed: boolean) => {
        if (!selectedOrderIds.length) {
            message.error("Bạn chưa chọn hoặc chưa có đơn hàng nào cần xác nhận");
            setModalConfirmDestinationOfficeOpen(false);
            return;
        }

        setModalConfirmDestinationOfficeOpen(false);
        setAddingLoading(true);
        try {
            const result = await orderApi.saveManagerConfirmDestinationOrdersInShipment(
                selectedOrderIds,
                confirmed
            );

            setTitle("Kết quả xác nhận bưu cục đích");
            setBulkResult(result as any);
            setBulkModalOpen(true);

            if (result.results) {
                message.success(result.message || "Cập nhật thành công");
                selectAllRequestRef.current++;
                setSelectedOrderIds([]);
            } else {
                message.warning(result.message || "Một số đơn hàng không thể xác nhận");
                if (result.results) {
                    setBulkResult(result as any);
                    setBulkModalOpen(true);
                }
            }
        } catch (error: any) {
            message.error(error.message || "Đã xảy ra lỗi. Vui lòng thử lại.");
        } finally {
            setAddingLoading(false);
        }
    };

    const handleSaveOrders = async () => {
        if (!shipmentId) return;
        try {
            setLoading(true);
            const res = await shipmentOrderApi.saveManagerShipmentOrders(
                Number(shipmentId),
                tempRemovedOrders,
                tempAddedOrders.map(o => o.id)
            );
            if (res.success) {
                message.success(res.message || "Cập nhật thành công");
                setTempRemovedOrders([]);
                setTempAddedOrders([]);
                await fetchOrders(1);
                setPage(1);
            } else {
                message.warning(res.message || "Một số đơn hàng không thể thêm");
                if (res.results) {
                    setBulkResult(res as any);
                    setBulkModalOpen(true);
                }
            }
        } catch (error: any) {
            message.error(error.message || "Cập nhật thất bại");
        } finally {
            setLoading(false);
        }
    };

    const handleSelectAllFiltered = async (select: boolean) => {
        if (!select) {
            setSelectedOrderIds([]);
            return;
        }

        const requestId = ++selectAllRequestRef.current;
        if (!shipmentId) return;
        setLoading(true);
        try {
            const result = await shipmentApi.getManagerAllOrderIdsByShipmentId(Number(shipmentId), {
                search: searchText,
            });

            if (requestId !== selectAllRequestRef.current) return;

            if (result.success && result.data) {
                setSelectedOrderIds(result.data);
            } else {
                message.error(result.message || "Lấy toàn bộ ID thất bại");
            }
        } catch (err: any) {
            message.error(err.message || "Lỗi server khi lấy toàn bộ ID");
        } finally {
            setLoading(false);
        }
    };

    const confirmDestinationOffice = async (confirmed: boolean) => {
        if (!orderId) {
            message.error("Không tìm thấy đơn hàng cần xác nhận bưu cục đích");
            return;
        }

        setModalConfirmDestinationOpen(false);

        try {
            setLoading(true);

            const result = await orderApi.setManagerConfirmDestinationOffice(orderId, confirmed);

            if (result.success) {
                message.success(
                    confirmed
                        ? "Đã xác nhận đơn hàng đến bưu cục đích thành công."
                        : "Đã xác nhận bưu cục hiện tại không phải bưu cục đích thành công."
                );
                fetchOrders(page);
            } else {
                message.error(result.message || "Có lỗi khi xác nhận bưu cục hiện tại là bưu cục đích!");
            }
        } catch (err: any) {
            message.error(err.message || "Có lỗi khi xác nhận bưu cục hiện tại là bưu cục đích!");
        } finally {
            setLoading(false);
            setOrderId(null);
        }
    };

    const columns: ColumnsType<ManagerOrderShipment> = [
        {
            title: "Mã đơn",
            dataIndex: "trackingNumber",
            key: "trackingNumber",
            align: "left",
            render: (trackingNumber) =>
                trackingNumber ? (
                    <Tooltip title="Click để xem chi tiết đơn hàng">
                        <span
                            className="navigate-link"
                            onClick={() =>
                                navigate(`/orders/tracking/${trackingNumber}`)
                            }
                        >
                            {trackingNumber}
                        </span>
                    </Tooltip>
                ) : (
                    <Tooltip title="Chưa có mã đơn hàng">
                        <span className="text-muted">Chưa có mã</span>
                    </Tooltip>
                ),
        },
        {
            title: "Trạng thái",
            dataIndex: "status",
            key: "status",
            align: "center",
            render: (status) => translateOrderStatus(status),
        },
        {
            title: "Thanh toán",
            key: "paymentInfo",
            align: "center",
            render: (_, record) => (
                <div>
                    <div>
                        {translateOrderPayerType(record.payer)}
                    </div>
                    <div>
                        <span className="text-muted">({translateOrderPaymentStatus(record.paymentStatus)})</span>
                    </div>
                </div>
            ),
        },
        {
            title: "Phí thu hộ (VNĐ)",
            dataIndex: "cod",
            key: "cod",
            align: "center",
            render: (_, record) => {
                const cod = record.cod || 0;
                const totalFee = record.totalFee || 0;
                const payer = record.payer;
                const fee = payer === "CUSTOMER" ? cod + totalFee : cod;
                return fee.toLocaleString("vi-VN");
            },
        },
        {
            title: "Trọng lượng (Kg)",
            dataIndex: "weight",
            key: "weight",
            align: "center",
            render: (weight) => weight?.toFixed(2) || "0.00",
        },
        {
            title: "Người nhận",
            dataIndex: "recipient",
            key: "recipient",
            align: "left",
            render: (recipient, record) => {
                return (
                    <div>
                        <span className="custom-table-content-strong">{record.recipientName}</span>
                        <br/>
                        {record.recipientPhone}
                        <br/>
                        <span
                            className="custom-table-content-limit">{record.recipientFullAddress || "Chưa có địa chỉ"}</span>
                    </div>
                );
            },
        },
        {
            title: "Bưu cục đích",
            dataIndex: "toOffice",
            key: "toOffice",
            align: "left",
            render: (toOffice, record) => {
                if (!toOffice) return <span className="text-muted">N/A</span>;
                const address = addressMap[`toOffice-${record.id}`];
                return (
                    <div>
                        <span>{toOffice.name}</span> - <span>{toOffice.postalCode}</span>
                        <br/>
                        {toOffice.latitude && toOffice.longitude ? (
                            <Tooltip title="Nhấn để mở Google Maps">
                                <span
                                    className="navigate-link custom-table-content-limit"
                                    onClick={() =>
                                        window.open(
                                            `https://www.google.com/maps?q=${toOffice.latitude},${toOffice.longitude}`,
                                            "_blank",
                                            "noopener,noreferrer"
                                        )
                                    }
                                >
                                    {address || "Chưa có địa chỉ"}
                                </span>
                            </Tooltip>
                        ) : (
                            <span>{address || "Chưa có địa chỉ"}</span>
                        )}
                    </div>
                );
            },
        },
        ...(canDeleteOrdersManagerShipment(shipmentStatus) ? [{
            key: "action",
            align: "center" as const,
            render: (_: any, record: ManagerOrderShipment) => (
                <Tooltip title="Xóa tạm thời">
                    <Button
                        type="link"
                        danger
                        onClick={() => handleTempRemove(record.id)}
                    >
                        <DeleteOutlined/>
                    </Button>
                </Tooltip>
            ),
        }] : []),
        {
            key: "confirmDestination",
            align: "center" as const,
            title: "Xác nhận đích",
            width: 100,
            render: (_: any, record: ManagerOrderShipment) => (
                <Tooltip
                    title={record.pendingDestinationConfirm ? "Xác nhận đơn đã đến bưu cục đích" : "Đơn không cần xác nhận bưu cục đích"}
                    placement="top"
                >
                    <Button
                        type="primary"
                        shape="circle"
                        size="middle"
                        icon={<CheckCircleOutlined style={{ fontSize: '18px' }} />}
                        className="primary-button-circle"
                        disabled={!record.pendingDestinationConfirm}
                        onClick={() => {
                            setOrderId(record.id);
                            setModalConfirmDestinationOpen(true);
                        }}
                    />
                </Tooltip>
            ),
        },
    ];

    const tableData = [
        ...orders.filter(o => !tempRemovedOrders.includes(o.id)),
        ...tempAddedOrders
    ];

    return (
        <div className="list-page-layout">
            <div className="list-page-content">

                <div className="search-filters-container">
                    <Row gutter={16} className="search-filters-row">
                        <Col span={24}>
                            <div className="list-page-actions">
                                <Tooltip title="Tìm theo mã đơn hàng">
                                    <Input
                                        className="search-input"
                                        placeholder="Tìm theo mã đơn hành..."
                                        value={searchText}
                                        onChange={e => setSearchText(e.target.value)}
                                        prefix={<SearchOutlined/>}
                                    />
                                </Tooltip>
                            </div>
                        </Col>
                    </Row>
                </div>

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <ShoppingOutlined className="title-icon"/>
                            Danh sách đơn hàng thuộc chuyến
                        </Title>
                    </Col>

                    <Col>
                        <div className="list-page-actions">
                            <Col>
                                <div className="list-page-actions">
                                    <Space size={8}>
                                        {canEditOrdersManagerShipment(shipmentStatus, shipmentType) && (
                                            <><Tooltip
                                                title="Nhập mã đơn hàng (có thể nhập nhiều mã, cách nhau bằng dấu , ví dụ: UTE001,UTE002,UTE003)">
                                                <Input
                                                    className="search-input shipment-search-input"
                                                    placeholder="Nhập mã đơn để thêm..."
                                                    value={newTrackingNumber}
                                                    onChange={e => setNewTrackingNumber(e.target.value)}
                                                    onPressEnter={handleAddOrder}
                                                    style={{width: 220}}/>
                                            </Tooltip><Tooltip title="Thêm tạm thời">
                                                <Button
                                                    className="warning-button"
                                                    loading={addingLoading}
                                                    icon={<PlusOutlined/>}
                                                    onClick={handleAddOrder}
                                                >
                                                    Thêm
                                                </Button>
                                            </Tooltip></>
                                        )}

                                        {(tempAddedOrders.length > 0 || tempRemovedOrders.length > 0) &&
                                            canEditOrdersManagerShipment(shipmentStatus, shipmentType) && (
                                                <Button
                                                    className="modal-cancel-button"
                                                    icon={<CloseCircleOutlined/>}
                                                    onClick={handleDiscardChanges}
                                                >
                                                    Hủy thay đổi
                                                </Button>
                                            )}

                                        {canEditOrdersManagerShipment(shipmentStatus, shipmentType) && (
                                            <Button
                                                onClick={handleSaveOrders}
                                                className="modal-ok-button"
                                                icon={<SaveOutlined/>}
                                            >
                                                Lưu
                                            </Button>
                                        )}

                                        {canConfirmDestinationOrdersManagerShipment(shipmentStatus) && (
                                            selectedOrderIds.length === 0 ? (
                                                <Button
                                                    onClick={async () => {
                                                        await handleSelectAllFiltered(true);
                                                        setModalConfirmDestinationOfficeOpen(true);
                                                    }}
                                                    className="modal-ok-button"
                                                    icon={<PlayCircleOutlined/>}
                                                >
                                                    Xác nhận tất cả đơn đến bưu cục đích
                                                </Button>
                                            ) : (
                                                <Button
                                                    onClick={() => setModalConfirmDestinationOfficeOpen(true)}
                                                    className="modal-ok-button"
                                                    icon={<PlayCircleOutlined/>}
                                                >
                                                    Xác nhận đến bưu cục đích ({selectedOrderIds.length})
                                                </Button>
                                            )
                                        )}

                                        <Button
                                            onClick={handleExport}
                                            className="success-button"
                                            icon={<FileExcelOutlined/>}
                                            disabled={total === 0}
                                        >
                                            Xuất excel
                                        </Button>
                                    </Space>
                                </div>
                            </Col>

                        </div>
                    </Col>
                </Row>

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>
                    </Col>
                    <Col>
                        <span className="text-muted">Các thay đổi hiện tại chỉ là tạm thời. Nhấn Lưu để xác nhận và hoàn tất</span>
                    </Col>
                </Row>

                <div className="table-container">
                    <Table
                        columns={columns}
                        dataSource={tableData}
                        rowKey="id"
                        scroll={{x: "max-content"}}
                        className="list-page-table"
                        pagination={{
                            current: page,
                            pageSize: limit,
                            total,
                            onChange: setPage,
                        }}
                        loading={loading}
                        rowSelection={shipmentStatus === 'COMPLETED' ? {
                            type: 'checkbox',
                            preserveSelectedRowKeys: false,
                            selectedRowKeys: selectedOrderIds,
                            onChange: (keys) => setSelectedOrderIds(keys as number[]),
                            onSelectAll: (selected) => handleSelectAllFiltered(selected),
                            getCheckboxProps: (record) => ({
                                disabled: !record.pendingDestinationConfirm,
                            }),
                        }: undefined}
                        rowClassName={(record) =>
                            selectedOrderIds.includes(record.id) ? "selectd-checkbox-table-row-selected" : ""
                        }
                    />
                </div>

                {bulkResult && (
                    <BulkResult
                        open={bulkModalOpen}
                        results={bulkResult}
                        onClose={() => setBulkModalOpen(false)}
                        title={title}
                    />
                )}
            </div>

            <ConfirmModal
                title="Xác nhận đơn hàng đã đến bưu cục đích"
                message="Xác nhận đơn hàng này đã đến nơi và đã sẵn sàng để giao hay không?"
                open={modalConfirmDestinationOfficeOpen}
                onOk={() => confirmDestinationOrders(true)}
                onCancel={() => confirmDestinationOrders(false)}
                loading={loading}
            />

            <ConfirmModal
                title="Xác nhận đơn hàng đã đến bưu cục đích"
                message="Xác nhận đơn hàng này đã đến nơi và đã sẵn sàng để giao hay không?"
                open={modalConfirmDestinationOpen}
                onOk={() => confirmDestinationOffice(true)}
                onCancel={() => confirmDestinationOffice(false)}
                loading={loading}
            />
        </div>
    );
};

export default ManagerShipmentOrders;