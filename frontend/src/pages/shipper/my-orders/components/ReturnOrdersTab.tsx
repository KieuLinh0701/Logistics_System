import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useState} from "react";
import {Button, message, Space, Table, Tag, Typography} from "antd";
import {EyeOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ShipperOrder} from "../../../../api/orderApi";
import orderApi from "../../../../api/orderApi";
import type {TabRefreshHandle} from "../MyOrdersPage";

const { Text } = Typography;

interface ReturnOrdersTabProps {
  search?: string;
  status?: string;
}

const ReturnOrdersTab = forwardRef<TabRefreshHandle, ReturnOrdersTabProps>(
  ({ search, status }, ref) => {
    const navigate = useNavigate();
    const [orders, setOrders] = useState<ShipperOrder[]>([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
      current: 1,
      pageSize: 10,
      total: 0,
    });

    const fetchOrders = useCallback(async () => {
      try {
        setLoading(true);
        const params: any = {
          page: pagination.current,
          limit: pagination.pageSize,
        };
        if (status) params.status = status;
        if (search) params.search = search;

        const res = await orderApi.getShipperReturnOrders(params);
        setOrders((res.orders || []) as ShipperOrder[]);
        setPagination((prev) => ({ ...prev, total: res.pagination?.total || 0 }));
      } catch (error) {
        console.error("Error fetching return orders:", error);
        message.error("Lỗi khi tải danh sách đơn hoàn trả");
      } finally {
        setLoading(false);
      }
    }, [pagination.current, pagination.pageSize, status, search]);

    useEffect(() => {
      fetchOrders();
    }, [fetchOrders]);

    useImperativeHandle(ref, () => ({
      reload: () => {
        fetchOrders();
      },
    }));

    const getStatusColor = (s: string) => {
      switch (s) {
        case "RETURN_AT_ORIGIN_OFFICE": return "warning";
        case "RETURNING": return "processing";
        case "RETURN_RETRY": return "orange";
        case "RETURNED": return "success";
        case "RETURN_FAILED_FINAL": return "error";
        default: return "default";
      }
    };

    const getStatusText = (s: string) => {
      switch (s) {
        case "RETURN_AT_ORIGIN_OFFICE": return "Chờ nhận hoàn trả";
        case "RETURNING": return "Đang hoàn trả";
        case "RETURN_RETRY": return "Hoàn lại";
        case "RETURNED": return "Đã hoàn trả";
        case "RETURN_FAILED_FINAL": return "Hoàn trả thất bại";
        default: return s;
      }
    };

    const columns = [
      {
        title: "Mã đơn hàng",
        dataIndex: "trackingNumber",
        key: "trackingNumber",
        width: 160,
        minWidth: 160,
        render: (text: string) => <span className="tracking-number-cell table-strong">{text}</span>,
      },
      {
        title: "Người nhận hoàn",
        key: "recipient",
        render: (record: ShipperOrder) => {
          const senderAddress =
            record.senderFullAddress ||
            record.senderAddress ||
            "";
          const address =
            typeof senderAddress === "string"
              ? senderAddress
              : (senderAddress as any)?.fullAddress || "";
          return (
            <Space direction="vertical" size={2}>
              <Text strong className="table-strong">{record.senderName}</Text>
              <Text className="table-muted">{record.senderPhone}</Text>
              <Text className="table-muted">{address}</Text>
            </Space>
          );
        },
      },
      {
        title: "Dịch vụ",
        dataIndex: "serviceType",
        key: "service",
        render: (serviceType: any) => {
          const serviceName =
            typeof serviceType === "string"
              ? serviceType
              : serviceType?.name ?? "";
          return <Text className="table-strong">{serviceName || "—"}</Text>;
        },
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        key: "status",
        render: (s: string) => (
          <Tag color={getStatusColor(s)} style={{ fontWeight: 600, textTransform: "uppercase" }}>
            {getStatusText(s)}
          </Tag>
        ),
      },
      {
        title: "Thao tác",
        key: "action",
        render: (record: ShipperOrder) => (
          <Space>
            <Button icon={<EyeOutlined />} onClick={() => navigate(`/shipper/orders/${record.id}`, { state: { from: "/shipper/my-orders?tab=return" } })}>
              Chi tiết
            </Button>
          </Space>
        ),
      },
    ];

    return (
      <div className="my-orders-tab-wrapper">
        <div className="my-orders-results">Kết quả: {pagination.total} đơn</div>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={orders}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            onChange: (page, pageSize) =>
              setPagination((prev) => ({ ...prev, current: page, pageSize: pageSize || 10 })),
          }}
          scroll={{ x: 1100 }}
          className="my-orders-table"
        />
      </div>
    );
  }
);

ReturnOrdersTab.displayName = "ReturnOrdersTab";

export default ReturnOrdersTab;
