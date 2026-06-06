import React, { useEffect, useState } from "react";
import { Alert, Button, Card, Col, Empty, Input, Row, Select, Space, Spin, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { ReloadOutlined, SearchOutlined, EyeOutlined, InboxOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi, { type ShipperOrder } from "../../api/orderApi";
import { translateOrderStatus } from "../../utils/orderUtils";
import "../../styles/ListPage.css";
import "./ShipperPagesShared.css";

const { Text, Title } = Typography;

const FailedDeliveryOrders: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [orders, setOrders] = useState<ShipperOrder[]>([]);
  const [search, setSearch] = useState<string | undefined>();

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const res = await orderApi.getShipperOrders({ page: 1, limit: 200, status: "DELIVERY_RETRY", search });
      setOrders((res.orders || []).filter((o: ShipperOrder) => o.status === "DELIVERY_RETRY"));
    } catch {
      message.error("Lỗi khi tải danh sách hàng giao thất bại");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleReturnToOffice = async (orderId: number) => {
    try {
      setLoading(true);
      await orderApi.returnFailedToOffice(orderId);
      message.success("Đã nộp hàng về bưu cục");
      await fetchOrders();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi nộp hàng về bưu cục");
    } finally {
      setLoading(false);
    }
  };

  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string, record) => (
        <Button
          type="link"
          onClick={() => navigate(`/shipper/orders/${record.id}`)}
          style={{ padding: 0, height: "auto" }}
        >
          <Text strong style={{ color: "#111111" }}>{text}</Text>
        </Button>
      ),
    },
    {
      title: "Thông tin người nhận",
      key: "recipient",
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text strong style={{ color: "#111111" }}>{record.recipientName}</Text>
          <Text style={{ color: "#111111" }}>{record.recipientPhone}</Text>
        </Space>
      ),
    },
    {
      title: "Địa chỉ",
      key: "address",
      render: (_, record) => {
        const address =
          record.recipientFullAddress ||
          (typeof record.recipientAddress === "string"
            ? record.recipientAddress
            : (record.recipientAddress as any)?.fullAddress) || "";
        return (
          <Text style={{ color: "#111111" }}>{address || "-"}</Text>
        );
      },
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (status: string) => (
        <Tag color="orange" style={{ fontWeight: 600 }}>
          {translateOrderStatus(status)}
        </Tag>
      ),
    },
    {
      title: "Thao tác",
      key: "action",
      render: (_, record) => (
        <Button icon={<InboxOutlined />} onClick={() => handleReturnToOffice(record.id)}>
          Nộp về bưu cục
        </Button>
      ),
    },
  ];

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel">
          <div className="shipper-filter-grow">
            <Input
              allowClear
              className="search-input"
              placeholder="Tìm theo mã đơn, người nhận, SĐT"
              prefix={<SearchOutlined />}
              value={search}
              onChange={(e) => setSearch(e.target.value || undefined)}
              onPressEnter={fetchOrders}
              style={{ width: "100%" }}
            />
          </div>
          <div className="shipper-filter-actions">
            <Button icon={<ReloadOutlined />} onClick={fetchOrders}>
              Làm mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main" style={{ margin: 0 }}>
              Hàng giao thất bại
            </h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {orders.length} đơn</div>
            </div>
          </div>
        </div>

        <Card className="shipper-page-table list-page-table" bodyStyle={{ padding: 0 }}>
          {loading && !orders.length ? (
            <div style={{ textAlign: "center", padding: 32 }}>
              <Spin />
            </div>
          ) : orders.length === 0 ? (
            <div style={{ padding: 24 }}>
              <Empty description="Không có đơn nào cần nộp về bưu cục" />
            </div>
          ) : (
            <Table
              rowKey="id"
              columns={columns}
              dataSource={orders}
              pagination={false}
              loading={loading}
              scroll={{ x: 900 }}
            />
          )}
        </Card>
      </div>
    </div>
  );
};

export default FailedDeliveryOrders;
