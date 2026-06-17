import React, { useMemo } from "react";
import { Dropdown, Modal, Table } from "antd";
import { DownOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";
import type { AdminServiceType } from "../../../../types/serviceType";

interface ServiceTypesTableProps {
  loading: boolean;
  rows: AdminServiceType[];
  page: number;
  pageSize: number;
  total: number;
  onPageChange: (page: number, pageSize?: number) => void;
  onEdit: (record: AdminServiceType) => void;
  onDelete: (id: number) => void;
}

const ServiceTypesTable: React.FC<ServiceTypesTableProps> = ({
  loading,
  rows,
  page,
  pageSize,
  total,
  onPageChange,
  onEdit,
  onDelete,
}) => {
  const columns = useMemo(
    () => [
      { title: "Tên dịch vụ", dataIndex: "name" },
      { title: "Thời gian giao", dataIndex: "deliveryTime" },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string | undefined) => (v === "ACTIVE" ? "Hoạt động" : v === "INACTIVE" ? "Tạm ngưng" : v),
      },
      { title: "Mô tả", dataIndex: "description" },
      {
        title: "",
        render: (_: unknown, record: AdminServiceType) => (
          <Dropdown
            menu={{
              items: [
                { key: "edit", icon: <EditOutlined />, label: "Sửa" },
                { key: "delete", icon: <DeleteOutlined />, label: "Xóa" },
              ],
              onClick: ({ key }) => {
                if (key === "edit") onEdit(record);
                if (key === "delete") {
                  Modal.confirm({
                    title: "Xóa loại dịch vụ này?",
                    okText: "Xóa",
                    cancelText: "Hủy",
                    okButtonProps: { danger: true },
                    onOk: () => onDelete(record.id),
                  });
                }
              },
            }}
            trigger={["click"]}
            placement="bottomRight"
          >
            <button
              type="button"
              className="dropdown-trigger-button"
              style={{
                color: "#000",
                border: "1px solid #000",
                padding: "6px 10px",
                borderRadius: 6,
                display: "inline-flex",
                alignItems: "center",
                gap: 6,
                background: "transparent",
              }}
            >
              Thêm <DownOutlined />
            </button>
          </Dropdown>
        ),
      },
    ],
    [onDelete, onEdit]
  );

  return (
    <Table
      rowKey="id"
      loading={loading}
      columns={columns as any}
      dataSource={rows}
      pagination={{
        current: page,
        pageSize,
        total,
        onChange: onPageChange,
      }}
    />
  );
};

export default ServiceTypesTable;
