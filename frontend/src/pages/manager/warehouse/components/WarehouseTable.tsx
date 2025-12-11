import React from "react";
import { Table, Tabs, Tooltip } from "antd";
import type { ColumnsType } from "antd/es/table";
import dayjs from "dayjs";
import utc from "dayjs/plugin/utc";
import timezone from "dayjs/plugin/timezone";
import { useNavigate } from "react-router-dom";
import type { OrderHistory } from "../../../../types/orderHistory";
import { translateOrderStatus } from "../../../../utils/orderUtils";
import { HomeOutlined, InboxOutlined } from "@ant-design/icons";

dayjs.extend(utc);
dayjs.extend(timezone);

interface WarehouseTableProps {
  orders: OrderHistory[];
  loading?: boolean;
  activeTab: string;
  onTabChange: (key: string) => void;
  page: number;
  limit: number;
  total: number;
  onPageChange: (page: number) => void;
}

const WarehouseTable: React.FC<WarehouseTableProps> = ({
  orders,
  loading = false,
  activeTab,
  onTabChange,
  page,
  limit,
  total,
  onPageChange,
}) => {
  const navigate = useNavigate();
  const columns: ColumnsType<OrderHistory> = [
    {
      title: "Mã đơn hàng",
      key: "trackingNumber",
      align: "center",
      render: (_, record) => record.order.trackingNumber ? (
        <Tooltip title="Click để xem chi tiết đơn hàng">
          <span
            className="navigate-link"
            onClick={() => navigate(`/orders/tracking/${record.order.trackingNumber}`)}
          >
            {record.order.trackingNumber}
          </span>
        </Tooltip>
      ) : (
        <Tooltip title="Chưa có mã đơn hàng">
          <span className="text-muted">Chưa có mã</span>
        </Tooltip>
      ),
    },
    {
      title: "Trọng lượng (Kg)",
      key: "weight",
      align: "center",
      render: (_, record) => record.order?.weight || 0,
    },
    {
      title: "Dịch vụ",
      key: "serviceType",
      align: "center",
      render: (_, record) => record.order.serviceTypeName || <span className="text-muted">N/A</span>,
    },
    {
      title: "Trạng thái",
      key: "status",
      align: "center",
      render: (_, record) => translateOrderStatus(record.order.status),
    },
    {
      title: "Thời gian",
      key: "actionTime",
      align: "center",
      render: (_, record) =>
        record.actionTime
          ? dayjs(record.actionTime).tz("Asia/Ho_Chi_Minh").format("DD/MM/YYYY HH:mm:ss")
          : <span className="text-muted">N/A</span>,
    },
    {
      title: "Ghi chú đơn hàng",
      key: "orderNote",
      align: "center",
      render: (_, record) => (
        <span title={record.order.notes}>
          {record.order.notes || <span className="text-muted">N/A</span>}
        </span>
      ),
    },
    {
      title: "Ghi chú vận chuyển",
      key: "note",
      align: "center",
      render: (_, record) => (
        <div title={record.note}>
          {record.note || <span className="text-muted">N/A</span>}
        </div>
      ),
    },
  ];

  return (
    <div>
      <Tabs
        activeKey={activeTab}
        onChange={onTabChange}
        className="warehouse-tabs"
        tabBarGutter={12}
        items={[
          {
            key: "1",
            label: (
              <span className="tab-label">
                <InboxOutlined /> Chuẩn bị nhập kho
              </span>
            ),
          },
          {
            key: "2",
            label: (
              <span className="tab-label">
                <HomeOutlined /> Đang trong kho
              </span>
            ),
          },
        ]}
      />
      <Table
        columns={columns}
        dataSource={orders}
        loading={loading}
        rowKey="id"
        scroll={{ x: "max-content" }}
        className="list-page-table"
        bordered
        pagination={{
          current: page,
          pageSize: limit,
          total: total,
          onChange: onPageChange,
        }}
      />
    </div>
  );
};

export default WarehouseTable;