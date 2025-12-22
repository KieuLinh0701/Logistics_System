import React, { useEffect, useState } from "react";
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Typography,
  Row,
  Col,
  Select,
  Input,
  message,
} from "antd";
import {
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import orderApi from "../../api/orderApi";
import type { ShipperOrder } from "../../api/orderApi";

const { Title, Text } = Typography;
const { Option } = Select;

interface FilterParams {
  status?: string;
  search?: string;
}

const ShipperOrders: React.FC = () => {
  const navigate = useNavigate();
  const [orders, setOrders] = useState<ShipperOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [filters, setFilters] = useState<FilterParams>({});
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagination.current, pagination.pageSize, filters]);

  const fetchOrders = async () => {
    try {
      setLoading(true);
      const params = {
        page: pagination.current,
        limit: pagination.pageSize,
        status: filters.status,
        search: filters.search,
      } as any;
      const res = await orderApi.getShipperOrders(params);
      setOrders(res.orders);
      setPagination((prev) => ({ ...prev, total: res.pagination.total }));
    } catch (error) {
      console.error("Error fetching orders:", error);
      message.error("Lỗi khi tải danh sách đơn hàng");
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PICKED_UP":
        return "orange";
      case "READY_FOR_PICKUP":
        return "blue";
      case "DELIVERING":
        return "processing";
      case "DELIVERED":
        return "success";
      case "FAILED_DELIVERY":
      case "RETURNED":
        return "error";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "PICKED_UP":
        return "Đã lấy hàng";
      case "READY_FOR_PICKUP":
        return "Sẵn sàng lấy hàng";
      case "DELIVERING":
        return "Đang giao hàng";
      case "DELIVERED":
        return "Đã giao";
      case "FAILED_DELIVERY":
        return "Giao thất bại";
      case "RETURNED":
        return "Đã hoàn";
      default:
        return status;
    }
  };

  const resetFilters = () => {
    setFilters({});
  };

  const columns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => <Text strong style={{ color: "#1f2937" }}>{text}</Text>,
    },
    {
      title: "Thông tin người nhận",
      key: "recipient",
      render: (record: ShipperOrder) => {
        const address =
          typeof record.recipientAddress === "string"
            ? record.recipientAddress
            : (record.recipientAddress as any)?.fullAddress ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Text strong style={{ color: "#111827" }}>{record.recipientName}</Text>
            <Text type="secondary" style={{ fontSize: 12, color: "#6b7280" }}>
              {record.recipientPhone}
            </Text>
            <Text style={{ fontSize: 12, color: "#4b5563" }}>{address}</Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ & COD",
      key: "serviceCod",
      render: (record: ShipperOrder) => {
        const serviceName =
          typeof record.serviceType === "string"
            ? record.serviceType
            : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Text style={{ color: "#1f2937" }}>{serviceName || "—"}</Text>
            <Text style={{ color: "#ef4444", fontWeight: 600 }}>
              {record.cod ? `${record.cod.toLocaleString()}đ` : "COD: 0đ"}
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
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/shipper/orders/${record.id}`)}>
            Chi tiết
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Title level={3}>Đơn hàng cần giao</Title>
        </Col>
        <Col>
          <Space>
            <Input
              allowClear
              placeholder="Tìm theo mã đơn, người nhận, SĐT"
              prefix={<SearchOutlined />}
              style={{ width: 260 }}
              value={filters.search}
              onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value }))}
            />
            <Select
              allowClear
              placeholder="Trạng thái"
              style={{ width: 160 }}
              value={filters.status}
              onChange={(v) => setFilters((f) => ({ ...f, status: v }))}
            >
              <Option value="PICKED_UP">Đã lấy hàng</Option>
              <Option value="DELIVERING">Đang giao</Option>
              <Option value="DELIVERED">Đã giao</Option>
              <Option value="FAILED_DELIVERY">Giao thất bại</Option>
              <Option value="RETURNED">Đã hoàn</Option>
            </Select>
            <Button icon={<ReloadOutlined />} onClick={resetFilters}>
              Xóa lọc
            </Button>
          </Space>
        </Col>
      </Row>

      <Card bordered={false}>
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
              setPagination((prev) => ({ ...prev, current: page, pageSize })),
          }}
        />
      </Card>
    </div>
  );
};

export default ShipperOrders;
