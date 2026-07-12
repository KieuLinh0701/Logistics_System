import React from "react";
import {Button, Modal, Space, Tag, Typography} from "antd";
import {
  AimOutlined,
  CarOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  EnvironmentOutlined,
  StarFilled,
} from "@ant-design/icons";
import type {ShipperOrder} from "../../../../api/orderApi";
import type {RecommendationLevel} from "./UnassignedOrdersTable";

const {Text} = Typography;

interface UnassignedRecommendationModalProps {
  open: boolean;
  order: ShipperOrder | null;
  onClose: () => void;
}

const levelLabel = (level?: RecommendationLevel | string): string => {
  switch (level) {
    case "HIGH":
      return "Rất phù hợp";
    case "MEDIUM":
      return "Phù hợp";
    case "LOW":
      return "Có thể nhận";
    case "NOT_RECOMMENDED":
      return "Ít phù hợp";
    case "OVER_CAPACITY":
      return "Vượt tải";
    default:
      return "—";
  }
};

const levelColor = (level?: RecommendationLevel | string): string => {
  switch (level) {
    case "HIGH":
      return "success";
    case "MEDIUM":
      return "processing";
    case "LOW":
      return "warning";
    case "OVER_CAPACITY":
      return "error";
    case "NOT_RECOMMENDED":
    default:
      return "default";
  }
};

// Icon cho từng nhóm tiêu chí, dựa trên nội dung lý do trả về từ AI Service.
const reasonIcon = (reason: string): React.ReactNode => {
  const r = reason.toLowerCase();
  if (r.includes("vượt tải")) return <CarOutlined />;
  if (r.includes("tải")) return <DatabaseOutlined />;
  if (r.includes("khoảng cách") || r.includes("gần") || r.includes("cách")) {
    return <AimOutlined />;
  }
  if (r.includes("thời gian")) return <ClockCircleOutlined />;
  if (r.includes("khu vực") || r.includes("bưu cục")) return <EnvironmentOutlined />;
  if (r.includes("ưu tiên")) return <StarFilled />;
  return <StarFilled />;
};

const UnassignedRecommendationModal: React.FC<UnassignedRecommendationModalProps> = ({
  open,
  order,
  onClose,
}) => {
  if (!order) {
    return (
      <Modal open={open} onCancel={onClose} footer={null} title="Mức độ phù hợp" destroyOnClose>
        <Text type="secondary">Đang tải...</Text>
      </Modal>
    );
  }
  const score = (order as any).recommendationScore as number | undefined;
  const level = (order as any).recommendationLevel as RecommendationLevel | undefined;
  const reasons: string[] | undefined = (order as any).recommendationReasons;
  const distance = (order as any).estimatedDistanceKm as number | undefined;
  const duration = (order as any).estimatedDurationMinutes as number | undefined;

  // Label cho block tóm tắt khoảng cách/thời gian khi AI không trả về reason
  // tương ứng (ví dụ destination thiếu tọa độ). AI Service đã tự generate
  // reason có chứa label theo destinationType, nên 2 dòng này chỉ là fallback.
  const destinationType =
    (order as any).destinationType === "PICKUP_SENDER"
      ? "PICKUP_SENDER"
      : (order as any).destinationType === "SENDER_RETURN"
        ? "SENDER_RETURN"
        : "RECIPIENT";
  const distanceLabel =
    destinationType === "PICKUP_SENDER"
      ? "Khoảng cách đến điểm lấy hàng"
      : destinationType === "SENDER_RETURN"
        ? "Khoảng cách đến shop/người gửi"
        : "Khoảng cách đến người nhận";
  const durationLabel =
    destinationType === "PICKUP_SENDER"
      ? "Thời gian dự kiến đến người gửi"
      : destinationType === "SENDER_RETURN"
        ? "Thời gian dự kiến đến shop/người gửi"
        : "Thời gian dự kiến đến người nhận";

  return (
    <Modal
      open={open}
      onCancel={onClose}
      destroyOnClose
      title={
        <Space>
          <StarFilled style={{color: "#faad14"}} />
          <span>
            {level ? `Mức độ phù hợp: ${score != null ? `${score}%` : "—"}` : "Mức độ phù hợp"}
          </span>
        </Space>
      }
      footer={[
        <Button key="close" type="primary" onClick={onClose}>
          Đóng
        </Button>,
      ]}
      width={520}
    >
      <Space direction="vertical" size={12} style={{width: "100%"}}>
        <div>
          <Text strong>Đơn hàng: </Text>
          <Text copyable>{order.trackingNumber}</Text>
        </div>

        {level ? (
          <Tag color={levelColor(level)} style={{fontWeight: 600, fontSize: 13}}>
            Mức đánh giá: {levelLabel(level)}
          </Tag>
        ) : (
          <Text type="secondary">Chưa có đánh giá cho đơn này.</Text>
        )}

        {distance != null ? (
          <div>
            <Text strong>{distanceLabel}: </Text>
            <Text>{distance.toFixed(2)} km</Text>
          </div>
        ) : null}

        {duration != null ? (
          <div>
            <Text strong>{durationLabel}: </Text>
            <Text>{duration} phút</Text>
          </div>
        ) : null}

        {Array.isArray(reasons) && reasons.length > 0 ? (
          <div>
            <Text strong>Các tiêu chí đánh giá:</Text>
            <ul style={{marginTop: 8, marginBottom: 0, paddingLeft: 20}}>
              {reasons.map((r, idx) => (
                <li key={`${order.id}-reason-${idx}`} style={{marginBottom: 4}}>
                  <Space size={6}>
                    <span style={{color: "#1c3d90"}}>{reasonIcon(r)}</span>
                    <Text>{r}</Text>
                  </Space>
                </li>
              ))}
            </ul>
          </div>
        ) : (
          <Text type="secondary">Hệ thống chưa sinh tiêu chí cụ thể cho đơn này.</Text>
        )}
      </Space>
    </Modal>
  );
};

export default UnassignedRecommendationModal;