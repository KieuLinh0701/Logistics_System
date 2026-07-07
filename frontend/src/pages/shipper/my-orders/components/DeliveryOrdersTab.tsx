import React, {forwardRef, useCallback, useEffect, useImperativeHandle, useState} from "react";
import {Button, message, Space, Table, Tag, Typography} from "antd";
import {EyeOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ShipperOrder} from "../../../../api/orderApi";
import orderApi from "../../../../api/orderApi";
import type {TabRefreshHandle} from "../MyOrdersPage";

const { Text } = Typography;

interface DeliveryOrdersTabProps {
  search?: string;
  status?: string;
}

const DeliveryOrdersTab = forwardRef<TabRefreshHandle, DeliveryOrdersTabProps>(
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

        const res = await orderApi.getShipperOrders(params);
        const hiddenStatuses = [
          "DELIVERED",
          "FAILED_DELIVERY",
          "DELIVERY_RETRY",
          "DELIVERY_FAILED_FINAL",
          "RETURN_AT_ORIGIN_OFFICE",
          "RETURNING",
          "RETURN_RETRY",
          "RETURNED",
          "RETURN_FAILED_FINAL",
        ];
        const visible = (res.orders || []).filter(
          (o: any) => !hiddenStatuses.includes(o.status)
        );
        setOrders(visible as ShipperOrder[]);
        setPagination((prev) => ({ ...prev, total: visible.length }));
      } catch (error) {
        console.error("Error fetching orders:", error);
        message.error("Lỗi khi tải danh sách đơn hàng");
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
        case "AT_DEST_OFFICE": return "geekblue";
        case "PICKED_UP": return "orange";
        case "READY_FOR_PICKUP": return "blue";
        case "DELIVERING": return "processing";
        case "DELIVERED": return "success";
        case "FAILED_DELIVERY":
        case "RETURNED":
        case "RETURN_FAILED_FINAL": return "error";
        case "RETURNING":
        case "RETURN_RETRY":
        case "RETURN_AT_ORIGIN_OFFICE": return "warning";
        default: return "default";
      }
    };

    const getStatusText = (s: string) => {
      switch (s) {
        case "AT_DEST_OFFICE": return "Tại bưu cục đích";
        case "PICKED_UP": return "Đã lấy hàng";
        case "READY_FOR_PICKUP": return "Sẵn sàng lấy hàng";
        case "DELIVERING": return "Đang giao hàng";
        case "DELIVERED": return "Đã giao";
        case "FAILED_DELIVERY": return "Giao thất bại";
        case "DELIVERY_RETRY": return "Chờ giao lại";
        case "DELIVERY_FAILED_FINAL": return "Giao thất bại";
        case "RETURNED": return "Đã hoàn";
        case "RETURNING": return "Đang hoàn trả";
        case "RETURN_AT_ORIGIN_OFFICE": return "Đã hoàn về bưu cục gốc";
        case "RETURN_RETRY": return "Hoàn lại";
        case "RETURN_FAILED_FINAL": return "Hoàn thất bại";
        default: return s;
      }
    };

    const columns = [
      {
        title: "Mã đơn hàng",
        dataIndex: "trackingNumber",
        key: "trackingNumber",
        width: 140,
        render: (text: string) => <Text strong className="table-strong">{text}</Text>,
      },
      {
        title: "Thông tin người nhận",
        key: "recipient",
        render: (record: ShipperOrder) => {
          const address =
            record.recipientFullAddress ||
            (typeof record.recipientAddress === "string"
              ? record.recipientAddress
              : (record.recipientAddress as any)?.fullAddress) || "";
          return (
            <Space direction="vertical" size={2}>
              <Text strong className="table-strong">{record.recipientName}</Text>
              <Text className="table-muted">{record.recipientPhone}</Text>
              <Text className="table-muted">{address}</Text>
            </Space>
          );
        },
      },
      {
        title: "Dịch vụ & tiền thu",
        key: "serviceCod",
        render: (record: ShipperOrder) => {
          const serviceName =
            typeof record.serviceType === "string"
              ? record.serviceType
              : (record.serviceType as any)?.name ?? "";
          const payer = (record.payer || "").toUpperCase();
          const shippingFee = Number(record.shippingFee || 0);
          const cod = Number(record.cod || 0);
          const totalToCollect = payer === "CUSTOMER" ? shippingFee + cod : cod;
          return (
            <Space direction="vertical" size={2}>
              <Text className="table-strong">{serviceName || "—"}</Text>
              <Text className="table-cod">
                COD thu hộ: {cod > 0 ? `${cod.toLocaleString()}đ` : "0đ"}
              </Text>
              <Text className="table-muted">
                Phí ship cần thu: {payer === "CUSTOMER" && shippingFee > 0 ? `${shippingFee.toLocaleString()}đ` : "0đ"}
              </Text>
              <Text className="table-strong">
                Tổng cần thu: {`${totalToCollect.toLocaleString()}đ`}
              </Text>
            </Space>
          );
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
            <Button icon={<EyeOutlined />} onClick={() => navigate(`/shipper/orders/${record.id}`, { state: { from: "/shipper/my-orders?tab=delivery" } })}>
              Chi tiết
            </Button>
          </Space>
        ),
      },
    ];

    return (
      <div className="my-orders-tab-wrapper">
        <div className="my-orders-results">Kết quả: {orders.length} đơn</div>
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
          scroll={{ x: 960 }}
          className="my-orders-table"
        />
      </div>
    );
  }
);

DeliveryOrdersTab.displayName = "DeliveryOrdersTab";

export default DeliveryOrdersTab;
