import React from "react";
import {Button, Dropdown, message, Space, Table, Tooltip} from "antd";
import {
    CloseCircleOutlined,
    DeleteOutlined,
    DownOutlined,
    EditOutlined,
    PlayCircleOutlined,
    PrinterOutlined
} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ColumnsType} from "antd/es/table";
import type {Order} from "../../../../../types/order";
import dayjs from 'dayjs';
import {
    canCancelUserOrder,
    canDeleteUserOrder,
    canEditUserOrder,
    canPrintUserOrder,
    canPublicUserOrder,
    canReadyUserOrder,
    canTransitToOfficeUserOrder,
    translateOrderCodStatus,
    translateOrderPayerType,
    translateOrderPaymentStatus,
    translateOrderPickupType,
    translateOrderStatus
} from "../../../../../utils/orderUtils";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface Props {
    orders: Order[];
    onCancel: (id: number) => void;
    onPublic: (id: number) => void;
    onDelete: (id: number) => void;
    onPrint: (id: number) => void;
    onReady: (id: number) => void;
    onTransitToOffice: (id: number) => void;
    onEdit: (id: number, trackingNumber: string) => void;
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
                                         onPublic,
                                         onDelete,
                                         onPrint,
                                         onEdit,
                                         onReady,
                                         onTransitToOffice,
                                         page,
                                         limit,
                                         total,
                                         loading,
                                         onPageChange,
                                         selectedOrderIds,
                                         setSelectedOrderIds,
                                         onSelectAllFiltered,
                                     }) => {
    const navigate = useNavigate();

    const canPerformBulkAction = hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PRINT_BULK']);

    const tableData = orders.map((o) => ({...o, key: String(o.id)}));

    const rowSelection = canPerformBulkAction ? {
        type: 'checkbox' as const,
        selectedRowKeys: selectedOrderIds,
        onChange: (keys: React.Key[]) => setSelectedOrderIds(keys as number[]),
        onSelectAll: (selected: boolean) => {
            onSelectAllFiltered(selected);
        },
        getCheckboxProps: (record: Order) => ({
            disabled: !record.trackingNumber,
        }),
    } : undefined;

    const columns: ColumnsType<any> = [
        {
            title: "Mã đơn",
            dataIndex: "trackingNumber",
            key: "trackingNumber",
            align: "left",
            render: (trackingNumber, _) => {
                if (!trackingNumber) {
                    return (
                        <span className="text-muted">
              Chưa có mã
            </span>
                    );
                }

                const handleNavigate = () => {
                    if (hasPermissionGroup(['GROUP_USER', 'USER_ORDER_DETAIL'])) {
                        navigate(`/orders/tracking/${trackingNumber}`);
                    } else {
                        message.error("Bạn không có quyền xem chi tiết đơn hàng này.");
                    }
                };

                return (
                    <Tooltip title="Click để xem chi tiết đơn hàng">
            <span
                className="navigate-link"
                onClick={handleNavigate}
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
            title: "Người nhận",
            key: "recipient",
            align: "left",
            render: (_, record) => {

                return (
                    <><span className="long-column">
            <span className="custom-table-content-strong">{record.recipientAddress.name}</span><br/>
                        {record.recipientAddress.phoneNumber}<br/>
            <span className="custom-table-content-limit">{record.recipientAddress.fullAddress}</span>
          </span>
                    </>
                );
            }
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
                                <div key={idx} style={{ marginBottom: '2px' }}>
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
            title: "Thanh toán", key: "status", render: (_, record) =>
                <><span className="custom-table-content-strong">Người thanh toán:</span><br/>
                    {translateOrderPayerType(record.payer)}<br/>
                    <span className="custom-table-content-strong">Trạng thái:</span><br/>
                    {translateOrderPaymentStatus(record.paymentStatus)}</>
        },
        {
            title: "Tổng quan tiền",
            key: "totalMoney",
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
            title: "Người nhận trả",
            key: "codFromRecipient",
            align: "center",
            render: (_, record) => {

                if (record.payer === 'CUSTOMER') {
                    const codCollected = (record.cod || 0) + (record.totalFee || 0);
                    return codCollected.toLocaleString('vi-VN');
                } else {
                    return (record.cod || 0).toLocaleString('vi-VN');
                }
            }
        },
        {
            title: "Người gửi trả",
            key: "senderPaid",
            align: "center",
            render: (_, record) => {

                if (record.payer === 'SHOP') {
                    return (record.totalFee || 0).toLocaleString('vi-VN');
                }

                return 0;
            }
        },
        {
            title: "Còn nợ",
            key: "debt",
            align: "center",
            render: (_, record) => {
                if (record.payer !== 'SHOP') return 0;

                const cod = record.cod || 0;
                const fee = record.totalFee || 0;
                const diff = cod - fee;

                const debt = diff > 0 ? 0 : Math.abs(diff);

                return (
                    <span className={debt > 0 ? "custom-table-content-error" : ""}>
            {debt.toLocaleString('vi-VN')}
          </span>
                );
            }
        },
        {
            title: "COD thu về",
            key: "codCollected",
            align: "center",
            render: (_, record) => {
                if (record.payer !== 'SHOP') {
                    return (record.cod || 0).toLocaleString('vi-VN');
                }

                const cod = record.cod || 0;
                const fee = record.totalFee || 0;
                const codCollected = Math.max(0, cod - fee);

                return (
                    <span className={codCollected > 0 ? "custom-table-content-strong" : ""}>
            {codCollected.toLocaleString('vi-VN')}
          </span>
                );
            }
        },
        {
            title: "Trạng thái COD",
            key: "codStatus",
            align: "center",
            render: (_, record) => translateOrderCodStatus(record.codStatus)
        },
        {
            key: "action",
            align: "center",
            render: (_, record) => {
                const canCancel = canCancelUserOrder(record.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_CANCEL']);
                const canEdit = canEditUserOrder(record.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_EDIT']);
                const canDelete = canDeleteUserOrder(record.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_DELETE_DRAFT']);
                const canPrint = canPrintUserOrder(record.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PRINT_BULK']);
                const canPublic = canPublicUserOrder(record.status) && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_PROCESS']);
                const canReady = canReadyUserOrder(record.status) && record.pickupType === "PICKUP_BY_COURIER" && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_READY']);
                const canTransitToOffice = canTransitToOfficeUserOrder(record.status) && record.pickupType === "AT_OFFICE" && hasPermissionGroup(['GROUP_USER', 'USER_ORDER_TRANSIT_TO_OFFICE']);

                const items = [
                    ...(canPrint ? [{
                        key: "print",
                        icon: <PrinterOutlined/>,
                        label: "In phiếu",
                        onClick: () => onPrint(record.id),
                    }] : []),

                    ...(canReady ? [{
                        key: "ready",
                        icon: <PlayCircleOutlined/>,
                        label: "Sẵn sàng để lấy",
                        onClick: () => onReady(record.id),
                    }] : []),

                    ...(canTransitToOffice ? [{
                        key: "ready",
                        icon: <PlayCircleOutlined/>,
                        label: "Chuyển về bưu cục",
                        onClick: () => onTransitToOffice(record.id),
                    }] : []),

                    ...(canPublic ? [{
                        key: "public",
                        icon: <PlayCircleOutlined/>,
                        label: "Chuyển xử lý",
                        onClick: () => onPublic(record.id),
                    }] : []),

                    ...(canEdit ? [{
                        key: "edit",
                        icon: <EditOutlined/>,
                        label: "Sửa",
                        onClick: () => onEdit(record.id, record.trackingNumber),
                    }] : []),

                    ...(canCancel ? [{
                        key: "cancel",
                        icon: <CloseCircleOutlined/>,
                        label: "Hủy",
                        onClick: () => onCancel(record.id),
                    }] : []),

                    ...(canDelete ? [{
                        key: "delete",
                        icon: <DeleteOutlined/>,
                        label: "Xóa",
                        danger: true,
                        onClick: () => onDelete(record.id),
                    }] : []),
                ];

                return (
                    <Space>
                        {hasPermissionGroup(['GROUP_USER', 'USER_ORDER_DETAIL']) && (
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
                        )}

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
                dataSource={tableData}
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
                rowSelection={rowSelection}
                rowClassName={(record) =>
                    selectedOrderIds.includes(record.id) ? "selectd-checkbox-table-row-selected" : ""
                }
            />
        </div>
    );
}

export default OrderTable;