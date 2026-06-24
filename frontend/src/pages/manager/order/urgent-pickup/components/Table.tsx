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
}

const OrderTable: React.FC<Props> = ({
                                         orders,
                                         onConfirm,
                                         page,
                                         limit,
                                         total,
                                         loading,
                                         onPageChange,
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
            align: "center",
            render: (_, record) => {
                return (
                    <Button
                        type="primary"
                        className="primary-button"
                        icon={<CheckCircleOutlined />}
                        onClick={() => onConfirm(record.id)}
                    >
                        Xác nhận
                    </Button>
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
            />
        </div>
    );
}

export default OrderTable;