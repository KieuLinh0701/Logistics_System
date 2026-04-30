import React, { useMemo } from "react";
import { Dropdown, Modal, Table } from "antd";
import { DeleteOutlined, DownOutlined, EditOutlined } from "@ant-design/icons";
import type { AdminVehicle } from "../../../../types/vehicle";

interface VehiclesTableProps {
  loading: boolean;
  rows: AdminVehicle[];
  page: number;
  pageSize: number;
  total: number;
  typeText: (value?: string) => string;
  statusText: (value?: string) => string;
  onPageChange: (page: number, pageSize?: number) => void;
  onView: (record: AdminVehicle) => void;
  onEdit: (record: AdminVehicle) => void;
  onDelete: (id: number) => void;
}

const VehiclesTable: React.FC<VehiclesTableProps> = ({
  loading,
  rows,
  page,
  pageSize,
  total,
  typeText,
  statusText,
  onPageChange,
  onView,
  onEdit,
  onDelete,
}) => {
  const columns = useMemo(
    () => [
      { title: "Biển số xe", dataIndex: "licensePlate", render: (v: string) => v || "-" },
      {
        title: "Loại xe",
        dataIndex: "type",
        render: (v: string | undefined) => typeText(v),
      },
      {
        title: "Tải trọng",
        dataIndex: "capacity",
        render: (v: number | undefined) => (v != null ? `${v} kg` : "-"),
      },
      {
        title: "Bưu cục",
        dataIndex: ["office", "name"],
        render: (_: unknown, record: AdminVehicle) => record.office?.name || "Chưa phân công",
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string | undefined) => statusText(v),
      },
      {
        title: "",
        render: (_: unknown, record: AdminVehicle) => (
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <span
              onClick={() => onView(record)}
              style={{ color: "#52c41a", cursor: "pointer", display: "inline-flex", alignItems: "center", gap: 6 }}
              title="Xem"
            >
              <span style={{ color: "#52c41a" }}>Xem</span>
            </span>

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
                      title: "Xóa phương tiện này?",
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
              <button type="button" className="dropdown-trigger-button admin-actions-trigger">
                Thêm <DownOutlined />
              </button>
            </Dropdown>
          </div>
        ),
      },
    ],
    [onDelete, onEdit, onView, statusText, typeText]
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

export default VehiclesTable;
