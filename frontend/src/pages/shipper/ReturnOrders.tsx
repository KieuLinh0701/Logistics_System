import React, {useEffect, useState} from "react";
import {Button, Input, message, Select, Space, Table, Tag, Typography} from "antd";
import {EyeOutlined, ReloadOutlined, SearchOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ShipperOrder} from "../../api/orderApi";
import orderApi from "../../api/orderApi";
import "../../styles/ListPage.css";
import "./ShipperPagesShared.css";

const { Text } = Typography;
const { Option } = Select;

interface FilterParams {
  status?: string;
  search?: string;
}

const ShipperReturnOrders: React.FC = () => {
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
      };
      const res = await orderApi.getShipperReturnOrders(params);
      setOrders((res.orders || []) as ShipperOrder[]);
      setPagination((prev) => ({ ...prev, total: res.pagination?.total || 0 }));
    } catch (error) {
      console.error("Error fetching return orders:", error);
      message.error("Lỗi khi tải danh sách đơn hoàn trả");
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "RETURN_AT_ORIGIN_OFFICE":
        return "warning";
      case "RETURNING":
        return "processing";
      case "RETURN_RETRY":
        return "orange";
      case "RETURNED":
        return "success";
      case "RETURN_FAILED_FINAL":
        return "error";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "RETURN_AT_ORIGIN_OFFICE":
        return "Chờ nhận hoàn trả";
      case "RETURNING":
        return "Đang hoàn trả";
      case "RETURN_RETRY":
        return "Hoàn lại";
      case "RETURNED":
        return "Đã hoàn trả";
      case "RETURN_FAILED_FINAL":
        return "Hoàn trả thất bại";
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
      title: "Người gửi (Shop)",
      key: "sender",
      render: (record: ShipperOrder) => {
        const senderName = (record as any).senderName || "";
        const senderPhone = (record as any).senderPhone || "";
        const senderAddress = (record as any).senderAddress || "";
        const fullAddress = typeof senderAddress === "string"
          ? senderAddress
          : (senderAddress as any)?.fullAddress || "";
        return (
          <Space direction="vertical" size={2}>
            <Text strong className="shipper-table-strong">{senderName}</Text>
            <Text className="shipper-table-muted">{senderPhone}</Text>
            <Text className="shipper-table-muted">{fullAddress}</Text>
          </Space>
        );
      },
    },
    {
      title: "Người nhận hoàn",
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
      title: "Dịch vụ",
      key: "service",
      render: (record: ShipperOrder) => {
        const serviceName =
          typeof record.serviceType === "string"
            ? record.serviceType
            : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Text className="shipper-table-strong">{serviceName || "—"}</Text>
            <Tag color="warning">Hoàn trả</Tag>
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
              placeholder="Tìm theo mã đơn, người gửi, người nhận, SĐT"
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
              style={{ width: 180 }}
              value={filters.status}
              onChange={(v) => setFilters((f) => ({ ...f, status: v }))}
            >
              <Option value="RETURN_AT_ORIGIN_OFFICE">Chờ nhận hoàn trả</Option>
              <Option value="RETURNING">Đang hoàn trả</Option>
              <Option value="RETURN_RETRY">Hoàn lại</Option>
            </Select>
            <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
              Làm mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Đơn hàng hoàn trả</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {pagination.total} đơn</div>
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
            scroll={{ x: 1100 }}
          />
        </div>
      </div>
    </div>
  );
};

export default ShipperReturnOrders;
