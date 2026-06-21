import React, {useEffect, useState} from "react";
import {Button, message, Space, Table, Tag, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {EyeOutlined, ReloadOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ShipperOrder} from "../../api/orderApi";
import orderApi from "../../api/orderApi";
import "../../styles/ListPage.css";
import "./ShipperPagesShared.css";

type OrderItem = ShipperOrder;

const ShipperUnassignedOrders: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<OrderItem[]>([]);
  const [page, setPage] = useState(1);
  const [limit, setLimit] = useState(10);
  const [total, setTotal] = useState(0);

  const fetchUnassigned = async (p = page, l = limit) => {
    try {
      setLoading(true);
      const resp = await orderApi.getShipperUnassignedOrders({ page: p, limit: l });
      setData(resp.orders || []);
      setTotal(resp.pagination?.total || 0);
      setPage(resp.pagination?.page || p);
      setLimit(resp.pagination?.limit || l);
    } catch (err) {
      message.error("Lỗi khi tải đơn chưa gán");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnassigned(1, limit);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleClaim = async (orderId: number) => {
    try {
      await orderApi.claimShipperOrder(orderId);
      message.success("Đã nhận đơn");
      fetchUnassigned(page, limit);
    } catch (err: any) {
      message.error(err?.message || "Lỗi khi nhận đơn");
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "default";
      case "CONFIRMED":
      case "AT_DEST_OFFICE":
        return "blue";
      case "READY_FOR_PICKUP":
        return "blue";
      case "PICKED_UP":
        return "orange";
      case "DELIVERING":
        return "processing";
      case "DELIVERED":
        return "success";
      case "CANCELLED":
        return "error";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "PENDING":
        return "Chờ xử lý";
      case "CONFIRMED":
        return "Đã xác nhận";
      case "AT_DEST_OFFICE":
        return "Đã đến bưu cục";
      case "READY_FOR_PICKUP":
        return "Sẵn sàng lấy hàng";
      case "PICKED_UP":
        return "Đã lấy hàng";
      case "DELIVERING":
        return "Đang giao hàng";
      case "DELIVERED":
        return "Đã giao";
      case "PARTIAL_DELIVERY":
        return "Giao 1 phần";
      case "PARTIAL_RETURN":
        return "Trả 1 phần";
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
    }
  };

  const columns: ColumnsType<OrderItem> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      render: (text: string) => <Typography.Text strong className="shipper-table-strong">{text}</Typography.Text>,
    },
    {
      title: "Thông tin người nhận",
      key: "recipient",
      render: (_, record) => {
        const address =
          record.recipientFullAddress ||
          (typeof record.recipientAddress === "string"
            ? record.recipientAddress
            : (record.recipientAddress as any)?.fullAddress) || "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text strong className="shipper-table-strong">
              {record.recipientName}
            </Typography.Text>
            <Typography.Text className="shipper-table-muted">{record.recipientPhone}</Typography.Text>
            <Typography.Text className="shipper-table-muted">{address}</Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ & COD",
      key: "serviceCod",
      render: (_, record) => {
        const serviceName =
          typeof record.serviceType === "string" ? record.serviceType : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text className="shipper-table-strong">{serviceName || "—"}</Typography.Text>
            <Typography.Text className="shipper-cod-value">
              {record.cod ? `${record.cod.toLocaleString()}đ` : "COD: 0đ"}
            </Typography.Text>
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
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => navigate(`/shipper/orders/${record.id}`)}>
            Chi tiết
          </Button>
          <Button type="primary" className="primary-button" onClick={() => handleClaim(record.id!)}>
            Nhận đơn
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel shipper-filter-panel--end">
          <div className="shipper-filter-actions">
            <Button icon={<ReloadOutlined />} onClick={() => fetchUnassigned(page, limit)}>
              Làm mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <div>
            <h3 className="list-page-title-main">Đơn chưa gán</h3>
            <div className="shipper-header-meta">
              <div className="list-page-tag">Kết quả: {total} đơn</div>
            </div>
          </div>
        </div>

        <div className="list-page-table shipper-page-table">
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={data}
            pagination={{
              current: page,
              pageSize: limit,
              total,
              onChange: (p, l) => fetchUnassigned(p, l),
            }}
            scroll={{ x: 960 }}
          />
        </div>
      </div>
    </div>
  );
};

export default ShipperUnassignedOrders;
