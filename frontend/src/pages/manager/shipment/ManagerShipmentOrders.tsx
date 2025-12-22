import React, { useEffect, useRef, useState } from "react";
import { Table, Input, Button, message, Tooltip, Space, Row, Col, Tag } from "antd";
import { SearchOutlined, DeleteOutlined, ShoppingOutlined } from "@ant-design/icons";
import { useParams, useNavigate, useSearchParams } from "react-router-dom";
import type { ManagerOrderShipment } from "../../../types/shipment";
import type { BulkResponse } from "../../../types/response";
import shipmentApi from "../../../api/shipmentApi";
import shipmentOrderApi from "../../../api/shipmentOrderApi";
import BulkResult from "./components/BulkResult";
import { translateOrderPayerType, translateOrderPaymentStatus, translateOrderStatus } from "../../../utils/orderUtils";
import type { ColumnsType } from "antd/es/table";
import locationApi from "../../../api/locationApi";
import Title from "antd/es/typography/Title";
import "./ManagerShipments.css"

const ManagerShipmentOrders: React.FC = () => {
    const { shipmentId } = useParams<{ shipmentId: string }>();
    const latestRequestRef = useRef(0);
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();

    const [orders, setOrders] = useState<ManagerOrderShipment[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const [limit, setLimit] = useState(10);
    const [total, setTotal] = useState(0);
    const [searchText, setSearchText] = useState("");

    const [tempAddedOrders, setTempAddedOrders] = useState<ManagerOrderShipment[]>([]);
    const [tempRemovedOrders, setTempRemovedOrders] = useState<number[]>([]);
    const [newTrackingNumber, setNewTrackingNumber] = useState("");
    const [addingLoading, setAddingLoading] = useState(false);

    const [bulkModalOpen, setBulkModalOpen] = useState(false);
    const [bulkResult, setBulkResult] = useState<BulkResponse<ManagerOrderShipment>>();

    const [addressMap, setAddressMap] = useState<Record<string, string>>({});

    const updateURL = () => {
        const params: any = {};

        if (searchText) params.search = searchText;
        if (page) params.page = page;

        setSearchParams(params, { replace: true });
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
        setLoading(true);
        try {
            const res = await shipmentApi.getManagerOrdersByShipmentId(Number(shipmentId), {
                page: currentPage,
                limit,
                search: searchText,
            });
            if (res.success && res.data) {
                setOrders(res.data.list || []);
                setTotal(res.data.pagination?.total || 0);
            } else {
                message.error(res.message || "Lỗi khi lấy đơn hàng");
            }
        } catch (error: any) {
            message.error(error.message || "Lỗi khi lấy đơn hàng");
        } finally {
            setLoading(false);
        }
    };

    const formatAddress = async (
        key: string,
        detail?: string,
        wardCode?: number,
        cityCode?: number
    ) => {
        try {
            const cityName = cityCode
                ? await locationApi.getCityNameByCode(cityCode)
                : "";
            const wardName =
                cityCode && wardCode
                    ? await locationApi.getWardNameByCode(cityCode, wardCode)
                    : "";

            setAddressMap(prev => ({
                ...prev,
                [key]: [detail, wardName, cityName].filter(Boolean).join(", "),
            }));
        } catch (error) {
            setAddressMap(prev => ({
                ...prev,
                [key]: detail || "",
            }));
        }
    };

    useEffect(() => {
        [...orders, ...tempAddedOrders].forEach(o => {
            if (o.recipient) {
                formatAddress(
                    `recipient-${o.id}`,
                    o.recipient.detail,
                    o.recipient.wardCode,
                    o.recipient.cityCode
                );
            }
            if (o.toOffice) {
                formatAddress(
                    `toOffice-${o.id}`,
                    o.toOffice.detail,
                    o.toOffice.wardCode,
                    o.toOffice.cityCode
                );
            }
        });
    }, [orders, tempAddedOrders]);

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
                const address = addressMap[`recipient-${record.id}`];
                return (
                    <div>
                        <span className="custom-table-content-strong">{recipient?.name}</span>
                        <br />
                        {recipient?.phone}
                        <br />
                        <span className="custom-table-content-limit">{address || "Chưa có địa chỉ"}</span>
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
                        <br />
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
        {
            key: "action",
            align: "center",
            render: (_, record) => (
                <Tooltip title="Xóa tạm thời">
                    <Button
                        type="link"
                        danger
                        onClick={() => handleTempRemove(record.id)}
                    >
                        <DeleteOutlined />
                    </Button>
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
                                        prefix={<SearchOutlined />}
                                    />
                                </Tooltip>
                            </div>
                        </Col>
                    </Row>
                </div>

                <Row className="list-page-header" justify="space-between" align="middle">
                    <Col>
                        <Title level={3} className="list-page-title-main">
                            <ShoppingOutlined className="title-icon" />
                            Danh sách đơn hàng thuộc chuyến
                        </Title>
                    </Col>

                    <Col>
                        <div className="list-page-actions">
                            <Col>
                                <div className="list-page-actions">
                                    <Space size={8}>
                                        <Tooltip title="Nhập mã đơn hàng (có thể nhập nhiều mã, cách nhau bằng dấu , ví dụ: UTE001,UTE002,UTE003)">
                                            <Input
                                                className="search-input shipment-search-input"
                                                placeholder="Nhập mã đơn để thêm..."
                                                value={newTrackingNumber}
                                                onChange={e => setNewTrackingNumber(e.target.value)}
                                                onPressEnter={handleAddOrder}
                                                style={{ width: 220 }}
                                            />
                                        </Tooltip>
                                        <Tooltip title="Thêm tạm thời">
                                            <Button
                                                className="warning-button"
                                                loading={addingLoading}
                                                onClick={handleAddOrder}
                                            >
                                                Thêm
                                            </Button>
                                        </Tooltip>

                                        {(tempAddedOrders.length > 0 || tempRemovedOrders.length > 0) && (
                                            <Button
                                                className="modal-cancel-button"
                                                onClick={handleDiscardChanges}
                                            >
                                                Hủy thay đổi
                                            </Button>
                                        )}

                                        <Button
                                            onClick={handleSaveOrders}
                                            className="modal-ok-button"
                                        >
                                            Lưu
                                        </Button>
                                    </Space>
                                </div>
                            </Col>

                        </div>
                    </Col>
                </Row>

                <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>

                <div className="table-container">
                    <Table
                        columns={columns}
                        dataSource={tableData}
                        rowKey="id"
                        scroll={{ x: "max-content" }}
                        className="list-page-table"
                        pagination={{
                            current: page,
                            pageSize: limit,
                            total,
                            onChange: setPage,
                        }}
                        loading={loading}
                    />
                </div>

                {bulkResult && (
                    <BulkResult
                        open={bulkModalOpen}
                        results={bulkResult}
                        onClose={() => setBulkModalOpen(false)}
                    />
                )}
            </div>
        </div>
    );
};

export default ManagerShipmentOrders;