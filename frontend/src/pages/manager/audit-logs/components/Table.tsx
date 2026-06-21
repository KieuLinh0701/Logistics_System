import React from 'react';
import {Table, Tag} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import dayjs from 'dayjs';
import type {AuditLog} from "../../../../../types/auditLog.ts";
import {
    translateAuditLogAction,
    translateAuditLogEntity,
    translateAuditLogStatus
} from "../../../../../utils/auditLogUtils.ts";

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
            title: "Đối tượng",
            dataIndex: "entity",
            key: "entity",
            align: "center",
            render: (entity) => {
                return translateAuditLogEntity(entity);
            },
        },
        {
            title: "Mã đối tượng",
            dataIndex: "id",
            key: "id",
            align: "center",
            render: (id) => <span className="custom-table-content-strong">{id}</span>,
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