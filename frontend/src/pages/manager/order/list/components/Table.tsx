import React from "react";
import {Button, Dropdown, Space, Table, Tooltip} from "antd";
import {CheckCircleOutlined, CloseCircleOutlined, DownOutlined, EditOutlined, PrinterOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ColumnsType} from "antd/es/table";
import type {Order} from "../../../../../types/order";
import dayjs from 'dayjs';
import {
    canAtOriginOfficeManagerOrder,
    canCancelManagerOrder,
    canConfirmManagerOrder,
    canEditManagerOrder,
    canPrintManagerOrder,
    canReturnedManagerOrder,
    translateOrderCreatorType,
    translateOrderPayerType,
    translateOrderPaymentStatus,
    translateOrderPickupType,
    translateOrderStatus
} from "../../../../../utils/orderUtils";

interface Props {
    orders: Order[];
    onCancel: (id: number) => void;
    onPrint: (id: number) => void;
    onAtOriginOffice: (id: number) => void;
    onConfirm: (id: number) => void;
    onEdit: (id: number, trackingNumber: string) => void;
    onReturned: (id: number) => void;
    page: number;
    limit: number;
    total: number;
    loading: boolean;
    onPageChange: (page: number, limit?: number) => void;
    selectedOrderIds: number[];
    setSelectedOrderIds: React.Dispatch<React.SetStateAction<number[]>>;
    onSelectAllFiltered: (select: boolean) => void;
}

const OrderTable: React.FC<Props> = ({
                                         orders,
                                         onCancel,
                                         onPrint,
                                         onEdit,
                                         onConfirm,
                                         onAtOriginOffice,
                                         onReturned,
                                         page,
                                         limit,
                                         total,
                                         loading,
                                         onPageChange,
                                         selectedOrderIds,
                                         setSelectedOrderIds,
                                         onSelectAllFiltered
                                     }) => {

    const navigate = useNavigate();

    const columns: ColumnsType<any> = [
        {
            title: "Mã đơn",
            dataIndex: "trackingNumber",
            key: "trackingNumber",
            align: "left",
            render: (trackingNumber, _) => {
                if (!trackingNumber) {
                    return (
                        <Tooltip title="Chưa có mã đơn hàng">
              <span className="text-muted">
                Chưa có mã
              </span>
                        </Tooltip>
                    );
                }

                return (
                    <Tooltip title="Click để xem chi tiết đơn hàng">
            <span
                className="navigate-link"
                onClick={() => navigate(`/orders/tracking/${trackingNumber}`)}
            >
              {trackingNumber}
            </span>
                    </Tooltip>
                );
            },
        },
        {
            title: "Trạng thái",
            key: "status",
            align: "center",
            render: (_, record) => translateOrderStatus(record.status)
        },
        {
            title: "Người gửi",
            key: "sender",
            align: "left",
            render: (_, record) => {
                return (
                    <span className="long-column">
            <span className="custom-table-content-strong">{record.senderName}</span><br/>
                        {record.senderPhone}<br/>
            <span className="custom-table-content-limit">{record.senderFullAddress}</span>
          </span>
                );
            }
        },
        {
            title: "Người nhận",
            key: "recipient",
            align: "left",
            render: (_, record) => {
                return (
                    <span className="long-column">
            <span className="custom-table-content-strong">{record.recipientName}</span><br/>
                        {record.recipientPhone}<br/>
            <span className="custom-table-content-limit">{record.recipientFullAddress}</span>
          </span>
                );
            }
        },
        {
            title: "Tổng quan tiền",
            key: "totalMoney",
            align: "left",
            render: (_, record) => (
                <>
                    <span
                        className="custom-table-content-strong">Giá trị đơn:</span> {record.orderValue.toLocaleString('vi-VN')}<br/>
                    <span
                        className="custom-table-content-strong">COD (chưa phí):</span> {record.cod.toLocaleString('vi-VN')}<br/>
                    <span
                        className="custom-table-content-strong">Phí dịch vụ:</span> {record.totalFee.toLocaleString('vi-VN')}
                </>
            )
        },
        {
            title: "Thông tin giao hàng",
            key: "shippingInfo",
            align: "left",
            render: (_, record) => (
                <><span className="custom-table-content-strong">Hình thức lấy hàng:</span><br/>
                    {translateOrderPickupType(record.pickupType)}<br/>
                    <span className="custom-table-content-strong">Dịch vụ giao hàng:</span><br/>
                    {record.serviceTypeName}</>
            )
        },
        {
            title: "Thanh toán", key: "status", render: (_, record) =>
                <><span className="custom-table-content-strong">Người thanh toán:</span><br/>
                    {translateOrderPayerType(record.payer)}<br/>
                    <span className="custom-table-content-strong">Trạng thái:</span><br/>
                    {translateOrderPaymentStatus(record.paymentStatus)}</>
        },
        {
            title: "Khối lượng (Kg)",
            dataIndex: "weight",
            key: "weight",
            align: "center",
            render: (_: any, record: any) => {
                const declared = (record.weight || 0).toFixed(2);
                const adjusted = record.adjustedWeight?.toFixed(2);

                // nếu có chỉnh
                if (record.adjustedWeight != null) {
                    return (
                        <div>
                            <span className="custom-table-content-error">{adjusted}</span><br/>
                            (<span className="text-muted custom-text-removed">{declared}</span>)
                        </div>
                    );
                }

                // nếu không chỉnh
                return <span>{declared}</span>;
            }
        },
        {
            title: "Người tạo đơn hàng",
            dataIndex: "createdByType",
            key: "createdByType",
            align: "center",
            render: (_, record) => (
                <div className="order-creator-cell">
                    <div>{translateOrderCreatorType(record.createdByType)}</div>
                    {(record.employeeCode && record.createdByType !== "USER") ? (
                            <div className="custom-table-content-strong">({record.employeeCode})</div>
                        ) :
                        (<div className="custom-table-content-strong">({record.userCode})</div>
                        )}
                </div>
            ),
        },
        {
            title: "Thời gian",
            key: "shippingInfo",
            align: "left",
            render: (_, record) => {
                const times = [
                    {label: "Thanh toán", value: record.paidAt},
                    {label: "Tạo đơn", value: record.createdAt},
                    {label: "Giao hàng", value: record.deliveriedAt},
                    {label: "Hoàn hàng", value: record.returnedAt},
                ];

                const validTimes = times.filter(t => t.value);

                return (
                    <>
                        {validTimes.length > 0 ? (
                            validTimes.map((t, idx) => (
                                <div key={idx} style={{marginBottom: '2px'}}>
                                    <span className="custom-table-content-strong">{t.label}:{" "}</span>
                                    {dayjs(t.value).format('HH:mm DD/MM/YYYY')}
                                </div>
                            ))
                        ) : (
                            <span className="text-muted">Chưa có thông tin</span>
                        )}
                    </>
                );
            }
        },
        {
            key: "action",
            align: "center",
            render: (_, record) => {
                const canCancel = canCancelManagerOrder(record.status, record.createdByType);
                const canConfirm = canConfirmManagerOrder(record.status, record.pickupType);
                const canEdit = canEditManagerOrder(record.status, record.createdByType);
                const canPrint = canPrintManagerOrder(record.status);
                const canAtOriginOffice = canAtOriginOfficeManagerOrder(record.status) && record.pickupType === 'AT_OFFICE';
                const canReturned = canReturnedManagerOrder(record.status) && record.pickupType === 'PICKUP_BY_COURIER';

                const items = [
                    ...(canPrint ? [{
                        key: "print",
                        icon: <PrinterOutlined/>,
                        label: "In phiếu",
                        onClick: () => onPrint(record.id),
                    }] : []),

                    ...(canConfirm ? [{
                        key: "confirm",
                        icon: <CheckCircleOutlined/>,
                        label: "Xác nhận",
                        onClick: () => onConfirm(record.id),
                    }] : []),

                    ...(canAtOriginOffice ? [{
                        key: "atOrginOffice",
                        icon: <CheckCircleOutlined/>,
                        label: "Đã đến bưu cục",
                        onClick: () => onAtOriginOffice(record.id),
                    }] : []),

                    ...(canEdit ? [{
                        key: "edit",
                        icon: <EditOutlined/>,
                        label: "Sửa",
                        onClick: () => onEdit(record.id, record.trackingNumber),
                    }] : []),

                    ...(canReturned ? [{
                        key: "returned",
                        icon: <CloseCircleOutlined/>,
                        label: "Đã hoàn hàng",
                        onClick: () => onReturned(record.id),
                    }] : []),

                    ...(canCancel ? [{
                        key: "cancel",
                        icon: <CloseCircleOutlined/>,
                        label: "Hủy",
                        onClick: () => onCancel(record.id),
                    }] : []),
                ];

                return (
                    <Space>
                        <Button
                            className="action-button-link"
                            type="link"
                            onClick={() =>
                                record.trackingNumber
                                    ? navigate(`/orders/tracking/${record.trackingNumber}`)
                                    : navigate(`/orders/id/${record.id}`)
                            }
                        >
                            Xem
                        </Button>

                        <Dropdown
                            menu={{items}}
                            trigger={["click"]}
                            disabled={items.length === 0}
                        >
                            <Button className="dropdown-trigger-button">
                                Thêm <DownOutlined/>
                            </Button>
                        </Dropdown>
                    </Space>
                );
            },
        },
    ];

    return (
        <div className="table-container">
            <Table
                columns={columns}
                dataSource={orders}
                loading={loading}
                rowKey="id"
                scroll={{x: "max-content"}}
                className="list-page-table"
                pagination={{
                    current: page,
                    pageSize: limit,
                    total,
                    onChange: (page, pageSize) => onPageChange(page, pageSize)
                }}
                rowSelection={{
                    type: 'checkbox',
                    preserveSelectedRowKeys: false,
                    selectedRowKeys: selectedOrderIds,
                    onChange: (keys) => setSelectedOrderIds(keys as number[]),
                    onSelectAll: (selected) => {
                        onSelectAllFiltered(selected);
                    },
                    getCheckboxProps: (record) => ({
                        disabled: !record.trackingNumber,
                    }),
                }}
                rowClassName={(record) =>
                    selectedOrderIds.includes(record.id) ? "selectd-checkbox-table-row-selected" : ""
                }
            />
        </div>
    );
}

export default OrderTable;