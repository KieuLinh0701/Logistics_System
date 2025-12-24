import React, { useEffect, useState } from "react";
import { Button, Table, message, Card, Row, Col, Typography, Modal, Space, Tooltip, Select, Input, Tag } from "antd";
import { SearchOutlined, ReloadOutlined, EyeOutlined } from "@ant-design/icons";
import { connectWebSocket, disconnectWebSocket } from "../../../socket/socket";
import { getUserId, getCurrentUser } from "../../../utils/authUtils";
import orderApi from "../../../api/orderApi";
import SimpleMap from "../../../components/map/SimpleMap";
import { useNavigate } from "react-router-dom";

export default function ShippingRequests() {
  const navigate = useNavigate();
  const [list, setList] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [mapVisible, setMapVisible] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<any | null>(null);
  const [filters, setFilters] = useState<{ search?: string }>({});
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 });

  async function load(page = pagination.current, limit = pagination.pageSize) {
    setLoading(true);
    try {
      const res = await orderApi.getShipperPickupByCourierRequests({ page, limit });
      const shipperOrders = res.orders || [];
      setList(shipperOrders.filter(o => {
        if (!filters.search) return true;
        const s = filters.search.toLowerCase();
        return (o.senderName || '').toLowerCase().includes(s)
          || (o.senderPhone || '').toLowerCase().includes(s)
          || (o.trackingNumber || '').toLowerCase().includes(s);
      }));
      setPagination((p) => ({ ...p, total: res.pagination?.total || 0, current: page }));
    } catch (e) {
      console.error("Error loading shipper pickup orders:", e);
      message.error("Không thể tải danh sách yêu cầu");
    } finally { setLoading(false); }
  }

  useEffect(() => { load(); }, []);

  useEffect(() => {
    const uid = getUserId();
    if (!uid) return;
    connectWebSocket(uid, (msg) => {
      message.info(msg.title || "Thông báo mới");
      if (msg.type === "assignment" || msg.type === "shipping_request_accepted" || msg.type === "order_ready_for_pickup") load();
    });
    return () => { disconnectWebSocket(); };
  }, []);

  async function accept(id: number) {
    try {
      const rec: any = list.find((r) => r.id === id);
      if (!rec) {
        message.error("Yêu cầu không tồn tại");
        return;
      }

      await orderApi.claimShipperOrder(rec.id);
      message.success("Đã nhận đơn");
      // Tự động tải lại toàn bộ danh sách từ backend để đảm bảo đồng bộ
      await load(1, pagination.pageSize);
      // Không mở modal hay yêu cầu xác nhận ngay; shipper sẽ dùng nút Chi tiết để xác nhận lấy
    } catch (e) {
      console.error(e);
      message.error("Lỗi khi nhận yêu cầu");
    }
  }

  async function openMapForOrder(order: any) {
    try {
      setLoading(true);
      const detail = await orderApi.getShipperOrderDetail(order.id);
      setSelectedOrder(detail || order);
      setMapVisible(true);
    } catch (e) {
      console.error('Không tải được chi tiết đơn:', e);
      message.error('Không thể tải chi tiết đơn');
      setSelectedOrder(order);
      setMapVisible(true);
    } finally {
      setLoading(false);
    }
  }

  async function markPickedUpFromMap(order: any) {
    try {
      let payload: any = {};
      try {
        await new Promise<void>((resolve) => {
          if (!navigator.geolocation) return resolve();
          navigator.geolocation.getCurrentPosition((pos) => {
            payload.latitude = pos.coords.latitude;
            payload.longitude = pos.coords.longitude;
            resolve();
          }, () => resolve(), { timeout: 5000 });
        });
      } catch (e) {}
      await orderApi.markShipperPickedUp(order.id, payload);
      message.success('Đã xác nhận đã lấy hàng');
      setMapVisible(false);
      load();
    } catch (e) {
      console.error(e);
      message.error('Lỗi khi xác nhận đã lấy');
    }
  }

  async function deliverToOriginFromMap(order: any) {
    try {
      await orderApi.deliverShipperToOrigin(order.id, {});
      message.success('Đã nộp hàng tại bưu cục');
      setMapVisible(false);
      load();
    } catch (e) {
      console.error(e);
      message.error('Lỗi khi nộp tại bưu cục');
    }
  }

  const STATUS_MAP: Record<string, { label: string; color: string }> = {
    READY_FOR_PICKUP: { label: 'Sẵn sàng lấy hàng', color: 'blue' },
    PICKING_UP: { label: 'Đang lấy', color: 'orange' },
    PICKED_UP: { label: 'Đã lấy', color: 'orange' },
    AT_ORIGIN_OFFICE: { label: 'Đã nộp tại bưu cục', color: 'green' },
    DELIVERED: { label: 'Đã giao', color: 'green' },
    CANCELLED: { label: 'Đã huỷ', color: 'red' },
    RETURNED: { label: 'Đã hoàn', color: 'red' },
  };

  const columns = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 140,
      render: (text: string) => <Typography.Text strong style={{ color: "#1f2937" }}>{text}</Typography.Text>,
    },
    {
      title: "Thông tin người gửi",
      key: "sender",
      render: (record: any) => {
        const address = typeof record.senderAddress === 'string' ? record.senderAddress : (record.senderAddress as any)?.fullAddress ?? '';
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text strong style={{ color: '#111827' }}>{record.senderName}</Typography.Text>
            <Typography.Text type="secondary" style={{ fontSize: 12, color: '#6b7280' }}>{record.senderPhone}</Typography.Text>
            <Typography.Text style={{ fontSize: 12, color: '#4b5563' }}>{address}</Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ & COD",
      key: "serviceCod",
      render: (record: any) => {
        const serviceName = typeof record.serviceType === 'string' ? record.serviceType : (record.serviceType as any)?.name ?? '';
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text style={{ color: '#1f2937' }}>{serviceName || '—'}</Typography.Text>
            <Typography.Text style={{ color: '#ef4444', fontWeight: 600 }}>{record.cod ? `${record.cod.toLocaleString()}đ` : 'COD: 0đ'}</Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Trạng thái",
      dataIndex: "status",
      key: "status",
      render: (s: string) => {
        const meta = STATUS_MAP[s] || { label: s, color: 'default' };
        return <Tag color={meta.color} style={{ fontWeight: 600, textTransform: 'uppercase' }}>{meta.label}</Tag>;
      },
    },
    {
      title: "Thao tác",
      key: "action",
      render: (record: any) => (
        <Space>
          {record.status === 'READY_FOR_PICKUP' && (
            <Button type="primary" onClick={() => accept(record.id)}>Nhận</Button>
          )}
          <Button icon={<EyeOutlined />} onClick={() => openMapForOrder(record)}>Chi tiết</Button>
        </Space>
      ),
    },
  ];

  const { Title } = Typography;

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <Row justify="space-between" align="middle" style={{ marginBottom: 16 }}>
        <Col>
          <Title level={3}>Yêu cầu lấy hàng tại nhà</Title>
        </Col>
        <Col>
          <Space>
            <Input
              allowClear
              placeholder="Tìm theo mã đơn, người gửi, SĐT"
              prefix={<SearchOutlined />}
              style={{ width: 260 }}
              value={filters.search}
              onChange={(e) => setFilters((f) => ({ ...f, search: e.target.value }))}
            />
            <Button icon={<ReloadOutlined />} onClick={() => { setFilters({}); load(1, pagination.pageSize); }}>
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
          dataSource={list}
          pagination={{
            current: pagination.current,
            pageSize: pagination.pageSize,
            total: pagination.total,
            onChange: (page, pageSize) => { setPagination((p) => ({ ...p, current: page, pageSize })); load(page, pageSize); }
          }}
        />

        <Modal
          title={selectedOrder ? `Bản đồ - ${selectedOrder.trackingNumber || selectedOrder.id}` : "Bản đồ"}
          visible={mapVisible}
          onCancel={() => setMapVisible(false)}
          footer={(
            selectedOrder ? (
              selectedOrder.status === 'PICKED_UP' ? (
                <Space>
                  <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                  <Button type="primary" style={{ backgroundColor: '#16a34a', borderColor: '#16a34a' }} onClick={() => selectedOrder && deliverToOriginFromMap(selectedOrder)}>Nộp tại bưu cục</Button>
                </Space>
              ) : (
                <Space>
                  <Button onClick={() => setMapVisible(false)}>Đóng</Button>
                  <Button type="primary" onClick={() => selectedOrder && markPickedUpFromMap(selectedOrder)}>Xác nhận đã lấy</Button>
                </Space>
              )
            ) : null
          )}
          width={800}
        >
          {selectedOrder && (
            (() => {
              const currentUser = getCurrentUser();
              // try to get shipper's office from currentUser, fallback to order.fromOffice
              const shipperOffice = (currentUser as any)?.office || selectedOrder.fromOffice || null;
              const deliverOffice = shipperOffice ? {
                id: shipperOffice.id,
                name: shipperOffice.name,
                address: shipperOffice.detail || `${shipperOffice.detail || ''}`,
                latitude: shipperOffice.latitude,
                longitude: shipperOffice.longitude,
              } : null;

              // Nếu đã ở trạng thái PICKED_UP, hiển thị modal chuyên cho lộ trình nộp hàng
              if (selectedOrder.status === 'PICKED_UP') {
                return (
                  <SimpleMap
                    // hide receive card when already picked up
                    deliveryStops={[]}
                    deliverOffice={deliverOffice}
                  />
                );
              }

              // Ngược lại, hiển thị lộ trình nhận hàng (gồm điểm người gửi)
              // Không truyền `deliverOffice` ở giai đoạn Sẵn sàng lấy / Đang lấy để chỉ hiện lộ trình nhận
              return (
                <SimpleMap
                  title="Lộ trình nhận hàng"
                  deliveryStops={[{
                    id: selectedOrder.id,
                    trackingNumber: selectedOrder.trackingNumber,
                    recipientName: selectedOrder.senderName || selectedOrder.recipientName,
                    recipientPhone: selectedOrder.senderPhone || selectedOrder.recipientPhone,
                    recipientAddress: selectedOrder.senderAddress || selectedOrder.recipientAddress || '',
                    codAmount: selectedOrder.cod || 0,
                    priority: 'normal',
                    serviceType: selectedOrder.serviceType || 'Tiêu chuẩn',
                    estimatedTime: '',
                    status: selectedOrder.status === 'PICKED_UP' ? 'completed' : 'in_progress',
                    coordinates: { lat: selectedOrder.latitude || 0, lng: selectedOrder.longitude || 0 },
                    distance: 0,
                    travelTime: 0,
                  }]}
                  deliverOffice={null}
                />
              );
            })()
          )}
        </Modal>
      </Card>
    </div>
  );
}
 
