import React, { useEffect, useState } from "react";
import {
  Table,
  Tag,
  Button,
  Space,
  Typography,
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
import "../../styles/ListPage.css";
import "./ShipperPagesShared.css";

const { Text } = Typography;
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
      const visible = (res.orders || []).filter(
        (o: any) => o.status !== "DELIVERED" && o.status !== "FAILED_DELIVERY"
      );
      setOrders(visible as ShipperOrder[]);
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
      case "DELIVERY_RETRY":
        return "Chờ giao lại";
      case "DELIVERY_FAILED_FINAL":
        return "Giao thất bại";
      case "PARTIAL_DELIVERY":
        return "Giao 1 phần";
      case "PARTIAL_RETURN":
        return "Trả 1 phần";
      case "RETURNED":
        return "Đã hoàn";
      default:
        return status;
    }
  };

  const resetFilters = () => {
    setFilters({});
    setPagination((prev) => ({ ...prev, current: 1 }));
  };

  const handleRefresh = () => {
    resetFilters();
  };

  const columns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => <Text strong className="shipper-table-strong">{text}</Text>,
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
            <Text strong className="shipper-table-strong">{record.recipientName}</Text>
            <Text className="shipper-table-muted">{record.recipientPhone}</Text>
            <Text className="shipper-table-muted">{address}</Text>
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
            <Text className="shipper-table-strong">{serviceName || "—"}</Text>
            <Text className="shipper-cod-value">
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
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel">
          <div className="shipper-filter-grow">
            <Input
              allowClear
              className="search-input"
              placeholder="Tìm theo mã đơn, người nhận, SĐT"
              prefix={<SearchOutlined />}
              value={filters.search}
              onChange={(e) =>
                setFilters((f) => ({ ...f, search: e.target.value || undefined }))
              }
              style={{ width: "100%" }}
            />
          </div>
          <div className="shipper-filter-actions">
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
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              Làm mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Đơn hàng cần giao</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {orders.length} đơn</div>
            </div>
          </div>
        </div>

        <div className="list-page-table shipper-page-table">
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
          />
        </div>
      </div>
    </div>
  );
};

export default ShipperOrders;
