import React from 'react';
import {Table, Button, Space, Tooltip, Dropdown} from 'antd';
import {DeleteOutlined, DownOutlined, EditOutlined, TeamOutlined} from '@ant-design/icons';
import type {ColumnsType} from 'antd/es/table';
import dayjs from "dayjs";
import type {Role} from "../../../../../types/role.ts";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";

interface RoleTableProps {
    data: Role[];
    loading?: boolean;
    onEdit: (id: number) => void;
    page: number;
    limit: number;
    total: number;
    onPageChange: (page: number, limit?: number) => void;
    onViewUsersByRole: (roleId: number) => void;
    onDelete: (id: number) => void;
}

const RoleTable: React.FC<RoleTableProps> = ({
                                                 data,
                                                 loading = false,
                                                 onEdit,
                                                 page,
                                                 limit,
                                                 total,
                                                 onPageChange,
                                                 onViewUsersByRole,
                                                 onDelete
                                             }) => {

    const canEdit = hasPermissionGroup(['GROUP_USER', 'USER_PERMISSION_GROUP_EDIT']);
    const canDelete = hasPermissionGroup(['GROUP_USER', 'USER_PERMISSION_GROUP_DELETE']);
    const canViewEmployee = hasPermissionGroup(['GROUP_USER', 'USER_PERMISSION_GROUP_USER_VIEW']);

    const columns: ColumnsType<Role> = [
        {title: 'Tên quyền', dataIndex: 'name', key: 'name', align: 'left'},
        {title: 'Mô tả', dataIndex: 'description', key: 'description', align: 'left'},
        {
            title: 'Ngày tạo',
            dataIndex: 'createdAt',
            key: 'createdAt',
            align: 'center',
            render: (text) => text ? dayjs(text).format('HH:mm:ss DD/MM/YYYY') : '-'
        },
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
            render: (_: any, record: Role) => {

                const items = [];

                if (canEdit) {
                    items.push({
                        key: "edit",
                        icon: <EditOutlined/>,
                        label: "Sửa",
                        onClick: () => onEdit(record.id!),
                    });
                }

                if (canDelete) {
                    items.push({
                        key: "delete",
                        icon: <DeleteOutlined/>,
                        label: "Xóa",
                        onClick: () => onDelete(record.id!),
                    });
                }

                return (
                    <Space size="middle">
                        {canViewEmployee && (
                            <Tooltip title="Xem danh sách nhân sự thuộc nhóm quyền">
                                <Button
                                    type="text"
                                    icon={<TeamOutlined style={{color: '#1c3d90'}}/>}
                                    onClick={() => onViewUsersByRole(record.id!)}
                                />
                            </Tooltip>
                        )}

                        <Dropdown menu={{items}} trigger={['click']} disabled={items.length === 0}>
                            <Button className="dropdown-trigger-button">
                                Hành động <DownOutlined/>
                            </Button>
                        </Dropdown>
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

export default RoleTable;