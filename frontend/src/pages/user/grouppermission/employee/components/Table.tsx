import React from 'react';
import {Switch, Table} from 'antd';
import {CheckOutlined, CloseOutlined} from '@ant-design/icons';
import type {ColumnsType} from 'antd/es/table';
import type {User} from "../../../../../types/user.ts";
import {hasPermissionGroup} from "../../../../../utils/authUtils.ts";
import dayjs from "dayjs";

interface EmployeeTableProps {
    data: User[];
    loading?: boolean;
    onSetActive: (userId: number, isActive: boolean) => void;
    page: number;
    limit: number;
    total: number;
    onPageChange: (page: number, limit?: number) => void;
}

const EmployeeTable: React.FC<EmployeeTableProps> = ({
                                                               data,
                                                               loading = false,
                                                               onSetActive,
                                                               page,
                                                               limit,
                                                               total,
                                                               onPageChange,
                                                           }) => {

    const canAssign = hasPermissionGroup(['GROUP_USER', 'USER_PERMISSION_ASSIGN']);

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
        {title: 'Tên nhân viên', dataIndex: 'fullName', key: 'fullName', align: 'left'},
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
            title: 'Trạng thái',
            dataIndex: 'isActive',
            key: 'isActive',
            align: 'center',
            render: (val: boolean, record: User) => (
                <Switch
                    className={"custom-switch"}
                    checked={val}
                    disabled={!canAssign}
                    onChange={(checked) => onSetActive(record.id, checked)}
                    checkedChildren={<CheckOutlined/>}
                    unCheckedChildren={<CloseOutlined/>}
                />
            ),
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

export default EmployeeTable;