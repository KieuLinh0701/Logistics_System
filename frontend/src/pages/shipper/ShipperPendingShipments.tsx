import React, {useEffect, useState} from "react";
import {Button, message, Table, Tag, Typography} from "antd";
import {FileExcelOutlined, PlayCircleOutlined, ReloadOutlined} from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import "../../styles/ListPage.css";
import "./ShipperPagesShared.css";

const { Text } = Typography;

const ShipperPendingShipments: React.FC = () => {
  const [shipments, setShipments] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [fetchKey, setFetchKey] = useState(0);

  const normalizeShipments = (raw: any): any[] => {
    let data = raw;
    if (typeof raw === "string") {
      try {
        data = JSON.parse(raw);
      } catch (e) {
        console.error("[NORMALIZE] Failed to parse JSON string:", raw);
        return [];
      }
    }

    // Case 1: data is already an array
    if (Array.isArray(data)) {
      return data;
    }

    // Case 2: { success: true/false, data: [...] }
    if (data && typeof data === 'object' && 'success' in data) {
      if (Array.isArray(data.data)) {
        return data.data;
      }

      // Case 3: { success: true, data: { shipments: [...] } }
      if (data.data && typeof data.data === 'object' && 'shipments' in data.data) {
        return data.data.shipments || [];
      }
    }

    // Case 4: Direct object with shipments/data/items/content
    const candidates = ['shipments', 'data', 'items', 'content'];
    for (const key of candidates) {
      if (Array.isArray(data?.[key])) {
        return data[key];
      }
    }

    return [];
  };

  const fetchShipments = async () => {
    try {
      setLoading(true);
      const response = await shipmentApi.listShipperActiveShipments();

      const normalized = normalizeShipments(response);

      setShipments(normalized);
    } catch (error) {
      console.error("Error fetching shipper shipments:", error);
      message.error("Lỗi khi tải danh sách chuyến hàng");
      setShipments([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchShipments();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [fetchKey]);

  const getActionButton = (record: any) => {
    const status = record.status || record.shipmentStatus || record.state;

    if (status === "PENDING") {
      return (
        <Button
          className="primary-button"
          icon={<PlayCircleOutlined />}
          onClick={() => handleStartShipment(record.id)}
          loading={actionLoading}
        >
          Bắt đầu chuyến
        </Button>
      );
    }

    if (status === "IN_TRANSIT") {
      return (
        <Button
          className="success-button"
          icon={<FileExcelOutlined />}
          onClick={() => handleFinishShipment(record.id)}
          loading={actionLoading}
        >
          Kết thúc chuyến
        </Button>
      );
    }

    return null;
  };

  const handleStartShipment = async (shipmentId: number) => {
    if (!shipmentId) return;
    try {
      setActionLoading(true);
      await shipmentApi.startShipperDeliveryShipment(shipmentId);
      message.success("Đã bắt đầu chuyến giao hàng");
      setFetchKey((k) => k + 1);
    } catch (e: any) {
      message.error(e?.message || "Không thể bắt đầu chuyến");
    } finally {
      setActionLoading(false);
    }
  };

  const handleFinishShipment = async (shipmentId: number) => {
    if (!shipmentId) return;
    try {
      setActionLoading(true);
      await shipmentApi.finishShipperDeliveryShipment(shipmentId);
      message.success("Đã kết thúc chuyến giao hàng");
      setFetchKey((k) => k + 1);
    } catch (e: any) {
      message.error(e?.message || "Không thể kết thúc chuyến");
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "gold";
      case "IN_TRANSIT":
        return "processing";
      case "COMPLETED":
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
        return "Chờ bắt đầu";
      case "IN_TRANSIT":
        return "Đang giao";
      case "COMPLETED":
        return "Hoàn thành";
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
    }
  };

  const columns = [
    {
      title: "Mã chuyến",
      dataIndex: "code",
      key: "code",
      render: (text: string) => <Text strong>{text}</Text>,
    },
    {
      title: "Phương tiện",
      key: "vehicle",
      render: (_: any, record: any) => (
        <Text>{record.vehicle?.licensePlate || "—"}</Text>
      ),
    },
    {
      title: "Từ bưu cục",
      key: "fromOffice",
      render: (_: any, record: any) => <Text>{record.fromOffice?.name || "—"}</Text>,
    },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, record: any) => <Text>{record.toOffice?.name || "—"}</Text>,
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
      title: "Số đơn",
      dataIndex: "orderCount",
      key: "orderCount",
      render: (value: number) => <Text>{value ?? 0}</Text>,
    },
    {
      title: "Thời gian tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (text: string) => <Text>{text ? new Date(text).toLocaleString("vi-VN") : "—"}</Text>,
    },
    {
      title: "Thao tác",
      key: "action",
      width: 150,
      render: (_: any, record: any) => getActionButton(record),
    },
  ];

  return (
    <div className="list-page-layout shipper-page-root">
      <div className="list-page-content">
        <div className="shipper-filter-panel">
          <div className="shipper-filter-grow" />
          <div className="shipper-filter-actions">
            <Button icon={<ReloadOutlined />} onClick={() => setFetchKey((k) => k + 1)}>
              Làm mới
            </Button>
          </div>
        </div>

        <div className="list-page-header shipper-page-header">
          <h3 className="list-page-title-main">Chuyến hàng cần giao</h3>
          <div className="list-page-tag">Kết quả: {shipments.length} chuyến</div>
        </div>

        <div className="list-page-table shipper-page-table">
          <Table
            rowKey="id"
            loading={loading}
            columns={columns}
            dataSource={shipments}
            pagination={false}
            locale={{
              emptyText: "Chưa có chuyến hàng",
            }}
            scroll={{ x: 960 }}
          />
        </div>
      </div>
    </div>
  );
};

export default ShipperPendingShipments;
