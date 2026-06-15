import React from 'react';
import {Table, Tag} from 'antd';
import type {ColumnsType} from 'antd/es/table';
import dayjs from "dayjs";
import type {ShopWorkHistory} from "../../../../../types/shopWorkHistory.ts";

interface DataTableProps {
    data: ShopWorkHistory[];
    loading?: boolean;
    page: number;
    limit: number;
    total: number;
    onPageChange: (page: number, limit?: number) => void;
}

const DataTable: React.FC<DataTableProps> = ({
                                                 data,
                                                 loading = false,
                                                 page,
                                                 limit,
                                                 total,
                                                 onPageChange,
                                             }) => {

    const columns: ColumnsType<ShopWorkHistory> = [
        {title: 'Vai trò', dataIndex: 'roleName', key: 'roleName', align: 'left'},
        {
            title: 'Trạng thái',
            dataIndex: 'isCurrent',
            key: 'isCurrent',
            align: 'center',
            render: (isCurrent: boolean) => (
                <Tag color={isCurrent ? 'green' : 'default'}>
                    {isCurrent ? 'Đang làm' : 'Đã nghỉ'}
                </Tag>
            )
        },
        {
            title: 'Ngày vào',
            dataIndex: 'joinedAt',
            key: 'joinedAt',
            align: 'center',
            render: (text) => text ? dayjs(text).format('HH:mm:ss DD/MM/YYYY') : '-'
        },
        {
            title: 'Ngày nghỉ',
            dataIndex: 'leftAt',
            key: 'leftAt',
            align: 'center',
            render: (text) => text ? dayjs(text).format('HH:mm:ss DD/MM/YYYY') : '-'
        },
        {title: 'Ghi chú', dataIndex: 'note', key: 'note', align: 'left'},
    ];

    const tableData = data.map((p) => ({...p, key: p.id}));

    return (
        <div className="table-container">
            <Table
                columns={columns}
                dataSource={tableData}
                rowKey="key"
                loading={loading}
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
};

export default DataTable;