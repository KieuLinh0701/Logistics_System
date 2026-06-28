import React from "react";
import {Button, Table, Tooltip} from "antd";
import {CheckCircleOutlined} from "@ant-design/icons";
import type {ColumnsType} from "antd/es/table";
import type {Order} from "../../../../../types/order";
import dayjs from 'dayjs';

interface Props {
    orders: Order[];
    onConfirm: (id: number) => void;
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
                                         onConfirm,
                                         page,
                                         limit,
                                         total,
                                         loading,
                                         onPageChange,
                                         selectedOrderIds,
                                         setSelectedOrderIds,
                                         onSelectAllFiltered
                                     }) => {

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
                    <span className="custom-table-content-strong">
                      {trackingNumber}
                    </span>
                );
            },
        },
        {
            title: "Địa chỉ lấy hàng",
            key: "sender",
            align: "left",
            render: (_, record) => {
                return (
                    <span>{record.senderFullAddress}</span>
                );
            }
        },
        {
            title: "Thời gian sẵn sàng",
            key: "readyForPickupAt",
            dataIndex: "readyForPickupAt",
            align: "left",
            render: (value) => {
                return value ? (
                    <div>
                        {dayjs(value).format('HH:mm:ss DD/MM/YYYY')}
                    </div>
                ) : (
                    <span className="text-muted">Chưa có thông tin</span>
                );
            }
        },
        {
            key: "action",
            title: "Thao tác",
            align: "center",
            width: 80,
            render: (_, record) => {
                return (
                    <Tooltip title="Xác nhận đơn hàng" placement="top">
                        <Button
                            type="primary"
                            shape="circle"
                            size="middle"
                            icon={<CheckCircleOutlined style={{fontSize: '18px'}}/>}
                            className="primary-button-circle"
                            onClick={() => onConfirm(record.id)}
                        />
                    </Tooltip>
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