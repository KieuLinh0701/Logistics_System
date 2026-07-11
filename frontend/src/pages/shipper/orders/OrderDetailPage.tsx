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
    InputNumber,
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
    PictureOutlined,
    PlayCircleOutlined,
} from "@ant-design/icons";
import {useLocation, useNavigate, useParams} from "react-router-dom";
import dayjs from "dayjs";
import type {ShipperOrder} from "../../../api/orderApi";
import orderApi from "../../../api/orderApi";
import {getUserRole} from "../../../utils/authUtils";
import {dispatchShipperRouteRefresh} from "../delivery-route/deliveryRouteEvents";
import {translatePaymentSubmissionStatus} from "../../../utils/orderUtils";
import PickupAttemptModal from "../shared/components/PickupAttemptModal";
import {
    canMarkDelivered,
    canMarkPickedUp,
    canMarkReturnDelivered,
    canStartDelivery,
    isInActiveDeliveryShipment,
    isInActiveReturnShipment,
    isReturnOrder,
} from "../../../utils/orderActionGuards";
import "../ShipperPagesShared.css";

const { Title, Text, Paragraph } = Typography;

const ShipperOrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const location = useLocation();
  const [order, setOrder] = useState<ShipperOrder | null>(null);
  const [loading, setLoading] = useState(true);
  const [accepting, setAccepting] = useState(false);
  const [deliveryModal, setDeliveryModal] = useState(false);
  const [codModal, setCodModal] = useState(false);
  const [failedModal, setFailedModal] = useState(false);
  const [pickedUpModal, setPickedUpModal] = useState(false);
  const [pickedUpImageFile, setPickedUpImageFile] = useState<File | null>(null);
  const [pickedUpImagePreview, setPickedUpImagePreview] = useState<string | null>(null);
  const [deliveryProofImageFile, setDeliveryProofImageFile] = useState<File | null>(null);
  const [deliveryProofImagePreview, setDeliveryProofImagePreview] = useState<string | null>(null);
  const [successModal, setSuccessModal] = useState(false);
  const [returnDeliveryModal, setReturnDeliveryModal] = useState(false);
  const [returnDeliveryProofImageFile, setReturnDeliveryProofImageFile] = useState<File | null>(null);
  const [returnDeliveryProofImagePreview, setReturnDeliveryProofImagePreview] = useState<string | null>(null);
  const [returnFailedModal, setReturnFailedModal] = useState(false);
  const [returnFailedForm] = Form.useForm();
  const [returnFailedProofImageFile, setReturnFailedProofImageFile] = useState<File | null>(null);
  const [returnFailedProofImagePreview, setReturnFailedProofImagePreview] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [deliveryForm] = Form.useForm();
  const [codForm] = Form.useForm();
  const [failedForm] = Form.useForm();
  const [successForm] = Form.useForm();
  const [paymentSubmissionResponse, setPaymentSubmissionResponse] = useState<any | null>(null);
  const [collectionMode, setCollectionMode] = useState<"FULL" | "CUSTOM">("FULL");
  const [customCollectedAmount, setCustomCollectedAmount] = useState<number | undefined>(undefined);

  // Pickup action states
  const [pickupFailedModalOpen, setPickupFailedModalOpen] = useState(false);
  const [confirmPickupModalVisible, setConfirmPickupModalVisible] = useState(false);
  const [confirmPickupImageFile, setConfirmPickupImageFile] = useState<File | null>(null);
  const [confirmPickupImagePreview, setConfirmPickupImagePreview] = useState<string | null>(null);

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

      const payload: any = {
        status: "SUCCESS",
        note: "Đã giao thành công",
        proofImageUrl: proofImageUrl || undefined,
        collectionMode,
      };

      if (collectionMode === "CUSTOM" && customCollectedAmount !== undefined) {
        payload.actualCollected = customCollectedAmount;
      }

      const collectionNote = successForm.getFieldValue("collectionNote");
      if (collectionNote) {
        payload.collectionNote = collectionNote;
      }

      await orderApi.createDeliveryAttempt(Number(id), payload);
      message.success("Đã giao thành công");
      setDeliveryProofImageFile(null);
      setDeliveryProofImagePreview(null);
      setSuccessModal(false);
      setCollectionMode("FULL");
      setCustomCollectedAmount(undefined);
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

  const handleReturnDelivery = async () => {
    if (!order) return;
    try {
      setLoading(true);
      const proofImageUrl = await uploadProofImageIfSelected(returnDeliveryProofImageFile);
      await orderApi.markReturnDelivered(Number(id), proofImageUrl);
      message.success("Đã xác nhận giao trả hàng hoàn thành công");
      setReturnDeliveryProofImageFile(null);
      setReturnDeliveryProofImagePreview(null);
      setReturnDeliveryModal(false);
      fetchOrderDetail();
      dispatchShipperRouteRefresh();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi xác nhận giao trả hàng hoàn");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenReturnDeliveryModal = () => {
    setReturnDeliveryProofImageFile(null);
    setReturnDeliveryProofImagePreview(null);
    setReturnDeliveryModal(true);
  };

  const handleOpenReturnFailedModal = () => {
    returnFailedForm.resetFields();
    setReturnFailedProofImageFile(null);
    setReturnFailedProofImagePreview(null);
    setReturnFailedModal(true);
  };

  const handleSubmitReturnFailed = async (values: { failReason: string; note?: string }) => {
    if (!order) return;
    try {
      setLoading(true);
      const proofImageUrl = await uploadProofImageIfSelected(returnFailedProofImageFile);
      await orderApi.markReturnFailedFinal(order.id, {
        failReason: values.failReason,
        notes: values.note,
        proofImageUrl: proofImageUrl || undefined,
      });
      message.success("Đã ghi nhận giao hoàn thất bại");
      setReturnFailedModal(false);
      setReturnFailedProofImageFile(null);
      setReturnFailedProofImagePreview(null);
      returnFailedForm.resetFields();
      fetchOrderDetail();
      dispatchShipperRouteRefresh();
    } catch (error: any) {
      message.error(error?.response?.data?.message || "Lỗi khi ghi nhận giao hoàn thất bại");
    } finally {
      setLoading(false);
    }
  };

  // ========== PICKUP ACTIONS ==========

  const isPickupOrder = (order: ShipperOrder) => {
    const status = order.status;
    return [
      "READY_FOR_PICKUP",
      "URGENT_PICKUP",
      "PICKING_UP",
      "PICKUP_RETRY",
      "PICKED_UP",
      "AT_ORIGIN_OFFICE",
    ].includes(status);
  };

  const handleAcceptPickup = async () => {
    if (!order) return;
    try {
      setAccepting(true);
      const res: any = await orderApi.claimShipperOrderRequest(order.id);
      const data = res?.data ?? res;
      const isSuccess = res?.success !== false && data?.success !== false;
      const requiresReoptimize = data?.requiresReoptimize === true;

      const msg = data?.message || res?.message || (requiresReoptimize
          ? "Đơn đã được thêm vào chuyến lấy hàng hiện tại."
          : isSuccess
            ? "Nhận yêu cầu lấy hàng thành công."
            : "Không thể nhận yêu cầu lấy hàng");

      if (isSuccess) {
        message.success(msg);
      } else {
        message.error(msg);
      }

      if (requiresReoptimize) {
        message.warning("Đơn đã được thêm vào chuyến đang chạy. Vui lòng tối ưu lại tuyến.");
      }

      if (isSuccess) {
        dispatchShipperRouteRefresh();
      }

      fetchOrderDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.message || e?.message || "Lỗi khi nhận yêu cầu");
    } finally {
      setAccepting(false);
    }
  };

  const handleRetryPickup = async () => {
    if (!order) return;
    try {
      setLoading(true);
      await orderApi.retryPickup(order.id);
      message.success("Đã tiến hành đến lấy lại. Người gửi sẽ được thông báo.");
      fetchOrderDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Lỗi khi tiến hành lấy lại");
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitPickupFailed = async (values: { failReason: string; note?: string; file?: File | null }) => {
    if (!order) return;
    try {
      setLoading(true);
      let proofImageUrl: string | undefined;
      if (values.file) {
        proofImageUrl = await uploadProofImageIfSelected(values.file);
      }
      await orderApi.recordPickupAttempt(order.id, {
        status: "FAILED",
        failReason: values.failReason,
        note: values.note,
        proofImageUrl: proofImageUrl || undefined,
      });
      message.success("Đã ghi nhận lấy hàng thất bại");
      setPickupFailedModalOpen(false);
      fetchOrderDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Lỗi khi báo lấy hàng thất bại");
    } finally {
      setLoading(false);
    }
  };

  const handleOpenConfirmPickup = () => {
    setConfirmPickupImageFile(null);
    setConfirmPickupImagePreview(null);
    setConfirmPickupModalVisible(true);
  };

  const handleSelectPickupImage = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setConfirmPickupImageFile(file);
      const reader = new FileReader();
      reader.onload = () => setConfirmPickupImagePreview(reader.result as string);
      reader.readAsDataURL(file);
    }
    e.target.value = "";
  };

  const handleRemovePickupImage = () => {
    setConfirmPickupImageFile(null);
    setConfirmPickupImagePreview(null);
  };

  const handleConfirmPickupWithImage = async () => {
    if (!order) return;
    setUploading(true);
    try {
      let photoUrl: string | undefined;
      if (confirmPickupImageFile) {
        const uploadRes: any = await orderApi.uploadShipperProofImage(confirmPickupImageFile);
        photoUrl = uploadRes?.data?.imageUrl || (uploadRes as any)?.imageUrl || undefined;
      }
      await orderApi.markShipperPickedUp(order.id, { photoUrl });
      message.success("Đã xác nhận đã lấy hàng");
      setConfirmPickupModalVisible(false);
      fetchOrderDetail();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Lỗi khi xác nhận đã lấy");
    } finally {
      setUploading(false);
    }
  };

  const handleDeliverToOrigin = async () => {
    if (!order) return;
    try {
      setLoading(true);
      await orderApi.deliverShipperToOrigin(order.id, {});
      message.success("Đã nộp hàng tại bưu cục");
      fetchOrderDetail();
    } catch (e) {
      message.error("Lỗi khi nộp tại bưu cục");
    } finally {
      setLoading(false);
    }
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

  const attachReturnDeliveryProofImage = async (file: File) => {
    setReturnDeliveryProofImageFile(file);
    setReturnDeliveryProofImagePreview(await readFilePreview(file));
  };

  const attachReturnFailedProofImage = async (file: File) => {
    setReturnFailedProofImageFile(file);
    setReturnFailedProofImagePreview(await readFilePreview(file));
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
    showLabel = true,
  }: {
    file: File | null;
    preview: string | null;
    onChange: (file: File) => void;
    onRemove: () => void;
    label: string;
    showLabel?: boolean;
  }) => (
    <div style={{ marginBottom: 12 }}>
      {showLabel && <Text strong>{label}</Text>}
      <div style={{ marginTop: showLabel ? 8 : 0 }}>
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
      case "RETURN_AT_ORIGIN_OFFICE":
        return "warning";
      case "RETURN_READY_FOR_PICKUP":
      case "RETURN_PICKED_UP":
        return "gold";
      case "READY_FOR_PICKUP":
      case "URGENT_PICKUP":
        return "blue";
      case "PICKING_UP":
      case "PICKUP_RETRY":
        return "orange";
      case "AT_ORIGIN_OFFICE":
        return "green";
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
        return "Tại bưu cục";
      case "READY_FOR_PICKUP":
        return "Sẵn sàng lấy hàng";
      case "URGENT_PICKUP":
        return "Ưu tiên lấy hàng";
      case "PICKED_UP":
        return "Đã lấy hàng";
      case "PICKING_UP":
        return "Đang lấy hàng";
      case "PICKUP_RETRY":
        return "Lấy hàng thất bại - Thử lại";
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
        return "Đang hoàn trả";
      case "RETURN_AT_ORIGIN_OFFICE":
        return "Đã hoàn về bưu cục gốc";
      case "RETURN_READY_FOR_PICKUP":
        return "Sẵn sàng lấy hàng hoàn";
      case "RETURN_PICKED_UP":
        return "Đã lấy hàng hoàn lên xe";
      case "RETURN_RETRY":
        return "Hoàn lại";
      case "RETURN_FAILED_FINAL":
        return "Hoàn thất bại";
      case "AT_ORIGIN_OFFICE":
        return "Đã nộp tại bưu cục";
      case "CANCELLED":
        return "Đã huỷ";
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

  const returnStatusSet = new Set([
    "RETURN_AT_ORIGIN_OFFICE",
    "RETURN_READY_FOR_PICKUP",
    "RETURN_RETRY",
  ]);
  const isReturnStatus = returnStatusSet.has(String(order?.status || ""));
  const deliveryDestinationType =
    (order as any)?.destinationType === "SENDER_RETURN" || isReturnStatus
      ? "SENDER_RETURN"
      : "RECIPIENT";

  const deliveryContactName =
    (order as any)?.displayContactName ||
    (deliveryDestinationType === "SENDER_RETURN"
      ? order?.senderName
      : order?.recipientName) ||
    "";
  const deliveryContactPhone =
    (order as any)?.displayContactPhone ||
    (deliveryDestinationType === "SENDER_RETURN"
      ? order?.senderPhone
      : order?.recipientPhone) ||
    "";
  const deliveryContactAddress =
    (order as any)?.displayContactAddress ||
    (deliveryDestinationType === "SENDER_RETURN"
      ? buildAddress(
          (order as any)?.senderAddress,
          (order as any)?.senderFullAddress
        )
      : recipientAddressText) ||
    "";
  const deliveryContactType =
    (order as any)?.displayContactType ||
    (deliveryDestinationType === "SENDER_RETURN"
      ? "Shop/Người gửi"
      : "Người nhận");
  const deliveryCardTitle =
    deliveryDestinationType === "SENDER_RETURN"
      ? "Thông tin shop/người gửi"
      : "Thông tin người nhận";

  const navigateToOrders = () => {
    // Hỗ trợ cả `from` (My Orders tabs, route page) và `returnTo` (legacy)
    const from = location.state?.from as string | undefined;
    const returnTo = location.state?.returnTo as string | undefined;
    navigate(from || returnTo || "/shipper/my-orders?tab=delivery");
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
          <Button onClick={navigateToOrders}>Quay lại</Button>
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
                <Button icon={<ArrowLeftOutlined />} onClick={navigateToOrders}>
                  Quay lại
                </Button>
                <Title level={3} style={{ margin: 0 }}>
                  Chi tiết đơn hàng
                </Title>
              </Space>
            </Col>
            <Col>
              <Space>
                {/* ========== PICKUP ACTIONS ========== */}
                {/* Đơn pickup - READY_FOR_PICKUP / URGENT_PICKUP: Nút Nhận */}
                {/* Chỉ hiển thị nút "Nhận" khi đơn CHƯA thuộc shipment (shipmentCode = null) */}
                {isPickupOrder(order) && !order.shipmentCode && (order.status === "READY_FOR_PICKUP" || order.status === "URGENT_PICKUP") && (
                  <Button
                    type="primary"
                    className="primary-button"
                    loading={accepting}
                    onClick={handleAcceptPickup}
                  >
                    Nhận
                  </Button>
                )}
                {/* Đơn pickup - PICKING_UP: Nút Xác nhận đã lấy và Báo lấy hàng thất bại */}
                {isPickupOrder(order) && order.status === "PICKING_UP" && (order.shipmentStatus === "IN_TRANSIT" || !order.shipmentCode) && (
                  <>
                    <Button
                      danger
                      onClick={() => setPickupFailedModalOpen(true)}
                    >
                      Báo lấy hàng thất bại
                    </Button>
                    <Button
                      type="primary"
                      className="primary-button"
                      onClick={handleOpenConfirmPickup}
                    >
                      Xác nhận đã lấy
                    </Button>
                  </>
                )}
                {/* Đơn pickup - PICKUP_RETRY: Nút Tiến hành đến lấy lại */}
                {isPickupOrder(order) && order.status === "PICKUP_RETRY" && (order.shipmentStatus === "IN_TRANSIT" || !order.shipmentCode) && (
                  <Button
                    type="primary"
                    className="primary-button"
                    onClick={handleRetryPickup}
                    loading={loading}
                  >
                    Tiến hành đến lấy lại
                  </Button>
                )}
                {/* Đơn pickup tại nhà - PICKED_UP: Nút Nộp tại bưu cục */}
                {/* Chỉ dành cho PICKUP_BY_COURIER, không phải đơn giao hàng thường */}
                {order.pickupType === "PICKUP_BY_COURIER" && order.status === "PICKED_UP" && (
                  <Button
                    type="primary"
                    style={{ backgroundColor: "#16a34a", borderColor: "#16a34a" }}
                    onClick={handleDeliverToOrigin}
                    loading={loading}
                  >
                    Nộp tại bưu cục
                  </Button>
                )}

                {/* Đơn giao hàng thường - không áp dụng cho đơn hoàn trả */}
                {!isReturnOrder(order) && !isPickupOrder(order) && (getUserRole() === "shipper" || getUserRole() === "clerk") && order.status !== "PICKED_UP" && order.status !== "DELIVERED" && order.status !== "DELIVERING" && (
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
                {/* Đơn giao hàng - Bắt đầu giao */}
                {!isReturnOrder(order) && !isPickupOrder(order) && order.status === "PICKED_UP" && (
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
                {/* Đơn giao hàng - Đang giao */}
                {!isReturnOrder(order) && !isPickupOrder(order) && order.status === "DELIVERING" && (
                  <>
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      disabled={!canMarkDelivered(order)}
                      onClick={() => {
                        setDeliveryProofImageFile(null);
                        setDeliveryProofImagePreview(null);
                        setCollectionMode("FULL");
                        setCustomCollectedAmount(undefined);
                        successForm.resetFields();
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
                {/* Đơn hoàn trả - Đã giao trả hàng hoàn */}
                {isReturnOrder(order) && (order.status === "RETURNING" || order.status === "RETURN_RETRY" || order.status === "RETURN_AT_ORIGIN_OFFICE") && (
                  <>
                    <Button
                      type="primary"
                      icon={<CheckCircleOutlined />}
                      disabled={!canMarkReturnDelivered(order)}
                      title={!canMarkReturnDelivered(order) ? "Đơn chưa thuộc chuyến đang chạy" : ""}
                      onClick={handleOpenReturnDeliveryModal}
                    >
                      Xác nhận hoàn trả
                    </Button>
                    <Button
                      danger
                      icon={<CloseCircleOutlined />}
                      disabled={!canMarkReturnDelivered(order)}
                      title={!canMarkReturnDelivered(order) ? "Đơn chưa thuộc chuyến đang chạy" : ""}
                      onClick={handleOpenReturnFailedModal}
                    >
                      Giao hoàn thất bại
                    </Button>
                  </>
                )}
                {/* Đơn hoàn trả - Xem lộ trình khi đang hoàn trả */}
                {isReturnOrder(order) && (order.status === "RETURNING" || order.status === "RETURN_RETRY") && (
                  <Button icon={<CompassOutlined />} onClick={handleNavigateToRoute}>
                    Xem lộ trình
                  </Button>
                )}
              </Space>
            </Col>
          </Row>

          {/* Alert chỉ hiện cho đơn giao hàng chưa thuộc shipment active */}
          {!isReturnOrder(order) && !isPickupOrder(order) && !isInActiveDeliveryShipment(order) && !["DELIVERED", "CANCELLED"].includes(order.status) && (
            <Row justify="center">
              <Alert
                type="warning"
                showIcon
                description="Bạn không thể thao tác giao hàng khi đơn chưa được gắn vào chuyến."
              />
            </Row>
          )}
          {/* Alert cho đơn pickup thuộc shipment PENDING (chưa bắt đầu chuyến) */}
          {isPickupOrder(order) && order.shipmentCode && order.shipmentStatus === "PENDING" && !["PICKED_UP", "AT_ORIGIN_OFFICE", "CANCELLED"].includes(order.status) && (
            <Row justify="center">
              <Alert
                type="warning"
                showIcon
                description="Vui lòng bắt đầu chuyến trước khi xử lý yêu cầu lấy hàng."
              />
            </Row>
          )}
          {/* Alert cho đơn hoàn trả chưa thuộc shipment IN_TRANSIT */}
          {isReturnOrder(order) && !isInActiveReturnShipment(order) && !["RETURNED", "CANCELLED"].includes(order.status) && (
            <Row justify="center">
              <Alert
                type="warning"
                showIcon
                description="Vui lòng bắt đầu chuyến trước khi xử lý đơn hoàn trả."
              />
            </Row>
          )}
          {/* Alert cho đơn pickup CHƯA được nhận (không thuộc shipment) */}
          {isPickupOrder(order) && !order.shipmentCode && !["PICKED_UP", "AT_ORIGIN_OFFICE", "DELIVERED", "CANCELLED"].includes(order.status) && (order.status === "READY_FOR_PICKUP" || order.status === "URGENT_PICKUP") && (
            <Row justify="center">
              <Alert
                type="info"
                showIcon
                description="Bấm Nhận để nhận yêu cầu lấy hàng này."
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
              {/* Pickup attempts info */}
              {isPickupOrder(order) && (
                <Descriptions.Item label="Lần thử lấy hàng">
                  {(() => {
                    const attempts = (order as any).pickupAttempts || [];
                    const failedAttempts = attempts.filter((a: any) => a.status === "FAILED").length;
                    const maxAttempts = (order as any).maxPickupAttempts || 0;
                    return `${failedAttempts} / ${maxAttempts || "-"}`;
                  })()}
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

          {/* Pickup orders - show sender info instead of recipient */}
          {isPickupOrder(order) && (
            <Card title="Thông tin người gửi">
              <Descriptions column={1} bordered>
                <Descriptions.Item label="Họ tên">
                  <Text strong>{(order as any).senderName || "—"}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Số điện thoại">
                  <Space>
                    <PhoneOutlined />
                    <Text>{(order as any).senderPhone || "—"}</Text>
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label="Địa chỉ">
                  <Space>
                    <EnvironmentOutlined />
                    <Text>{(order as any).senderFullAddress || (order as any).senderAddress || "—"}</Text>
                  </Space>
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          {/* Regular orders - show recipient info; for return orders show shop/sender. */}
          {!isPickupOrder(order) && (
            <Card title={deliveryCardTitle}>
              <Descriptions column={1} bordered>
                <Descriptions.Item label="Loại">
                  <Tag color={deliveryDestinationType === "SENDER_RETURN" ? "purple" : "blue"}>
                    {deliveryContactType}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Họ tên">
                  <Text strong>{deliveryContactName || "—"}</Text>
                </Descriptions.Item>
                <Descriptions.Item label="Số điện thoại">
                  <Space>
                    <PhoneOutlined />
                    <Text>{deliveryContactPhone || "—"}</Text>
                  </Space>
                </Descriptions.Item>
                <Descriptions.Item label="Địa chỉ">
                  <Space>
                    <EnvironmentOutlined />
                    <Text>{deliveryContactAddress || "—"}</Text>
                  </Space>
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}

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
          <Alert
            message="Giao hàng không thành công"
            description="Đơn hàng sẽ được đánh dấu là giao thất bại và chuyển sang quy trình xử lý tiếp theo."
            type="error"
            showIcon
            style={{
              marginBottom: 24,
              backgroundColor: "#fff2f0",
              borderColor: "#ffccc7",
            }}
          />
          <div style={{ marginBottom: 24 }}>
            {renderImagePicker({
              file: deliveryProofImageFile,
              preview: deliveryProofImagePreview,
              onChange: attachDeliveryProofImage,
              onRemove: removeDeliveryProofImage,
              label: "Ảnh minh chứng giao thất bại (tuỳ chọn)",
            })}
          </div>
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
        </Form>
      </Modal>

      {/* Modal: Giao thành công */}
      <Modal
        title="Xác nhận giao thành công"
        open={successModal}
        onOk={handleFinishDelivery}
        onCancel={() => setSuccessModal(false)}
        confirmLoading={uploading}
        width={520}
        okText="Xác nhận giao"
      >
        <div className="delivery-success-modal">
          <Space direction="vertical" size={12} style={{ width: "100%" }}>
            <Alert
              message="Xác nhận giao hàng thành công"
              description="Đơn hàng sẽ được đánh dấu là đã giao."
              type="success"
              showIcon
            />

            {/* Section title: Ảnh minh chứng */}
            <div className="delivery-success-section-title">Ảnh minh chứng giao hàng (tuỳ chọn)</div>

            {/* Ảnh minh chứng - chỉ hiện phần input ảnh */}
            {renderImagePicker({
              file: deliveryProofImageFile,
              preview: deliveryProofImagePreview,
              onChange: attachDeliveryProofImage,
              onRemove: removeDeliveryProofImage,
              label: "Ảnh minh chứng",
              showLabel: false,
            })}

          {/* Phần thu tiền - chỉ hiện khi cần thu tiền */}
          {(() => {
            const cod = Number(order?.cod || 0);
            const shippingFee = Number(order?.shippingFee || 0);
            const isCustomerPayer = (order?.payer || "").toUpperCase() === "CUSTOMER";
            const totalNeedCollect = isCustomerPayer ? cod + shippingFee : cod;

            if (totalNeedCollect <= 0) {
              return null;
            }

            return (
              <>
                {/* Section title: Thu tiền */}
                <div className="delivery-success-section-title">Thu tiền</div>

                {/* Card thông tin thu tiền compact */}
                <Card size="small" className="collection-summary-card" style={{ marginBottom: 16 }}>
                  <Row gutter={16}>
                    {cod > 0 && (
                      <Col span={12}>
                        <div className="summary-item">
                          <Text strong className="summary-label">COD</Text>
                          <Text className="summary-value">{cod.toLocaleString()}đ</Text>
                        </div>
                      </Col>
                    )}
                    {isCustomerPayer && shippingFee > 0 && (
                      <Col span={12}>
                        <div className="summary-item">
                          <Text strong className="summary-label">Phí ship</Text>
                          <Text className="summary-value">{shippingFee.toLocaleString()}đ</Text>
                        </div>
                      </Col>
                    )}
                  </Row>
                  <Divider style={{ margin: '8px 0' }} />
                  <div className="total-row">
                    <Text strong>Tổng cần thu</Text>
                    <Text className="total-amount">{totalNeedCollect.toLocaleString()}đ</Text>
                  </div>
                </Card>

                {/* Section title: Xác nhận thu tiền */}
                <div className="delivery-success-section-title">Xác nhận thu tiền</div>

                {/* Hai lựa chọn thu tiền dạng card nhỏ */}
                <Row gutter={8}>
                  <Col span={12}>
                    <div
                      className={`collection-card ${collectionMode === "FULL" ? "selected" : ""}`}
                      onClick={() => {
                        setCollectionMode("FULL");
                        setCustomCollectedAmount(undefined);
                      }}
                    >
                      <CheckCircleOutlined className="card-icon" />
                      <span className="card-text">Đã thu đủ</span>
                    </div>
                  </Col>
                  <Col span={12}>
                    <div
                      className={`collection-card ${collectionMode === "CUSTOM" ? "selected" : ""}`}
                      onClick={() => setCollectionMode("CUSTOM")}
                    >
                      <DollarOutlined className="card-icon" />
                      <span className="card-text">Thu số tiền khác</span>
                    </div>
                  </Col>
                </Row>

                {/* Input số tiền thực thu - chỉ hiện khi CUSTOM */}
                {collectionMode === "CUSTOM" && (
                  <div style={{ marginTop: 8 }}>
                    <InputNumber
                      value={customCollectedAmount}
                      onChange={(value) => setCustomCollectedAmount(value ?? undefined)}
                      formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")}
                      parser={(value) => Number(value?.replace(/[^\d]/g, "") || 0)}
                      placeholder="Nhập số tiền thực thu"
                      style={{ width: '100%' }}
                      suffix="đồng"
                    />
                    {customCollectedAmount !== undefined && customCollectedAmount !== totalNeedCollect && (
                      <Alert
                        style={{ marginTop: 6 }}
                        type={customCollectedAmount > totalNeedCollect ? "warning" : "error"}
                        message={
                          customCollectedAmount > totalNeedCollect
                            ? `Dư ${(customCollectedAmount - totalNeedCollect).toLocaleString()} đồng`
                            : `Thiếu ${(totalNeedCollect - customCollectedAmount).toLocaleString()} đồng`
                        }
                      />
                    )}
                  </div>
                )}

                {/* Ghi chú - compact */}
                <Input.TextArea
                  rows={2}
                  placeholder="Ghi chú thu tiền..."
                />
              </>
            );
          })()}
        </Space>
        </div>
      </Modal>

      {/* Modal: Giao trả hàng hoàn */}
      <Modal
        title="Xác nhận giao trả hàng hoàn"
        open={returnDeliveryModal}
        onOk={handleReturnDelivery}
        onCancel={() => setReturnDeliveryModal(false)}
        confirmLoading={loading}
        width={640}
        okText="Xác nhận giao trả"
      >
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Alert
            message="Xác nhận giao trả hàng hoàn cho người gửi"
            description="Đơn hàng sẽ được đánh dấu là đã hoàn trả thành công."
            type="success"
            showIcon
          />
          {renderImagePicker({
            file: returnDeliveryProofImageFile,
            preview: returnDeliveryProofImagePreview,
            onChange: attachReturnDeliveryProofImage,
            onRemove: () => {
              setReturnDeliveryProofImageFile(null);
              setReturnDeliveryProofImagePreview(null);
            },
            label: "Ảnh minh chứng giao trả hàng hoàn (tuỳ chọn)",
          })}
        </Space>
      </Modal>

      {/* Modal: Giao hoàn thất bại */}
      <Modal
        title="Giao hoàn thất bại"
        open={returnFailedModal}
        onCancel={() => {
          setReturnFailedModal(false);
          returnFailedForm.resetFields();
          setReturnFailedProofImageFile(null);
          setReturnFailedProofImagePreview(null);
        }}
        footer={null}
        width={680}
      >
        <Form
          form={returnFailedForm}
          layout="vertical"
          onFinish={handleSubmitReturnFailed}
        >
          <Alert
            message="Giao hoàn không thành công"
            description="Đơn hàng sẽ được đánh dấu là giao hoàn thất bại. Hàng sẽ được giữ lại để nộp về bưu cục."
            type="error"
            showIcon
            style={{
              marginBottom: 24,
              backgroundColor: "#fff2f0",
              borderColor: "#ffccc7",
            }}
          />
          <div style={{ marginBottom: 24 }}>
            {renderImagePicker({
              file: returnFailedProofImageFile,
              preview: returnFailedProofImagePreview,
              onChange: attachReturnFailedProofImage,
              onRemove: () => {
                setReturnFailedProofImageFile(null);
                setReturnFailedProofImagePreview(null);
              },
              label: "Ảnh minh chứng giao hoàn thất bại (tuỳ chọn)",
            })}
          </div>
          <Form.Item
            name="failReason"
            label="Lý do"
            rules={[{ required: true, message: "Vui lòng chọn lý do" }]}
          >
            <Select placeholder="Chọn lý do giao hoàn thất bại">
              <Select.Option value="SENDER_NOT_AVAILABLE">Không liên lạc được người gửi</Select.Option>
              <Select.Option value="SENDER_REFUSED">Người gửi từ chối nhận lại</Select.Option>
              <Select.Option value="RETURN_ADDRESS_INVALID">Sai địa chỉ hoàn trả</Select.Option>
              <Select.Option value="RETURN_RESCHEDULE_REQUESTED">Người gửi hẹn giao lại sau</Select.Option>
              <Select.Option value="OTHER">Khác</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item
            name="note"
            label="Ghi chú (tuỳ chọn)"
          >
            <Input.TextArea
              rows={3}
              placeholder="Nhập ghi chú thêm..."
            />
          </Form.Item>
          <Form.Item style={{ marginBottom: 0, textAlign: "right" }}>
            <Space>
              <Button onClick={() => {
                setReturnFailedModal(false);
                returnFailedForm.resetFields();
                setReturnFailedProofImageFile(null);
                setReturnFailedProofImagePreview(null);
              }}>
                Hủy
              </Button>
              <Button type="primary" htmlType="submit" loading={loading}>
                Xác nhận giao hoàn thất bại
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* ========== PICKUP MODALS ========== */}

      {/* Modal: Báo lấy hàng thất bại */}
      <PickupAttemptModal
        open={pickupFailedModalOpen}
        loading={loading}
        onCancel={() => setPickupFailedModalOpen(false)}
        onSubmit={handleSubmitPickupFailed}
      />

      {/* Modal: Xác nhận đã lấy hàng cho pickup */}
      <Modal
        title="Xác nhận đã lấy hàng"
        open={confirmPickupModalVisible}
        onOk={handleConfirmPickupWithImage}
        onCancel={() => setConfirmPickupModalVisible(false)}
        confirmLoading={uploading}
        width={640}
        okText="Xác nhận"
      >
        <Space direction="vertical" size="large" style={{ width: "100%" }}>
          <Alert
            message="Xác nhận đã lấy hàng"
            description={`Bạn đang xác nhận đã lấy hàng cho đơn: ${order?.trackingNumber || order?.id}`}
            type="info"
            showIcon
          />
          <div style={{ marginBottom: 12 }}>
            <Text strong>Ảnh minh chứng lấy hàng (tuỳ chọn)</Text>
            <div style={{ marginTop: 8 }}>
              <input
                id="pickup-confirm-image-input"
                type="file"
                accept="image/*"
                style={{ display: "none" }}
                onChange={handleSelectPickupImage}
              />
              <Space>
                <Button
                  icon={<PictureOutlined />}
                  onClick={() => document.getElementById("pickup-confirm-image-input")?.click()}
                >
                  Chọn ảnh
                </Button>
                {confirmPickupImagePreview && (
                  <Button danger onClick={handleRemovePickupImage}>
                    Xóa ảnh
                  </Button>
                )}
              </Space>
            </div>
            {confirmPickupImagePreview && (
              <div style={{ marginTop: 12 }}>
                <img
                  src={confirmPickupImagePreview}
                  alt="Ảnh minh chứng lấy hàng"
                  style={{ maxWidth: "100%", maxHeight: 220, borderRadius: 8, border: "1px solid #e5e7eb" }}
                />
              </div>
            )}
          </div>
        </Space>
      </Modal>
    </div>
  );
};

export default ShipperOrderDetail;
