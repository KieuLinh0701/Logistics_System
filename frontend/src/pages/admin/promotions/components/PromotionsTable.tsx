import React, { useMemo } from "react";
import { Dropdown, Table } from "antd";
import { DeleteOutlined, DownOutlined, EditOutlined } from "@ant-design/icons";
import dayjs from "dayjs";
import type { Promotion } from "../../../../types/promotion";

interface PromotionsTableProps {
  loading: boolean;
  rows: Promotion[];
  page: number;
  pageSize: number;
  total: number;
  statusText: (value?: string) => string;
  promotionTypeText: (promotion: Promotion) => string;
  onPageChange: (page: number, pageSize?: number) => void;
  onView: (record: Promotion) => void;
  onEdit: (record: Promotion) => void;
  onDelete: (record: Promotion) => void;
}

const PromotionsTable: React.FC<PromotionsTableProps> = ({
  loading,
  rows,
  page,
  pageSize,
  total,
  statusText,
  promotionTypeText,
  onPageChange,
  onView,
  onEdit,
  onDelete,
}) => {
  const columns = useMemo(
    () => [
      {
        title: "Mã",
        dataIndex: "code",
        render: (code: string) => <span style={{ fontFamily: "monospace" }}>{code}</span>,
      },
      {
        title: "Tiêu đề",
        dataIndex: "title",
        ellipsis: true,
      },
      {
        title: "Loại",
        key: "type",
        render: (_: unknown, record: Promotion) => promotionTypeText(record),
      },
      {
        title: "Giảm giá",
        key: "discount",
        render: (_: unknown, record: Promotion) =>
          record.discountType === "PERCENTAGE"
            ? `${record.discountValue}%`
            : `${record.discountValue.toLocaleString("vi-VN")}đ`,
      },
      {
        title: "Thời gian",
        key: "dateRange",
        render: (_: unknown, record: Promotion) => (
          <div>
            <div>{dayjs(record.startDate).format("DD/MM/YYYY")}</div>
            <div>{dayjs(record.endDate).format("DD/MM/YYYY")}</div>
          </div>
        ),
      },
      {
        title: "Sử dụng",
        key: "usage",
        render: (_: unknown, record: Promotion) => `${record.usedCount} / ${record.usageLimit || "∞"}`,
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (status: string | undefined) => statusText(status),
      },
      {
        title: "",
        render: (_: unknown, record: Promotion) => (
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
                  { key: "delete", icon: <DeleteOutlined />, label: "Xóa", danger: record.usedCount > 0 },
                ],
                onClick: ({ key }) => {
                  if (key === "edit") onEdit(record);
                  if (key === "delete") onDelete(record);
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
    [onDelete, onEdit, onView, promotionTypeText, statusText]
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

export default PromotionsTable;
