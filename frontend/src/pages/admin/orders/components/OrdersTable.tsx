import React, {useMemo} from "react";
import {Dropdown, Modal, Table} from "antd";
import {DeleteOutlined, DownOutlined} from "@ant-design/icons";
import type {AdminOrder} from "../../../../types/order";
import {translateOrderStatus} from "../../../../utils/orderUtils";

interface Option {
  label: string;
  value: string;
}

interface OrdersTableProps {
  loading: boolean;
  rows: AdminOrder[];
  page: number;
  pageSize: number;
  total: number;
  statusOptions: Option[];
  onPageChange: (page: number, pageSize?: number) => void;
  onView: (record: AdminOrder) => void;
  onDelete: (id: number) => void;
}

const OrdersTable: React.FC<OrdersTableProps> = ({
  loading,
  rows,
  page,
  pageSize,
  total,
  statusOptions,
  onPageChange,
  onView,
  onDelete,
}) => {
  const statusText = (status?: string) => {
    if (!status) return "-";
    return statusOptions.find((item) => item.value === status)?.label || translateOrderStatus(status);
  };

  const columns = useMemo(
    () => [
      { title: "Mã vận đơn", dataIndex: "trackingNumber", render: (v: string) => v || "-" },
      { title: "Người gửi", dataIndex: "senderName", render: (v: string) => v || "-" },
      { title: "Người nhận", dataIndex: "recipientName", render: (v: string) => v || "-" },
      {
        title: "Tổng tiền",
        dataIndex: "totalFee",
        render: (v: number | undefined) => (v != null ? `${v.toLocaleString("vi-VN")} VNĐ` : "-"),
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string | undefined) => statusText(v),
      },
      {
        title: "",
        render: (_: unknown, record: AdminOrder) => (
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
                  { key: "delete", icon: <DeleteOutlined />, label: "Xóa" },
                ],
                onClick: ({ key }) => {
                  if (key === "delete") {
                    Modal.confirm({
                      title: "Xóa đơn hàng này?",
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
    [onDelete, onView]
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

export default OrdersTable;
