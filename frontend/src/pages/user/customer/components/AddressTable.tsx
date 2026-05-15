import React from "react";
import { Table, Button, Dropdown } from "antd";
import { EditOutlined, DeleteOutlined, DownOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import type { Address } from "../../../../types/address.ts";
import type { RecipientAddressWithStats } from "../../../../types/recipientAddress.ts";

interface AddressTableProps {
    data: RecipientAddressWithStats[];
    loading?: boolean;
    onEdit: (address: Address) => void;
    onDelete: (addressId: number) => void;
    page: number;
    limit: number;
    total: number;
    onPageChange: (page: number, limit?: number) => void;
}

const AddressTable: React.FC<AddressTableProps> = ({
                                                       data,
                                                       loading = false,
                                                       onEdit,
                                                       onDelete,
                                                       page,
                                                       limit,
                                                       total,
                                                       onPageChange,
                                                   }) => {
    const columns: ColumnsType<RecipientAddressWithStats> = [
        {
            title: "Tên",
            key: "name",
            align: "left",
            render: (_, record) => record.address.name,
        },
        {
            title: "Số điện thoại",
            key: "phoneNumber",
            align: "left",
            render: (_, record) => record.address.phoneNumber,
        },
        {
            title: "Địa chỉ",
            key: "detail",
            align: "left",
            render: (_, record) => {
                const addr = record.address;

                return (
                    <div>
                        <span>{addr.fullAddress}</span>
                    </div>
                );
            },
        },
        {
            title: "Tổng đơn",
            key: "totalOrders",
            align: "left",
            render: (_, record) => record.recipientStats.totalSystemOrders,
        },
        {
            title: "Tỉ lệ thành công",
            key: "successRate",
            align: "left",
            render: (_, record) => `${record.recipientStats.successRate}%`,
        },
        {
            title: "Tỉ lệ hoàn hàng",
            key: "returnedRate",
            align: "left",
            render: (_, record) => `${record.recipientStats.returnedRate}%`,
        },
        {
            key: "action",
            align: "left",
            render: (_, record) => {
                const addr = record.address;

                const items = [
                    {
                        key: "edit",
                        icon: <EditOutlined />,
                        label: "Sửa",
                        onClick: () => onEdit(addr),
                    },
                    {
                        key: "delete",
                        icon: <DeleteOutlined />,
                        label: "Xóa",
                        onClick: () => onDelete(addr.id!),
                    },
                ];

                return (
                    <Dropdown menu={{ items }} trigger={["click"]}>
                        <Button className="dropdown-trigger-button">
                            Hành động <DownOutlined />
                        </Button>
                    </Dropdown>
                );
            },
        },
    ];

    const tableData = data.map((p, index) => ({
        ...p,
        key: p.address.id ?? index,
    }));

    return (
        <div className="table-container">
            <Table
                columns={columns}
                dataSource={tableData}
                rowKey="key"
                loading={loading}
                scroll={{ x: "max-content" }}
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

export default AddressTable;