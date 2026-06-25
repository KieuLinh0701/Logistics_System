import React, {useEffect, useRef, useState} from "react";
import {Button, Col, Input, message, Row, Space, Table, Tag, Tooltip} from "antd";
import {FileExcelOutlined, SearchOutlined, ShoppingOutlined} from "@ant-design/icons";
import {useNavigate, useParams, useSearchParams} from "react-router-dom";
import shipmentApi from "../../../api/shipmentApi";
import {translateOrderPayerType, translateOrderPaymentStatus, translateOrderStatus} from "../../../utils/orderUtils";
import type {ColumnsType} from "antd/es/table";
import Title from "antd/es/typography/Title";
import "./ShipperShipmentHistory.css"
import type {Order} from "../../../types/order.ts";

const ShipperShipmentOrders: React.FC = () => {
    const {shipmentId} = useParams<{ shipmentId: string }>();
    const [searchParams, setSearchParams] = useSearchParams();
    const navigate = useNavigate();

    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(false);
    const [page, setPage] = useState(1);
    const limit = 10;
    const [total, setTotal] = useState(0);
    const [searchText, setSearchText] = useState("");
    const latestRequestRef = useRef(0);

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

            const res = await shipmentApi.getShipperOrdersByShipmentId(Number(shipmentId), {
                page: currentPage,
                limit,
                search: searchText,
            });

            if (requestId !== latestRequestRef.current) return;

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

    const handleExport = async () => {
        try {
            const result = await shipmentApi.exportShipperOrdersByShipmentId(
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

    const columns: ColumnsType<Order> = [
        {
            title: "Mã đơn",
            dataIndex: "trackingNumber",
            key: "trackingNumber",
            align: "left",
            render: (trackingNumber, record) =>
                trackingNumber ? (
                    <Tooltip title="Click để xem chi tiết đơn hàng">
                        <span
                            className="navigate-link"
                            onClick={() =>
                                navigate(`/shipper/orders/${record.id}`)
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
            title: "Người gửi",
            dataIndex: "sender",
            key: "sender",
            align: "left",
            render: (sender, record) => {
                return (
                    <div>
                        <span className="custom-table-content-strong">{record.senderName}</span>
                        <br/>
                        {record.senderPhone}
                        <br/>
                        <span
                            className="custom-table-content-limit">{record.senderFullAddress || "Chưa có địa chỉ"}</span>
                    </div>
                );
            },
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
                                        placeholder="Tìm theo mã đơn hàng..."
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
                    <Tag className="list-page-tag">Kết quả trả về: {total} đơn hàng</Tag>
                </Row>

                <div className="table-container">
                    <Table
                        columns={columns}
                        dataSource={orders}
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
                    />
                </div>
            </div>
        </div>
    );
};

export default ShipperShipmentOrders;