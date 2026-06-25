import React from 'react';
import {Button, Space, Table, Tooltip} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import dayjs from 'dayjs';
import {translateShipmentStatus} from '../../../../utils/shipmentUtils';
import type {ManagerShipment} from '../../../../types/shipment';

interface TableProps {
    data: ManagerShipment[];
    currentPage: number;
    pageSize: number;
    total: number;
    loading?: boolean;
    onDetail: (value: ManagerShipment) => void;
    onPageChange: (page: number, pageSize?: number) => void;
}

const RequestTable: React.FC<TableProps> = ({
                                                data,
                                                currentPage,
                                                pageSize,
                                                total,
                                                loading = false,
                                                onDetail,
                                                onPageChange,
                                            }) => {

    const columns: ColumnsType<ManagerShipment> = [
        {
            title: 'Mã chuyến',
            dataIndex: 'code',
            key: 'code',
            align: 'center',
            render: (_, record) => {
                return (
                    <Tooltip title="Click để xem danh sách đơn hàng của chuyến hàng">
            <span
                className="navigate-link"
                onClick={() => onDetail(record)}
            >
              {record.code}
            </span>
                    </Tooltip>
                );
            }
        },
        {
            title: 'Trạng thái',
            dataIndex: 'status',
            key: 'status',
            align: 'center',
            render: (status) => translateShipmentStatus(status)
        },
        {
            title: 'Người tạo chuyến',
            dataIndex: 'createdBy',
            key: 'createdBy',
            align: 'left',
            render: (_, record) => {
                const createdBy = record.createdBy;

                if (!createdBy) {
                    return <span className="text-muted">N/A</span>;
                }

                return (
                    <div>
            <span className="custom-table-content-strong">
              {createdBy.lastName} {createdBy.firstName} - {createdBy.code}
            </span><br/>
                        <span>
              {createdBy.phoneNumber}
            </span><br/>
                        <span>
              {createdBy.email}
            </span>
                    </div>
                );
            }
        },
        {
            title: 'Thời gian',
            key: 'time',
            align: 'left',
            render: (_, record) => {
                const createdAt = record.createdAt
                    ? dayjs(record.createdAt).format('HH:mm:ss DD/MM/YYYY')
                    : null;

                const updatedAt = record.updatedAt
                    ? dayjs(record.updatedAt).format('HH:mm:ss DD/MM/YYYY')
                    : null;

                const startTime = record.startTime
                    ? dayjs(record.startTime).format('HH:mm:ss DD/MM/YYYY')
                    : null;

                const endTime = record.endTime
                    ? dayjs(record.endTime).format('HH:mm:ss DD/MM/YYYY')
                    : null;

                if (!createdAt && !startTime && !endTime) {
                    return <span className="text-muted">N/A</span>;
                }

                return (
                    <div>
                        {startTime && (
                            <div>
                <span className="custom-table-content-strong">
                  Bắt đầu:{" "}
                </span>
                                {startTime}
                            </div>
                        )}
                        {endTime && (
                            <div>
                <span className="custom-table-content-strong">
                  Kết thúc:{" "}
                </span>
                                {endTime}
                            </div>
                        )}
                        {startTime && (
                            <hr className="separator"/>
                        )}
                        {createdAt && (
                            <div>
                <span className="custom-table-content-strong">
                  Tạo mới:{" "}
                </span>
                                {createdAt}
                            </div>
                        )}
                        {updatedAt && (
                            <div>
                <span className="custom-table-content-strong">
                  Cập nhật:{" "}
                </span>
                                {updatedAt}
                            </div>
                        )}
                    </div>
                );
            }
        },
        {
            key: "action",
            align: "left",
            render: (_, record: ManagerShipment) => {

                return (
                    <Space>
                        <Button
                            className="action-button-link"
                            type="link"
                            onClick={() => onDetail(record)}
                        >
                            DS đơn hàng
                        </Button>
                    </Space>
                );
            },
        },
    ];

    const tableData = data.map((p, index) => ({
        ...p,
        key: String(index + 1 + (currentPage - 1) * pageSize),
    }));

    return (
        <div className="table-container">
            <Table
                columns={columns}
                dataSource={tableData}
                rowKey="id"
                scroll={{x: "max-content"}}
                className="list-page-table"
                pagination={{
                    current: currentPage,
                    pageSize,
                    total,
                    onChange: onPageChange,
                }}
                loading={loading}
            />
        </div>
    );
}
    ;

    export default RequestTable;