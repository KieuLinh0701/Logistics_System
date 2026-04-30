import React, { useMemo } from "react";
import { Dropdown, Modal, Table } from "antd";
import { DownOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";
import type { AdminOffice } from "../../../../types/office";

interface Option {
  label: string;
  value: string;
}

interface PostOfficesTableProps {
  loading: boolean;
  rows: (AdminOffice & { displayAddress?: string })[];
  page: number;
  pageSize: number;
  total: number;
  officeTypeOptions: Option[];
  officeStatusOptions: Option[];
  onPageChange: (page: number, pageSize?: number) => void;
  onView: (record: AdminOffice) => void;
  onEdit: (record: AdminOffice) => void;
  onDelete: (id: number) => void;
}

const PostOfficesTable: React.FC<PostOfficesTableProps> = ({
  loading,
  rows,
  page,
  pageSize,
  total,
  officeTypeOptions,
  officeStatusOptions,
  onPageChange,
  onView,
  onEdit,
  onDelete,
}) => {
  const columns = useMemo(
    () => [
      { title: "Mã", dataIndex: "code" },
      { title: "Tên bưu cục", dataIndex: "name" },
      { title: "Số điện thoại", dataIndex: "phoneNumber" },
      {
        title: "Loại",
        dataIndex: "type",
        render: (v: string) => {
          const label = officeTypeOptions.find((item) => item.value === v)?.label || v || "N/A";
          return label;
        },
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string) => {
          const label = officeStatusOptions.find((item) => item.value === v)?.label || v || "N/A";
          return label;
        },
      },
      {
        title: "Địa chỉ",
        render: (_: unknown, record: AdminOffice & { displayAddress?: string }) => record.displayAddress || "Chưa có",
      },
      {
        title: "",
        render: (_: unknown, record: AdminOffice) => (
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
                      title: "Xóa bưu cục này?",
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
          </div>
        ),
      },
    ],
    [officeTypeOptions, officeStatusOptions, onDelete, onEdit, onView]
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

export default PostOfficesTable;
