import React from 'react';
import {Table, Tag} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import dayjs from 'dayjs';
import type {AuditLog} from "../../../../types/auditLog.ts";
import {translateAuditLogAction, translateAuditLogStatus} from "../../../../utils/auditLogUtils.ts";
import {translateEntityType} from "../../../../utils/entityTypeUtils.ts";

interface AuditLogTableProps {
    data: AuditLog[];
    page: number;
    limit: number;
    total: number;
    loading: boolean;
    onPageChange: (page: number, limit?: number) => void;
}

const AuditLogTable: React.FC<AuditLogTableProps> = ({
                                                         data,
                                                         page,
                                                         limit,
                                                         total,
                                                         loading,
                                                         onPageChange,
                                                     }) => {

    const columns: ColumnsType<AuditLog> = [
        {
            title: "Thời gian",
            dataIndex: "createdAt",
            key: "createdAt",
            align: "center",
            width: 150,
            render: (date) => dayjs(date).format("HH:mm:ss DD/MM/YYYY"),
        },
        {
            title: "Nhân viên",
            key: "user",
            align: "left",
            render: (_, record) => (
                <div>
                    <div>{record.user.fullName}</div>
                    <div>{record.user.phoneNumber}</div>
                </div>
            ),
        },
        {
            title: "Đối tượng",
            dataIndex: "entity",
            key: "entity",
            align: "center",
            render: (entity) => {
                return translateEntityType(entity);
            },
        },
        {
            title: "Mã đối tượng",
            dataIndex: "entityId",
            key: "entityId",
            align: "center",
            render: (entityId) => <span className="custom-table-content-strong">{entityId}</span>,
        },
        {
            title: "Hành động",
            dataIndex: "action",
            key: "action",
            align: "center",
            render: (action) => {
                return translateAuditLogAction(action);
            },
        },
        {
            title: "Mô tả",
            dataIndex: "description",
            key: "description",
            align: "left",
        },
        {
            title: "Trạng thái",
            dataIndex: "status",
            key: "status",
            align: "center",
            render: (status) => (
                <Tag color={status === 'SUCCESS' ? 'green' : 'red'}>{translateAuditLogStatus(status)}</Tag>
            ),
        },
    ];

    const tableData = data.map((p, index) => ({
        ...p,
        key: String(index + 1 + (page - 1) * limit),
    }));

    return (
        <div className="table-container">
            <Table
                columns={columns}
                dataSource={tableData}
                rowKey="key"
                scroll={{x: "max-content"}}
                className="list-page-table"
                loading={loading}
                pagination={{
                    current: page,
                    pageSize: limit,
                    total,
                    onChange: (page, pageSize) => onPageChange(page, pageSize)
                }}
            />
        </div>
    );
};

export default AuditLogTable;