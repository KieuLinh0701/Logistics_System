import React, {useEffect, useState} from "react";
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  Divider,
  Form,
  Input,
  message,
  Modal,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
} from "antd";
import {
  ArrowLeftOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  CompassOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  PhoneOutlined,
  PlayCircleOutlined,
  PictureOutlined,
} from "@ant-design/icons";
import {useNavigate, useParams} from "react-router-dom";
import dayjs from "dayjs";
import type {ShipperOrder} from "../../api/orderApi";
import orderApi from "../../api/orderApi";
import {getUserRole} from "../../utils/authUtils";
import {dispatchShipperRouteRefresh} from "./deliveryRouteEvents";
import {translatePaymentSubmissionStatus} from "../../utils/orderUtils";
import {
  canMarkDelivered,
  canMarkPickedUp,
  canStartDelivery,
  isInActiveDeliveryShipment,
} from "../../utils/orderActionGuards";

const { Title, Text, Paragraph } = Typography;

const ShipperOrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [order, setOrder] = useState<ShipperOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [deliveryModal, setDeliveryModal] = useState(false);
  const [codModal, setCodModal] = useState(false);
  const [failedModal, setFailedModal] = useState(false);
  const [pickedUpModal, setPickedUpModal] = useState(false);
  const [pickedUpImageFile, setPickedUpImageFile] = useState<File | null>(null);
  const [pickedUpImagePreview, setPickedUpImagePreview] = useState<string | null>(null);
  const [deliveryProofImageFile, setDeliveryProofImageFile] = useState<File | null>(null);
  const [deliveryProofImagePreview, setDeliveryProofImagePreview] = useState<string | null>(null);
  const [successModal, setSuccessModal] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [deliveryForm] = Form.useForm();
  const [codForm] = Form.useForm();
  const [failedForm] = Form.useForm();
  const [paymentSubmissionResponse, setPaymentSubmissionResponse] = useState<any | null>(null);

  const getCodPreviewItems = () => {
    if (!order || !order.orderProducts) return [];

    const baseItems = (order.orderProducts as any[]).map((p: any) => {
      const delivered = p.deliveredQuantity ?? 0;
      const price = p.price ?? p.productPrice ?? 0;
      const amount = delivered * price;
      return {
        productName: p.productName || "",
        deliveredQuantity: delivered,
        price,
        amount,
      };
    });

    const totalByDelivered = baseItems.reduce((s, it) => s + (it.amount || 0), 0);
    const orderCod = Number(order?.cod ?? 0);

    if (totalByDelivered <= 0 && orderCod > 0) {
      const first = baseItems[0];
      if (first) {
        return [
          {
            ...first,
            deliveredQuantity: first.deliveredQuantity > 0 ? first.deliveredQuantity : 1,
            amount: orderCod,
          },
        ];
      }
      return [
        {
          productName: "COD",
          deliveredQuantity: 1,
          price: orderCod,
          amount: orderCod,
        },
      ];
    }

    return baseItems;
  };

  const getTotalCodPreview = () => {
    const sum = getCodPreviewItems().reduce((s, it) => s + (it.amount || 0), 0);
    if (sum > 0) return sum;
    return Number(order?.codAmount ?? order?.cod ?? 0);
  };

  const getPayerText = () => {
    const payer = (order?.payer || "").toUpperCase();
    if (payer === "SHOP") return "Shop / Người gửi";
    if (payer === "CUSTOMER") return "Người nhận";
    return order?.payer || "—";
  };

  const getShippingFeeLabel = () => {
    const payer = (order?.payer || "").toUpperCase();
    if (payer === "CUSTOMER") return "Phí vận chuyển cần thu";
    return "Shop đã trả phí vận chuyển";
  };

  const getTotalNeedCollect = () => {
    const payer = (order?.payer || "").toUpperCase();
    const cod = Number(order?.cod || 0);
    const shippingFee = Number(order?.shippingFee || 0);
    if (payer === "CUSTOMER") return cod + shippingFee;
    return cod;
  };

  useEffect(() => {
    if (id) {
      fetchOrderDetail();
    }
  }, [id]);

  const fetchOrderDetail = async () => {
    try {
      setLoading(true);
      const res = await orderApi.getShipperOrderDetail(Number(id));

      if (!res) {
        setOrder(null);
        message.error("Không tìm thấy đơn hàng (API trả về dữ liệu rỗng)");
        return;
      }

      if ("data" in res) {
        setOrder((res as any).data || null);
      } else {
        setOrder(res as any);
      }

    } catch (error) {
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

  const uploadProofImageIfSelected = async (file: File | null) => {
    if (!file) return undefined;
    setUploading(true);
    try {
      const res = await orderApi.uploadShipperProofImage(file);
      return res?.data?.imageUrl || undefined;
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Upload ảnh minh chứng thất bại");
      throw error;
    } finally {
      setUploading(false);
    }
  };

  const handleConfirmPickedUp = async () => {
    if (!order) return;
    try {
      setLoading(true);
      const photoUrl = await uploadProofImageIfSelected(pickedUpImageFile);
      await orderApi.markShipperPickedUp(Number(id), {
        photoUrl: photoUrl || order.pickupProofImageUrl || undefined,
      });
      message.success("Đã xác nhận lấy hàng");
      setPickedUpImageFile(null);
      setPickedUpImagePreview(null);
      setPickedUpModal(false);
      fetchOrderDetail();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi đánh dấu đã lấy hàng");
    } finally {
      setLoading(false);
    }
  };

  const handleFinishDelivery = async () => {
    if (!order) return;
    try {
      setLoading(true);
      const proofImageUrl = await uploadProofImageIfSelected(deliveryProofImageFile);
      await orderApi.createDeliveryAttempt(Number(id), {
        status: "SUCCESS",
        note: "Đã giao thành công",
        proofImageUrl: proofImageUrl || undefined,
      });
      message.success("Đã giao thành công");
      setDeliveryProofImageFile(null);
      setDeliveryProofImagePreview(null);
      setSuccessModal(false);
      fetchOrderDetail();
      dispatchShipperRouteRefresh();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi cập nhật trạng thái");
    } finally {
      setLoading(false);
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
      const res = await orderApi.collectShipperCOD({
        orderId: Number(id),
        notes: values.codNotes,
      });

      setPaymentSubmissionResponse(res || null);

      message.success("Đã thu COD thành công");
      setCodModal(false);
      fetchOrderDetail();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi thu COD");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitFailedDelivery = async (values: any) => {
    try {
      setLoading(true);
      const proofImageUrl = await uploadProofImageIfSelected(deliveryProofImageFile);
      await orderApi.createDeliveryAttempt(Number(id), {
        status: "FAILED",
        failReason: mapFailReason(values?.reason),
        note: values?.detail || "",
        proofImageUrl: proofImageUrl || undefined,
      });
      message.success("Đã cập nhật trạng thái giao hàng");
      setFailedModal(false);
      setDeliveryProofImageFile(null);
      setDeliveryProofImagePreview(null);
      fetchOrderDetail();
      dispatchShipperRouteRefresh();
    } catch (error) {
      message.error("Lỗi khi cập nhật trạng thái");
    } finally {
      setLoading(false);
    }
  };

  const handleFailedDelivery = () => {
    failedForm.resetFields();
    setFailedModal(true);
  };

  const readFilePreview = (file: File) =>
    new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(reader.result as string);
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });

  const attachPickedUpImage = async (file: File) => {
    setPickedUpImageFile(file);
    setPickedUpImagePreview(await readFilePreview(file));
  };

  const attachDeliveryProofImage = async (file: File) => {
    setDeliveryProofImageFile(file);
    setDeliveryProofImagePreview(await readFilePreview(file));
  };

  const removePickedUpImage = () => {
    setPickedUpImageFile(null);
    setPickedUpImagePreview(null);
  };

  const removeDeliveryProofImage = () => {
    setDeliveryProofImageFile(null);
    setDeliveryProofImagePreview(null);
  };

  const renderImagePicker = ({
    file,
    preview,
    onChange,
    onRemove,
    label,
  }: {
    file: File | null;
    preview: string | null;
    onChange: (file: File) => void;
    onRemove: () => void;
    label: string;
  }) => (
    <div style={{ marginBottom: 12 }}>
      <Text strong>{label}</Text>
      <div style={{ marginTop: 8 }}>
        <input
          id={`${label}-input`}
          type="file"
          accept="image/*"
          style={{ display: "none" }}
          onChange={(event) => {
            const selected = event.target.files?.[0];
            if (selected) {
              onChange(selected);
              event.target.value = "";
            }
          }}
        />
        <Space>
          <Button
            icon={<PictureOutlined />}
            onClick={() => document.getElementById(`${label}-input`)?.click()}
          >
            Chọn ảnh
          </Button>
          {preview && (
            <Button danger onClick={onRemove}>
              Xóa ảnh
            </Button>
          )}
        </Space>
      </div>
      {preview && (
        <div style={{ marginTop: 12 }}>
          <img
            src={preview}
            alt={label}
            style={{ maxWidth: "100%", maxHeight: 220, borderRadius: 8, border: "1px solid #e5e7eb" }}
          />
        </div>
      )}
      {uploading && (
        <div style={{ marginTop: 8 }}>
          <Spin size="small" /> <Text type="secondary">Đang tải ảnh lên...</Text>
        </div>
      )}
    </div>
  );

  const mapFailReason = (label: string): string => {
    switch (label) {
      case "Khách không có mặt":
        return "RECIPIENT_NOT_AVAILABLE";
      case "Không liên lạc được":
        return "NO_RESPONSE";
      case "Sai địa chỉ":
        return "WRONG_ADDRESS";
      case "Khách từ chối nhận":
        return "RECIPIENT_REFUSED";
      case "Khách hẹn giao lại":
        return "RESCHEDULE_REQUESTED";
      default:
        return "OTHER";
    }
  };

  const handleNavigateToRoute = () => {
    navigate("/route");
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "default";
      case "CONFIRMED":
      case "AT_DEST_OFFICE":
        return "blue";
      case "PICKED_UP":
        return "orange";
      case "DELIVERING":
        return "processing";
      case "DELIVERED":
        return "success";
      case "FAILED_DELIVERY":
      case "DELIVERY_RETRY":
      case "DELIVERY_FAILED_FINAL":
      case "RETURNED":
      case "RETURN_FAILED_FINAL":
        return "error";
      case "RETURNING":
      case "RETURN_RETRY":
        return "warning";
      default:
        return "default";
    }
  };

  const getStatusText = (status: string) => {
    if (!status) return "";
    switch (status) {
      case "PENDING":
        return "Chờ xử lý";
      case "CONFIRMED":
        return "Đã xác nhận";
      case "AT_DEST_OFFICE":
        return "Đã đến bưu cục";
      case "READY_FOR_PICKUP":
        return "Sẵn sàng lấy";
      case "PICKED_UP":
        return "Đã lấy hàng";
      case "DELIVERING":
        return "Đang giao hàng";
      case "DELIVERED":
        return "Đã giao";
      case "FAILED_DELIVERY":
        return "Giao thất bại";
      case "DELIVERY_RETRY":
        return "Chờ nộp về bưu cục";
      case "DELIVERY_FAILED_FINAL":
        return "Giao thất bại cuối cùng";
      case "RETURNED":
        return "Đã hoàn";
      case "RETURNING":
        return "Đang hoàn";
      case "RETURN_RETRY":
        return "Hoàn lại";
      case "RETURN_FAILED_FINAL":
        return "Hoàn thất bại";
      default:
        return status;
    }
  };

  const buildAddress = (address: any, fallback?: string): string => {
    if (fallback) return fallback;
    if (typeof address === "string") return address;
    if (address?.fullAddress) return address.fullAddress;
    if (!address) return "";
    const parts = [address.detail, address.wardName, address.cityName].filter(Boolean);
    return parts.join(", ");
  };

  const recipientAddressText = buildAddress(
    order?.recipientAddress,
    order?.recipientFullAddress
  );

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
                {(getUserRole() === "shipper" || getUserRole() === "clerk") && order.status !== "PICKED_UP" && order.status !== "DELIVERED" && (
                  <Button
                    type="dashed"
                    disabled={!canMarkPickedUp(order)}
                    title={!canMarkPickedUp(order) ? "Đơn chưa thuộc chuyến DELIVERY đang chạy" : ""}
                    onClick={() => {
                      setPickedUpImageFile(null);
                      setPickedUpImagePreview(null);
                      setPickedUpModal(true);
                    }}
                  >
                    Đã lấy hàng
                  </Button>
                )}
                {order.status === "PICKED_UP" && (
                  <Button
                    type="primary"
                    icon={<PlayCircleOutlined />}
                    disabled={!canStartDelivery(order)}
                    title={!canStartDelivery(order) ? "Đơn chưa thuộc chuyến DELIVERY đang chạy" : ""}
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
                      disabled={!canMarkDelivered(order)}
                      onClick={() => {
                        setDeliveryProofImageFile(null);
                        setDeliveryProofImagePreview(null);
                        setSuccessModal(true);
                      }}
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

          {!isInActiveDeliveryShipment(order) && order.status !== "DELIVERED" && order.status !== "RETURNED" && order.status !== "CANCELLED" && (
            <Row justify="center">
              <Alert
                type="warning"
                showIcon
                description="Bạn không thể thao tác giao/hoàn khi đơn chưa được gắn vào chuyến."
              />
            </Row>
          )}

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
              <Descriptions.Item label="Chuyến vận chuyển">
                {order.shipmentCode ? (
                  <Tag color="blue">{order.shipmentCode}</Tag>
                ) : (
                  <Tag color="default">Chưa gắn chuyến</Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Dịch vụ">
                {typeof order.serviceType === "string"
                  ? order.serviceType
                  : (order.serviceType as any)?.name || ""}
              </Descriptions.Item>
              <Descriptions.Item label="Bưu cục hiện tại">
                {order.currentOffice?.name
                  ? order.currentOffice.name
                  : order.fromOffice?.name
                    ? `Chưa cập nhật - Bưu cục gốc: ${order.fromOffice.name}`
                    : "Chưa xác định"
                }
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

          {/* COD breakdown returned from collect API */}
          {paymentSubmissionResponse && (
            <Card title="Kết quả thu COD (hệ thống)" style={{ marginTop: 12 }}>
              <Alert
                message="COD được hệ thống tự động tính dựa trên số lượng giao thành công."
                type="info"
                showIcon
                style={{ marginBottom: 12 }}
              />

              <div style={{ marginBottom: 12 }}>
                <Text strong>Tổng COD: </Text>
                <Text style={{ color: '#f50', fontSize: 16 }}>{(paymentSubmissionResponse.totalAmount ?? 0).toLocaleString()}đ</Text>
              </div>

              {paymentSubmissionResponse.items && paymentSubmissionResponse.items.length > 0 ? (
                <Table
                  dataSource={paymentSubmissionResponse.items}
                  rowKey={(r: any, idx?: number) => r.orderProductId || r.productId || idx}
                  pagination={false}
                  columns={[
                    { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
                    { title: 'Số hệ thống', dataIndex: 'systemAmount', key: 'systemAmount', render: (v:number) => v?.toLocaleString() + 'đ' },
                    { title: 'Số thực thu', dataIndex: 'actualAmount', key: 'actualAmount', render: (v:number) => v?.toLocaleString() + 'đ' },
                    { title: 'Trạng thái', dataIndex: 'status', key: 'status', render: (s:string) => <Tag>{translatePaymentSubmissionStatus(s)}</Tag> },
                    { title: 'Ngày', dataIndex: 'paidAt', key: 'paidAt', render: (d:string) => d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '—' },
                    { title: 'Ghi chú', dataIndex: 'notes', key: 'notes' },
                  ]}
                />
              ) : (
                <Text type="secondary">Không có COD cần thu.</Text>
              )}
            </Card>
          )}

          {/* Partial delivery modal is opened when user clicks Giao 1 phần */}

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
                  <Text>{recipientAddressText}</Text>
                </Space>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* Payment Info */}
          <Card title="Thông tin thanh toán">
            <Descriptions column={2} bordered>
              <Descriptions.Item label="Người trả phí vận chuyển">
                {getPayerText()}
              </Descriptions.Item>
              <Descriptions.Item label="COD thu hộ">
                <Space>
                  <DollarOutlined style={{ color: "#f50" }} />
                  <Text strong style={{ color: "#f50", fontSize: 16 }}>
                    {order.cod > 0 ? `${order.cod.toLocaleString()}đ` : "0đ"}
                  </Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label={getShippingFeeLabel()}>
                <Space>
                  <DollarOutlined style={{ color: "#f50" }} />
                  <Text strong style={{ color: "#f50", fontSize: 16 }}>
                    {(order.payer || "").toUpperCase() === "CUSTOMER"
                      ? `${Number(order.shippingFee || 0).toLocaleString()}đ`
                      : "0đ"}
                  </Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Tổng tiền cần thu">
                <Space>
                  <DollarOutlined style={{ color: "#f50" }} />
                  <Text strong style={{ color: "#f50", fontSize: 16 }}>
                    {`${getTotalNeedCollect().toLocaleString()}đ`}
                  </Text>
                </Space>
              </Descriptions.Item>
            </Descriptions>
          </Card>

          {/* Payment submissions (COD transactions) */}
          <Card title="Giao dịch thu tiền" style={{ marginTop: 12 }}>
            {order.paymentSubmissions && order.paymentSubmissions.length > 0 ? (
              <Table
                dataSource={order.paymentSubmissions}
                rowKey="id"
                pagination={false}
                columns={[
                  { title: "Mã giao dịch", dataIndex: "code", key: "code" },
                  { title: "Số hệ thống", dataIndex: "systemAmount", key: "systemAmount", render: (v:number) => v?.toLocaleString() + 'đ' },
                  { title: "Số thực thu", dataIndex: "actualAmount", key: "actualAmount", render: (v:number) => v?.toLocaleString() + 'đ' },
                  { title: "Trạng thái", dataIndex: "status", key: "status", render: (s:string) => <Tag>{translatePaymentSubmissionStatus(s)}</Tag> },
                  { title: 'Ngày', dataIndex: 'paidAt', key: 'paidAt', render: (d:string) => d ? dayjs(d).format('DD/MM/YYYY HH:mm') : '—' },
                  { title: "Ghi chú", dataIndex: "notes", key: "notes" },
                ]}
              />
            ) : (
              <Text type="secondary">Chưa có giao dịch COD liên quan.</Text>
            )}
          </Card>

          {/* Notes */}
          {order.notes && (
            <Card title="Ghi chú">
              <Paragraph>{order.notes}</Paragraph>
            </Card>
          )}
        </Space>
      </Card>

      {/* Modal: Xác nhận đã lấy hàng */}
      <Modal
        title="Xác nhận đã lấy hàng"
        open={pickedUpModal}
        onOk={handleConfirmPickedUp}
        onCancel={() => setPickedUpModal(false)}
        confirmLoading={uploading}
        width={640}
      >
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Alert
            message="Bạn đang xác nhận lấy hàng cho đơn này."
            type="info"
            showIcon
          />
          {renderImagePicker({
            file: pickedUpImageFile,
            preview: pickedUpImagePreview,
            onChange: attachPickedUpImage,
            onRemove: removePickedUpImage,
            label: "Ảnh minh chứng lấy hàng",
          })}
        </Space>
      </Modal>

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
            description="Bạn sẽ được chuyển đến trang lộ trình vận chuyển sau khi xác nhận."
            type="info"
            showIcon
            style={{ marginBottom: 16 }}
          />
          <Form.Item name="notes" label="Ghi chú">
            <Input.TextArea rows={3} placeholder="Ghi chú về việc bắt đầu giao hàng (nếu có)" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Modal: Thu COD */}
      <Modal
        title="Thu COD"
        open={codModal}
        onOk={() => codForm.submit()}
        onCancel={() => setCodModal(false)}
        width={600}
        okText="Xác nhận thu COD"
      >
          <Form form={codForm} layout="vertical" onFinish={handleSubmitCOD}>
            <div style={{ marginBottom: 12 }}>
              <Text strong>COD cần thu: </Text>
              <Text style={{ color: '#f50', fontSize: 16 }}>{getTotalCodPreview().toLocaleString()}đ</Text>
            </div>

            {getCodPreviewItems().length > 0 ? (
              <Table
                dataSource={getCodPreviewItems()}
                rowKey={(r: any, idx?: number) => (r.productName || r.name || '') + "_" + (idx ?? 0)}
                pagination={false}
                size="small"
                columns={[
                  { title: 'Sản phẩm', dataIndex: 'productName', key: 'productName' },
                  { title: 'SL giao', dataIndex: 'deliveredQuantity', key: 'deliveredQuantity' },
                  { title: 'Giá', dataIndex: 'price', key: 'price', render: (v:number) => v?.toLocaleString() + 'đ' },
                  { title: 'Thành tiền', dataIndex: 'amount', key: 'amount', render: (v:number) => v?.toLocaleString() + 'đ' },
                ]}
                style={{ marginBottom: 12 }}
              />
            ) : (
              <Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>Không có COD cần thu.</Text>
            )}

            <Form.Item name="codNotes" label="Ghi chú về COD (tuỳ chọn)">
              <Input.TextArea rows={3} placeholder="Ghi chú về việc thu COD (nếu có)" />
            </Form.Item>
            <Form.Item name="notes" label="Ghi chú về giao hàng (tuỳ chọn)">
              <Input.TextArea rows={3} placeholder="Ghi chú về việc giao hàng (nếu có)" />
            </Form.Item>
          </Form>
      </Modal>

      {/* Modal: Giao thất bại (bắt buộc nhập lý do) */}
      <Modal
        title="Báo giao thất bại"
        open={failedModal}
        onOk={() => failedForm.submit()}
        onCancel={() => setFailedModal(false)}
        width={640}
        okText="Xác nhận"
      >
        <Form
          form={failedForm}
          layout="vertical"
          onFinish={(values: any) => {
            setDeliveryProofImageFile(null);
            setDeliveryProofImagePreview(null);
            handleSubmitFailedDelivery(values);
          }}
        >
          <Space direction="vertical" size="large" style={{ width: "100%" }}>
            {renderImagePicker({
              file: deliveryProofImageFile,
              preview: deliveryProofImagePreview,
              onChange: attachDeliveryProofImage,
              onRemove: removeDeliveryProofImage,
              label: "Ảnh minh chứng giao thất bại",
            })}
            <Form.Item
              name="reason"
              label="Lý do thất bại"
              rules={[{ required: true, message: "Vui lòng chọn lý do thất bại" }]}
            >
              <Select placeholder="Chọn lý do thất bại">
                <Select.Option value="Khách không có mặt">Khách không có mặt</Select.Option>
                <Select.Option value="Không liên lạc được">Không liên lạc được</Select.Option>
                <Select.Option value="Sai địa chỉ">Sai địa chỉ</Select.Option>
                <Select.Option value="Khách từ chối nhận">Khách từ chối nhận</Select.Option>
                <Select.Option value="Khách hẹn giao lại">Khách hẹn giao lại</Select.Option>
                <Select.Option value="Khác">Khác</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item name="detail" label="Chi tiết (nếu có)">
              <Input.TextArea rows={3} placeholder="Mô tả chi tiết lý do (tuỳ chọn)" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      {/* Modal: Giao thành công */}
      <Modal
        title="Xác nhận giao thành công"
        open={successModal}
        onOk={handleFinishDelivery}
        onCancel={() => setSuccessModal(false)}
        confirmLoading={uploading}
        width={640}
        okText="Xác nhận giao"
      >
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Alert
            message="Xác nhận giao hàng thành công"
            description="Đơn hàng sẽ được đánh dấu là đã giao."
            type="success"
            showIcon
          />
          {renderImagePicker({
            file: deliveryProofImageFile,
            preview: deliveryProofImagePreview,
            onChange: attachDeliveryProofImage,
            onRemove: removeDeliveryProofImage,
            label: "Ảnh minh chứng giao hàng thành công (tuỳ chọn)",
          })}
        </Space>
      </Modal>
    </div>
  );
};

export default ShipperOrderDetail;
