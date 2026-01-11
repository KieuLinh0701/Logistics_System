import React, { useEffect, useState } from "react";
import { Card, Table, Button, Space, Typography, message, Tag, Modal, Descriptions, Checkbox } from "antd";
import { ReloadOutlined, PlayCircleOutlined, CheckCircleOutlined, CloseCircleOutlined } from "@ant-design/icons";
import shipmentApi from "../../api/shipmentApi";
import type { DriverShipment } from "../../types/shipment";
import { useNavigate } from "react-router-dom";

const { Title, Text } = Typography;

const DriverShipments: React.FC = () => {
  const navigate = useNavigate();
  type DriverOrderItem = NonNullable<DriverShipment['orders']>[number];
  const [shipments, setShipments] = useState<DriverShipment[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0 });
  const [pickupModal, setPickupModal] = useState<{ visible: boolean; shipmentId?: number; orders?: any[] }>({ visible: false });
  const [selectedOrderIds, setSelectedOrderIds] = useState<number[]>([]);

  useEffect(() => {
    loadShipments();
  }, [pagination.page, pagination.limit]);

  const loadShipments = async () => {
    try {
      setLoading(true);
      const res = await shipmentApi.getDriverShipments({
        page: pagination.page,
        limit: pagination.limit,
      });
      setShipments(Array.isArray(res.shipments) ? res.shipments : []);
      setPagination(res.pagination || pagination);
    } catch (e: any) {
      message.error("Không tải được danh sách chuyến hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleStartShipment = async (shipmentId: number) => {
    Modal.confirm({
      title: "Bắt đầu vận chuyển",
      content: "Bạn có chắc chắn muốn bắt đầu vận chuyển chuyến hàng này?",
      onOk: async () => {
        try {
          const res = await shipmentApi.startShipment(shipmentId);
          if (res && !res.success) {
            message.error(res.message || "Không thể bắt đầu chuyến");
            return;
          }
          message.success("Đã bắt đầu vận chuyển");
          loadShipments();
        } catch (e: any) {
          message.error(e?.message || "Lỗi khi bắt đầu vận chuyển");
        }
      },
    });
  };

  const openPickupModal = (shipment: DriverShipment) => {
    // Không tự động chọn; người dùng sẽ click thủ công hoặc dùng Chọn tất cả
    setSelectedOrderIds([]);
    setPickupModal({ visible: true, shipmentId: shipment.id, orders: shipment.orders || [] });
  };

  const handlePickupConfirm = async () => {
    if (!pickupModal.shipmentId) return;
    if (!selectedOrderIds || selectedOrderIds.length === 0) {
      message.warning("Vui lòng chọn ít nhất một đơn");
      return;
    }
    try {
      setLoading(true);
      const res = await shipmentApi.pickupShipmentOrders(pickupModal.shipmentId, selectedOrderIds);
      if (res && !res.success) {
        message.error(res.message || "Không thể xác nhận lấy hàng");
      } else {
        message.success(res?.message || "Đã đánh dấu đơn là đã lấy");
        setPickupModal({ visible: false });
        loadShipments();
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || "Lỗi khi xác nhận lấy hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleClaimShipment = async (shipmentId: number) => {
    Modal.confirm({
      title: "Nhận chuyến",
      content: "Bạn có muốn nhận chuyến này?",
      onOk: async () => {
        try {
          await shipmentApi.claimShipment(shipmentId);
          message.success("Bạn đã nhận chuyến");
          loadShipments();
        } catch (e: any) {
          message.error(e?.message || "Lỗi khi nhận chuyến");
        }
      },
    });
  };

  const handleFinishShipment = async (shipmentId: number, status: "COMPLETED" | "CANCELLED") => {
    Modal.confirm({
      title: status === "COMPLETED" ? "Hoàn tất chuyến hàng" : "Hủy chuyến hàng",
      content: `Bạn có chắc chắn muốn ${status === "COMPLETED" ? "hoàn tất" : "hủy"} chuyến hàng này?`,
      onOk: async () => {
        try {
          await shipmentApi.finishShipment({ shipmentId, status });
          message.success(status === "COMPLETED" ? "Đã hoàn tất chuyến hàng" : "Đã hủy chuyến hàng");
          loadShipments();
        } catch (e: any) {
          message.error(e?.message || "Lỗi khi hoàn tất chuyến hàng");
        }
      },
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "default";
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
        return "Chờ xử lý";
      case "IN_TRANSIT":
        return "Đang vận chuyển";
      case "COMPLETED":
        return "Hoàn thành";
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
    }
  };

  const translateVehicleType = (type?: string) => {
    if (!type) return "";
    switch (type.toString().toUpperCase()) {
      case "TRUCK":
        return "Xe tải";
      case "VAN":
        return "Xe van";
      case "CONTAINER":
        return "Xe container";
      default:
        return type;
    }
  };

  const formatStartTime = (iso?: string) => {
    if (!iso) return "-";
    try {
      const d = new Date(iso);
      const date = d.toLocaleDateString("vi-VN");
      const time = d.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
      return (
        <div>
          <div>{date}</div>
          <div>{time}</div>
        </div>
      );
    } catch (e) {
      return iso;
    }
  };

  const columns = [
    { title: "Mã chuyến", dataIndex: "code", key: "code" },
    {
      title: "Trạng thái",
      key: "status",
      render: (_: any, r: DriverShipment) => (
        <Tag color={getStatusColor(r.status)}>{getStatusText(r.status)}</Tag>
      ),
    },
    {
      title: "Phương tiện",
      key: "vehicle",
      render: (_: any, r: DriverShipment) => {
        if (!r.vehicle) return "-";
        return (
          <div>
            <div style={{ fontWeight: 700, color: "#111827" }}>{r.vehicle.licensePlate}</div>
            <div style={{ fontSize: 12, color: "#6b7280" }}>
              ({translateVehicleType(r.vehicle.type)})
            </div>
          </div>
        );
      },
    },
    {
      title: "Từ bưu cục",
      key: "fromOffice",
      render: (_: any, r: DriverShipment) => r.fromOffice?.name || "-",
    },
    {
      title: "Đến bưu cục",
      key: "toOffice",
      render: (_: any, r: DriverShipment) => r.toOffice?.name || "-",
    },
    { title: "Số đơn", dataIndex: "orderCount", key: "orderCount" },
    { title: "Thời gian bắt đầu", dataIndex: "startTime", key: "startTime", render: (_: any, r: DriverShipment) => formatStartTime(r.startTime) },
    {
      title: "Thao tác",
      key: "actions",
      render: (_: any, r: DriverShipment) => (
        <Space direction="vertical" size="small">
          {r.status === "PENDING" && (
            <>
              <Button type="primary" onClick={() => openPickupModal(r)} block>
                Lấy hàng
              </Button>
              {/** Disable Start unless all orders are PICKED_UP */}
              <Button
                style={{ marginTop: 8 }}
                type="default"
                icon={<PlayCircleOutlined />}
                onClick={() => {
                  const allPicked = Array.isArray(r.orders) && r.orders.length > 0 && r.orders.every((o: any) => o.status === "PICKED_UP");
                  if (!allPicked) {
                    message.error("Không thể bắt đầu: vẫn còn đơn chưa được xác nhận đã lấy (PICKED_UP)");
                    return;
                  }
                  handleStartShipment(r.id);
                }}
                disabled={!(Array.isArray(r.orders) && r.orders.length > 0 && r.orders.every((o: any) => o.status === "PICKED_UP"))}
                block
              >
                Bắt đầu
              </Button>
            </>
          )}
          {r.status === "IN_TRANSIT" && (
            <Space direction="vertical" style={{ width: "100%" }}>
              <Button
                type="primary"
                icon={<CheckCircleOutlined />}
                onClick={() => handleFinishShipment(r.id, "COMPLETED")}
                block
              >
                Hoàn tất
              </Button>
              <Button
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => handleFinishShipment(r.id, "CANCELLED")}
                block
              >
                Hủy
              </Button>
              <Button onClick={() => navigate(`/driver/route`)} block>
                Xem lộ trình
              </Button>
            </Space>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={<Title level={3}>Quản lý chuyến vận chuyển</Title>}
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadShipments}>
            Tải lại
          </Button>
        }
      >
        <Table
          rowKey="id"
          columns={columns}
          dataSource={shipments}
          loading={loading}
          pagination={{
            current: pagination.page,
            pageSize: pagination.limit,
            total: pagination.total,
            onChange: (page, pageSize) => {
              setPagination({ ...pagination, page, limit: pageSize });
            },
          }}
          expandable={{
            expandedRowRender: (record: DriverShipment) => (
              <div style={{ margin: 0 }}>
                <Title level={5}>Chi tiết đơn hàng trong chuyến</Title>
                {record.orders && record.orders.length > 0 ? (
                  <Descriptions size="small" column={2}>
                    {record.orders.map((order: DriverOrderItem, index: number) => (
                      <Descriptions.Item key={order.id} label={`Đơn ${index + 1}`}>
                        <Space direction="vertical" size={0}>
                          <Text strong>{order.trackingNumber}</Text>
                          <Text type="secondary">{order.toOffice?.name || "—"}</Text>
                        </Space>
                      </Descriptions.Item>
                    ))}
                  </Descriptions>
                ) : (
                  <Text type="secondary">Không có đơn hàng</Text>
                )}
              </div>
            ),
            rowExpandable: (record: DriverShipment) => !!(record.orders && record.orders.length > 0),
          }}
        />
        {/* Pickup modal */}
        <Modal
          title="Lấy hàng - Chọn đơn"
          open={pickupModal.visible}
          onOk={handlePickupConfirm}
          onCancel={() => setPickupModal({ visible: false })}
          okText="Xác nhận"
          cancelText="Hủy"
        >
          {pickupModal.orders && pickupModal.orders.length > 0 ? (
            (() => {
              const available = (pickupModal.orders || []).filter((o: any) => o.status !== "PICKED_UP");
              if (available.length === 0) {
                return <Text>Đã lấy tất cả các đơn</Text>;
              }
              return (
                <div>
                  <Checkbox
                    checked={selectedOrderIds.length > 0 && selectedOrderIds.length === available.length}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedOrderIds(available.map(o => o.id));
                      } else {
                        setSelectedOrderIds([]);
                      }
                    }}
                  >
                    Chọn tất cả
                  </Checkbox>

                  <Checkbox.Group
                    style={{ width: "100%", marginTop: 12 }}
                    value={selectedOrderIds}
                    onChange={(vals) => setSelectedOrderIds((vals as any[]).map(v => Number(v)))}
                  >
                    <Space direction="vertical" style={{ width: "100%" }}>
                      {available.map((o: any) => (
                        <Checkbox key={o.id} value={o.id}>
                          {o.trackingNumber}
                        </Checkbox>
                      ))}
                    </Space>
                  </Checkbox.Group>
                </div>
              );
            })()
          ) : (
            <Text>Không có đơn để chọn</Text>
          )}
        </Modal>
      </Card>
    </div>
  );
};

export default DriverShipments;
