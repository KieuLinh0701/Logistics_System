import React, {useMemo} from "react";
import {Empty, Table} from "antd";
import type {ColumnsType} from "antd/es/table";
import type {AdminUser} from "../../../../types/user";
import UsersActions from "./UsersActions.tsx";
import {translateRoleName} from "../../../../utils/roleUtils";
import "../UsersTable.css";

interface UsersTableProps {
    data: AdminUser[];
    loading: boolean;
    page: number;
    pageSize: number;
    total: number;
    onPageChange: (page: number, size?: number) => void;
    onEdit: (u: AdminUser) => void;
    onDelete: (id: number) => void;
    roles?: Array<{ id: number; name: string }>;
    actionLoadingById?: Record<number, boolean>;
    onViewLogs: (id: number) => void;
}

const UsersTable: React.FC<UsersTableProps> = ({
                                                   data,
                                                   loading,
                                                   page,
                                                   pageSize,
                                                   total,
                                                   onPageChange,
                                                   onEdit,
                                                   onDelete,
                                                   roles = [],
                                                   actionLoadingById = {},
                                                   onViewLogs
                                               }) => {
    const columns: ColumnsType<AdminUser> = useMemo(() => [
        {title: 'Email', dataIndex: 'email'},
        {title: 'Họ tên', render: (_, record) => ((record.firstName || '') + ' ' + (record.lastName || '')).trim()},
        {title: 'SĐT', dataIndex: 'phoneNumber'},
        {
            title: 'Vai trò', dataIndex: 'role', render: (r: string, rec) => {
                // prefer explicit roles names from record, else map roleIds via `roles` prop, else fallback to record.role or raw value
                let rolesSource: string[] = [];
                if (rec.roles && rec.roles.length) rolesSource = rec.roles;
                else if (rec.rolesIds && rec.rolesIds.length) {
                    rolesSource = rec.rolesIds.map(id => {
                        const found = roles.find(rr => rr.id === id);
                        return found ? found.name : String(id);
                    });
                } else if (rec.role) rolesSource = [rec.role];
                else if (rec.roleId) {
                    const found = roles.find(rr => rr.id === rec.roleId);
                    rolesSource = [found ? found.name : String(rec.roleId)];
                } else if (r) rolesSource = [r];

                return rolesSource.map((rv) => translateRoleName(rv || '')).join(', ');
            }
        },
        {title: 'Trạng thái', dataIndex: 'isActive', render: (v: boolean) => v ? 'Hoạt động' : 'Khóa'},
        {
            title: '',
            key: 'actions',
            className: 'admin-actions-column',
            render: (_, record) => (
                <UsersActions record={record} onEdit={onEdit} onDelete={onDelete}
                              loading={!!actionLoadingById[record.id]} onViewLogs={onViewLogs}/>
            ),
            width: 120,
        }
    ], [onEdit, onDelete, actionLoadingById]);

    return (
        <div className="admin-users-table">
            <Table
                rowKey="id"
                columns={columns}
                dataSource={data}
                loading={loading}
                pagination={{current: page, pageSize, total, onChange: onPageChange}}
                locale={{emptyText: <Empty description="Không có người dùng"/>}}
            />
        </div>
    );
};

export default UsersTable;
