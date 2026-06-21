import React from 'react';
import {Button, Space, Table} from 'antd';
import {EditOutlined, EyeOutlined, HistoryOutlined} from '@ant-design/icons';
import type {ColumnsType} from 'antd/es/table';
import dayjs from "dayjs";
import type {User} from "../../../../../types/user.ts";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface DataTableProps {
    data: User[];
    loading?: boolean;
    onEdit: (user: User) => void;
    onViewRoles: (id: number) => void;
    onViewLogs: (id: number) => void;
    page: number;
    limit: number;
    total: number;
    onPageChange: (page: number, limit?: number) => void;
}

const DataTable: React.FC<DataTableProps> = ({
                                                 data,
                                                 loading = false,
                                                 onViewRoles,
                                                 onEdit,
                                                 page,
                                                 limit,
                                                 total,
                                                 onPageChange,
                                                 onViewLogs
                                             }) => {

    const canViewWorkHistory = hasPermissionGroup(['GROUP_USER', 'USER_EMPLOYEE_HISTORY_VIEW']);
    const canEdit = hasPermissionGroup(['GROUP_USER', 'USER_EMPLOYEE_EDIT']);

    const columns: ColumnsType<User> = [
        {
            title: 'Mã nhân viên',
            dataIndex: 'code',
            key: 'code',
            align: 'left',
            render: (code, _) => {
                return (
                    <span className="custom-table-content-strong">
                        {code}
                    </span>
                );
            }
        },
        {
            title: 'Họ tên',
            key: 'fullName',
            align: 'left',
            render(text, record) {
                return `${record.lastName} ${record.firstName}`;
            }
        },
        {title: 'Email', dataIndex: 'email', key: 'email', align: 'left'},
        {title: 'Số điện thoại', dataIndex: 'phoneNumber', key: 'phoneNumber', align: 'left'},
        {
            title: 'Cập nhật lần cuối',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            align: 'center',
            render: (text) => text ? dayjs(text).format('HH:mm:ss DD/MM/YYYY') : '-'
        },
        {
            key: 'action',
            align: 'left',
            render: (_: any, record: User) => {

                return (
                    <Space size="small">
                        <Button
                            type="text"
                            icon={<HistoryOutlined style={{color: '#1c3d90'}}/>}
                            onClick={() => onViewLogs(record.id!)}
                            title="Xem lịch sử hoạt động của nhân viên"
                        />
                        {canViewWorkHistory && (
                            <Button
                                type="text"
                                icon={<EyeOutlined style={{color: '#1c3d90'}}/>}
                                onClick={() => onViewRoles(record.id!)}
                                title="Xem lịch sử phân quyền của nhân viên"
                            />
                        )}

                        {canEdit && (
                            <Button
                                type="text"
                                icon={<EditOutlined style={{color: '#1c3d90'}}/>}
                                onClick={() => onEdit(record)}
                                title="Cập nhật thông tin nhân viên"
                            />
                        )}
                    </Space>
                );
            },
        },
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