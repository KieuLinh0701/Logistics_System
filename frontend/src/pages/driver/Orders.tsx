import React, { useEffect, useState } from "react";
import { Card, Table, Button, Space, Typography, message, Tag, Select, Modal, Row, Col } from "antd";
import { ReloadOutlined, CheckOutlined } from "@ant-design/icons";
import orderApi from "../../api/orderApi";
import { translateOrderStatus } from "../../utils/orderUtils";

const { Title, Text } = Typography;
const { Option } = Select;

interface OrderItem {
  id: number;
  trackingNumber: string;
  senderName: string;
  senderPhone: string;
  recipientName: string;
  recipientPhone: string;
  weight: number;
  cod: number;
  shippingFee: number;
  status: string;
  toOffice?: { id: number; name: string };
  serviceType?: { id: number; name: string };
  createdAt: string;
}

interface Vehicle {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status?: string;
}

const DriverOrders: React.FC = () => {
  const [orders, setOrders] = useState<OrderItem[]>([]);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(false);
  const [pickupLoading, setPickupLoading] = useState(false);
  const [selectedOrders, setSelectedOrders] = useState<number[]>([]);
  const [selectedVehicle, setSelectedVehicle] = useState<number | undefined>(undefined);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0 });

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      const [ordersRes, contextRes] = await Promise.all([
        orderApi.getDriverPendingOrders({
          page: pagination.page,
          limit: pagination.limit,
        }),
        orderApi.getDriverContext(),
      ]);
      setOrders(Array.isArray(ordersRes.orders) ? ordersRes.orders : []);
      setPagination(ordersRes.pagination || pagination);
      setVehicles(Array.isArray(contextRes.vehicles) ? contextRes.vehicles : []);
    } catch (e: any) {
      message.error("Không tải được dữ liệu");
    } finally {
      setLoading(false);
    }
  };

  const handlePickup = async () => {
  if (selectedOrders.length === 0) {
    message.warning("Vui lòng chọn ít nhất một đơn hàng");
    return;
  }

  if (!selectedVehicle) {
    message.warning("Vui lòng chọn phương tiện trước khi nhận hàng");
    return;
  }

  Modal.confirm({
    title: "Xác nhận nhận hàng",
    content: `Bạn có chắc chắn muốn nhận ${selectedOrders.length} đơn hàng đã chọn?`,
    onOk: async () => {
      try {
        setPickupLoading(true);

        await orderApi.driverPickUp({
          vehicleId: selectedVehicle,
          orderIds: selectedOrders,
        });

        message.success("Đã nhận hàng thành công");
        setSelectedOrders([]);
        setSelectedVehicle(undefined);
        loadData();
      } catch (e: any) {
        message.error(e?.response?.data?.message || e?.message || "Lỗi khi nhận hàng");
      } finally {
        setPickupLoading(false);
      }
    },
  });
};


  const getStatusTagColor = (status: string) => {
    switch (status) {
      case "AT_ORIGIN_OFFICE":
        return "blue";
      case "PICKED_UP":
        return "orange";
      case "IN_TRANSIT":
        return "cyan";
      case "AT_DEST_OFFICE":
        return "green";
      default:
        return "default";
    }
  };

  const columns = [
    { 
      title: "Mã đơn hàng", 
      dataIndex: "trackingNumber", 
      key: "trackingNumber",
      render: (text: string) => <Text strong>{text}</Text>
    },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, r: OrderItem) => r.toOffice?.name || "-",
    },
    {
      title: "Trạng thái",
      key: "status",
      render: (_: any, r: OrderItem) => (
        <Tag color={getStatusTagColor(r.status)}>{translateOrderStatus(r.status)}</Tag>
      ),
    },
    {
      title: "Ngày tạo",
      dataIndex: "createdAt",
      key: "createdAt",
      render: (date: string) => date ? new Date(date).toLocaleDateString('vi-VN') : "-",
    },
  ];

  const rowSelection = {
    selectedRowKeys: selectedOrders,
    onChange: (selectedRowKeys: React.Key[]) => {
      setSelectedOrders(selectedRowKeys as number[]);
    },
  };

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={<Title level={3}>Đơn hàng cần nhận</Title>}
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
            Tải lại
          </Button>
        }
      >
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={12}>
            <Space>
              <Text>Chọn phương tiện:</Text>
              <Select
                placeholder="Chọn xe vận chuyển"
                style={{ width: 200 }}
                value={selectedVehicle}
                onChange={setSelectedVehicle}
                allowClear
              >
                {vehicles
                  .map((vehicle) => (
                    <Option key={vehicle.id} value={vehicle.id}>
                      {vehicle.licensePlate} - {vehicle.type} ({vehicle.capacity}kg)
                    </Option>
                  ))}
              </Select>
            </Space>
          </Col>
          <Col span={12} style={{ textAlign: "right" }}>
            <Space>
              <Button
                type="primary"
                icon={<CheckOutlined />}
                onClick={handlePickup}
                loading={pickupLoading}
                disabled={selectedOrders.length === 0 || !selectedVehicle}
              >
                Nhận hàng ({selectedOrders.length})
              </Button>
            </Space>
          </Col>
        </Row>

        <Table
          rowKey="id"
          columns={columns}
          dataSource={orders}
          loading={loading}
          rowSelection={rowSelection}
          pagination={{
            current: pagination.page,
            pageSize: pagination.limit,
            total: pagination.total,
            onChange: (page, pageSize) => {
              setPagination({ ...pagination, page, limit: pageSize });
              loadData();
            },
          }}
        />
      </Card>
    </div>
  );
};

export default DriverOrders;
