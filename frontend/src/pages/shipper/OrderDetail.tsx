import React, { useState, useEffect } from "react";
import {
  Card,
  Row,
  Col,
  Typography,
  Descriptions,
  Tag,
  Button,
  Space,
  Divider,
  Modal,
  message,
  Spin,
  Form,
  InputNumber,
  Input,
  Alert,
} from "antd";
import {
  ArrowLeftOutlined,
  PhoneOutlined,
  EnvironmentOutlined,
  DollarOutlined,
  PlayCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CompassOutlined,
} from "@ant-design/icons";
import { useParams, useNavigate } from "react-router-dom";
import dayjs from "dayjs";
import orderApi from "../../api/orderApi";
import type { ShipperOrder } from "../../api/orderApi";

const { Title, Text, Paragraph } = Typography;

const ShipperOrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<ShipperOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [deliveryModal, setDeliveryModal] = useState(false);
  const [codModal, setCodModal] = useState(false);
  const [deliveryForm] = Form.useForm();
  const [codForm] = Form.useForm();

  useEffect(() => {
    if (id) {
      fetchOrderDetail();
    }
  }, [id]);

const fetchOrderDetail = async () => {
  try {
    setLoading(true);
    console.log(`[DEBUG] Fetching order detail for id=${id}`);

    const res = await orderApi.getShipperOrderDetail(Number(id));
    console.log("[DEBUG] Response from API:", res);

    if (!res) {
      console.warn("[WARN] API trả về dữ liệu null/undefined");
      setOrder(null);
      message.error("Không tìm thấy đơn hàng (API trả về dữ liệu rỗng)");
      return;
    }

    // Nếu API có cấu trúc dữ liệu khác, ví dụ data ở trong res.data
    // Bạn cũng nên log để kiểm tra
    if ("data" in res) {
      console.log("[DEBUG] Response có trường data:", (res as any).data);
      setOrder((res as any).data || null);
    } else {
      setOrder(res as any);
    }

  } catch (error) {
    console.error("Error fetching order detail:", error);
    message.error("Lỗi khi tải thông tin đơn hàng");
    setOrder(null);
  } finally {
    setLoading(false);
  }
};


  const handleStartDelivery = () => {
    deliveryForm.resetFields();
    setDeliveryModal(true);
  };

  const handleFinishDelivery = () => {
    if (!order) return;

    // Nếu có COD, hiển thị form thu COD
    if (order.cod && order.cod > 0) {
      codForm.setFieldsValue({ actualAmount: order.cod });
      setCodModal(true);
    } else {
      // Không có COD, cập nhật trạng thái trực tiếp
      updateDeliveryStatus("DELIVERED", "");
    }
  };

  const handleSubmitDelivery = async (values: any) => {
    try {
      setLoading(true);
      await orderApi.updateShipperDeliveryStatus(Number(id), {
        status: "DELIVERING",
        notes: values.notes,
      });
      message.success("Đã bắt đầu giao hàng");
      setDeliveryModal(false);
      fetchOrderDetail();
      // Chuyển đến trang lộ trình
      navigate("/route");
    } catch (error) {
      message.error("Lỗi khi cập nhật trạng thái");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitCOD = async (values: any) => {
    try {
      setLoading(true);
      // Cập nhật trạng thái đơn hàng thành DELIVERED
      await orderApi.updateShipperDeliveryStatus(Number(id), {
        status: "DELIVERED",
        notes: values.notes,
      });

      // Thu COD - tạo transaction INCOME
      await orderApi.collectShipperCOD({
        orderId: Number(id),
        actualAmount: values.actualAmount,
        notes: values.codNotes,
      });

      message.success("Đã giao hàng thành công và thu COD");
      setCodModal(false);
      fetchOrderDetail();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi xử lý giao hàng và thu COD");
    } finally {
      setLoading(false);
    }
  };

  const updateDeliveryStatus = async (status: string, notes?: string) => {
    try {
      setLoading(true);
      await orderApi.updateShipperDeliveryStatus(Number(id), {
        status,
        notes,
      });
      message.success("Đã cập nhật trạng thái giao hàng");
      fetchOrderDetail();
    } catch (error) {
      message.error("Lỗi khi cập nhật trạng thái");
    } finally {
      setLoading(false);
    }
  };

  const handleFailedDelivery = () => {
    Modal.confirm({
      title: "Xác nhận giao thất bại",
      content: "Đơn hàng sẽ được chuyển sang trạng thái GIAO THẤT BẠI.",
      onOk: () => updateDeliveryStatus("FAILED_DELIVERY", ""),
    });
  };

  const handleNavigateToRoute = () => {
    navigate("/route");
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PICKED_UP":
        return "orange";
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

  const buildAddress = (address: any): string => {
    if (typeof address === "string") return address;
    if (!address) return "";
    const parts = [
      address.detailAddress,
      address.ward?.name,
      address.district?.name,
      address.province?.name,
    ].filter(Boolean);
    return parts.join(", ");
  };

  if (loading && !order) {
    return (
      <div style={{ textAlign: "center", padding: 50 }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!order) {
    return (
      <Alert
        message="Không tìm thấy đơn hàng"
        description="Đơn hàng không tồn tại hoặc bạn không có quyền xem"
        type="error"
        showIcon
        action={
          <Button onClick={() => navigate("/shipper/orders")}>Quay lại</Button>
        }
      />
    );
  }

  return (
    <div style={{ padding: 24, background: "#F9FAFB", minHeight: "100vh" }}>
      <Card>
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          {/* Header */}
          <Row justify="space-between" align="middle">
            <Col>
              <Space>
                <Button icon={<ArrowLeftOutlined />} onClick={() => navigate("/shipper/orders")}>
                  Quay lại
                </Button>
                <Title level={3} style={{ margin: 0 }}>
                  Chi tiết đơn hàng
                </Title>
              </Space>
            </Col>
            <Col>
              <Space>
                {order.status === "PICKED_UP" && (
                  <Button
                    type="primary"
                    icon={<PlayCircleOutlined />}
                    onClick={handleStartDelivery}
                  >
                    Bắt đầu giao hàng
                  </Button>
                )}
                {order.status === "DELIVERING" && (
                  <>
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      onClick={handleFinishDelivery}
                    >
                      Giao thành công
                    </Button>
                    <Button
                      danger
                      icon={<CloseCircleOutlined />}
                      onClick={handleFailedDelivery}
                    >
                      Giao thất bại
                    </Button>
                    <Button icon={<CompassOutlined />} onClick={handleNavigateToRoute}>
                      Xem lộ trình
                    </Button>
                  </>
                )}
              </Space>
            </Col>
          </Row>

          <Divider />

          {/* Order Info */}
          <Card title="Thông tin đơn hàng">
            <Descriptions column={2} bordered>
              <Descriptions.Item label="Mã đơn hàng">
                <Text strong>{order.trackingNumber}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={getStatusColor(order.status)}>{getStatusText(order.status)}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Dịch vụ">
                {typeof order.serviceType === "string"
                  ? order.serviceType
                  : (order.serviceType as any)?.name || ""}
              </Descriptions.Item>
              <Descriptions.Item label="Ngày tạo">
                {dayjs(order.createdAt).format("DD/MM/YYYY HH:mm")}
              </Descriptions.Item>
              {order.deliveredAt && (
                <Descriptions.Item label="Ngày giao">
                  {dayjs(order.deliveredAt).format("DD/MM/YYYY HH:mm")}
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          {/* Recipient Info */}
          <Card title="Thông tin người nhận">
            <Descriptions column={1} bordered>
              <Descriptions.Item label="Họ tên">
                <Text strong>{order.recipientName}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Số điện thoại">
                <Space>
                  <PhoneOutlined />
                  <Text>{order.recipientPhone}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Địa chỉ">
                <Space>
                  <EnvironmentOutlined />
                  <Text>{buildAddress(order.recipientAddress)}</Text>
                </Space>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* Payment Info */}
          <Card title="Thông tin thanh toán">
            <Descriptions column={2} bordered>
              <Descriptions.Item label="Phí vận chuyển">
                {order.shippingFee ? `${order.shippingFee.toLocaleString()}đ` : "—"}
              </Descriptions.Item>
              <Descriptions.Item label="COD">
                <Space>
                  <DollarOutlined style={{ color: "#f50" }} />
                  <Text strong style={{ color: "#f50", fontSize: 16 }}>
                    {order.cod ? `${order.cod.toLocaleString()}đ` : "Không COD"}
                  </Text>
                </Space>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* Notes */}
          {order.notes && (
            <Card title="Ghi chú">
              <Paragraph>{order.notes}</Paragraph>
            </Card>
          )}
        </Space>
      </Card>

      {/* Modal: Bắt đầu giao hàng */}
      <Modal
        title="Bắt đầu giao hàng"
        open={deliveryModal}
        onOk={() => deliveryForm.submit()}
        onCancel={() => setDeliveryModal(false)}
        width={600}
      >
        <Form form={deliveryForm} layout="vertical" onFinish={handleSubmitDelivery}>
          <Alert
            message="Xác nhận bắt đầu giao hàng"
            description="Bạn sẽ được chuyển đến trang lộ trình giao hàng sau khi xác nhận."
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form.Item name="notes" label="Ghi chú">
            <Input.TextArea rows={3} placeholder="Ghi chú về việc bắt đầu giao hàng (nếu có)" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal: Thu COD khi giao thành công */}
      <Modal
        title="Giao hàng thành công - Thu COD"
        open={codModal}
        onOk={() => codForm.submit()}
        onCancel={() => setCodModal(false)}
        width={600}
        okText="Xác nhận giao hàng và thu COD"
      >
        <Form form={codForm} layout="vertical" onFinish={handleSubmitCOD}>
          <Alert
            message="Đơn hàng có COD"
            description={`Số tiền COD theo hệ thống: ${order.cod?.toLocaleString()}đ. Vui lòng nhập số tiền thực tế đã thu.`}
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form.Item
            name="actualAmount"
            label="Số tiền thực thu (đ)"
            rules={[
              { required: true, message: "Vui lòng nhập số tiền thực thu" },
              { type: "number", min: 0, message: "Số tiền phải lớn hơn 0" },
            ]}
          >
            <InputNumber<number>
              style={{ width: "100%" }}
              formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")}
              parser={(value) => Number(value?.replace(/[^\d]/g, "") || 0)}
              min={0}
              placeholder="Nhập số tiền thực thu"
            />
          </Form.Item>
          <Form.Item name="codNotes" label="Ghi chú về COD">
            <Input.TextArea rows={3} placeholder="Ghi chú về việc thu COD (nếu có)" />
          </Form.Item>
          <Form.Item name="notes" label="Ghi chú về giao hàng">
            <Input.TextArea rows={3} placeholder="Ghi chú về việc giao hàng (nếu có)" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ShipperOrderDetail;
