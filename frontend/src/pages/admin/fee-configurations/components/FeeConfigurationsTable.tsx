import React from "react";
import { Dropdown, Modal, Space, Table } from "antd";
import { DeleteOutlined, DownOutlined, EditOutlined } from "@ant-design/icons";
import type { ColumnsType } from "antd/es/table";
import type { FeeConfiguration } from "../../../../types/feeConfiguration";
import type { FeeConfigurationsTableProps } from "../../../../types/feeConfiguration";

const FeeConfigurationsTable: React.FC<FeeConfigurationsTableProps> = ({
  loading,
  rows,
  onView,
  onEdit,
  onDelete,
  feeTypeLabel,
}) => {
  const columns: ColumnsType<FeeConfiguration> = [
    {
      title: "Loại phí",
      dataIndex: "feeType",
      key: "feeType",
      render: (value: string) => feeTypeLabel(value),
    },
    {
      title: "Loại dịch vụ",
      dataIndex: "serviceTypeName",
      key: "serviceTypeName",
      render: (value?: string) => value || "Tất cả",
    },
    {
      title: "Cách tính",
      dataIndex: "calculationType",
      key: "calculationType",
      render: (value: string) =>
        value === "PERCENTAGE" ? "Phần trăm" : "Cố định",
    },
    {
      title: "Giá trị",
      key: "feeValue",
      render: (_, row) =>
        row.calculationType === "PERCENTAGE"
          ? `${row.feeValue}%`
          : `${row.feeValue.toLocaleString("vi-VN")}đ`,
    },
    {
      title: "Áp dụng từ",
      dataIndex: "minOrderFee",
      key: "minOrderFee",
      render: (value?: number) =>
        value !== undefined && value !== null
          ? `${value.toLocaleString("vi-VN")}đ`
          : "-",
    },
    {
      title: "Áp dụng đến",
      dataIndex: "maxOrderFee",
      key: "maxOrderFee",
      render: (value?: number) =>
        value !== undefined && value !== null
          ? `${value.toLocaleString("vi-VN")}đ`
          : "-",
    },
    {
      title: "Trạng thái",
      dataIndex: "active",
      key: "active",
      render: (value: boolean) => (value ? "Hoạt động" : "Tạm dừng"),
    },
    {
      title: "",
      key: "actions",
      fixed: "right",
      width: 140,
      render: (_, row) => (
        <Space>
          <span
            onClick={() => onView(row)}
            style={{ color: "#52c41a", cursor: "pointer", display: "inline-flex", alignItems: "center", gap: 6 }}
            title="Xem"
          >
            Xem
          </span>
          <Dropdown
            menu={{
              items: [
                { key: "edit", icon: <EditOutlined />, label: "Sửa" },
                { key: "delete", icon: <DeleteOutlined />, label: "Xóa" },
              ],
              onClick: ({ key }) => {
                if (key === "edit") onEdit(row);
                if (key === "delete") {
                  Modal.confirm({
                    title: "Xóa cấu hình này?",
                    okText: "Xóa",
                    cancelText: "Hủy",
                    okButtonProps: { danger: true },
                    onOk: () => onDelete(row.id),
                  });
                }
              },
            }}
            trigger={["click"]}
          >
            <button type="button" className="dropdown-trigger-button admin-actions-trigger">
              Thêm <DownOutlined />
            </button>
          </Dropdown>
        </Space>
      ),
    },
  ];

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns}
      dataSource={rows}
      pagination={false}
      scroll={{ x: 1100 }}
    />
  );
};

export default FeeConfigurationsTable;