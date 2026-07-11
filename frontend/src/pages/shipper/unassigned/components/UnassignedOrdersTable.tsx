import React, {useState} from "react";
import {Button, Space, Table, Tag, Tooltip, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {EyeOutlined, StarFilled, WarningOutlined} from "@ant-design/icons";
import {useNavigate} from "react-router-dom";
import type {ShipperOrder} from "../../../../api/orderApi";
import UnassignedRecommendationModal from "./UnassignedRecommendationModal";

const {Text} = Typography;

export type RecommendationLevel =
  | "HIGH"
  | "MEDIUM"
  | "LOW"
  | "NOT_RECOMMENDED"
  | "OVER_CAPACITY";

interface UnassignedOrdersTableProps {
  orders: ShipperOrder[];
  loading: boolean;
  pagination: {
    current: number;
    pageSize: number;
    total: number;
  };
  onPageChange: (page: number, limit: number) => void;
  onClaim: (orderId: number) => void;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case "PENDING":
      return "default";
    case "CONFIRMED":
      return "blue";
    case "AT_DEST_OFFICE":
    case "RETURN_AT_ORIGIN_OFFICE":
      return "orange";
    case "RETURN_READY_FOR_PICKUP":
      return "gold";
    case "RETURN_PICKED_UP":
      return "cyan";
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
      return "Tại bưu cục";
    case "RETURN_AT_ORIGIN_OFFICE":
      return "Đã hoàn về bưu cục gốc";
    case "RETURN_READY_FOR_PICKUP":
      return "Sẵn sàng lấy hàng hoàn";
    case "RETURN_PICKED_UP":
      return "Đã lấy hàng hoàn lên xe";
    case "READY_FOR_PICKUP":
      return "Sẵn sàng lấy hàng";
    case "PICKED_UP":
      return "Đã lấy hàng";
    case "DELIVERING":
      return "Đang giao hàng";
    case "DELIVERED":
      return "Đã giao";
    case "CANCELLED":
      return "Đã hủy";
    default:
      return status;
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

// Hiển thị nhãn gọn cho badge cột "Mức phù hợp": <level> <score>%
const recommendationBadgeLabel = (
  score: number,
  level?: RecommendationLevel | string,
): string => {
  if (level === "OVER_CAPACITY") return "Vượt tải";
  return `${levelLabel(level)} ${score}%`;
};

const UnassignedOrdersTable: React.FC<UnassignedOrdersTableProps> = ({
  orders,
  loading,
  pagination,
  onPageChange,
  onClaim,
}) => {
  const navigate = useNavigate();
  const [modalOrderId, setModalOrderId] = useState<number | null>(null);
  const [modalOrder, setModalOrder] = useState<ShipperOrder | null>(null);

  const openRecommendation = (order: ShipperOrder) => {
    if (order.id == null) return;
    setModalOrderId(order.id);
    setModalOrder(order);
  };

  const closeRecommendation = () => {
    setModalOrderId(null);
    setModalOrder(null);
  };

  const columns: ColumnsType<ShipperOrder> = [
    {
      title: "Mã đơn hàng",
      dataIndex: "trackingNumber",
      key: "trackingNumber",
      width: 150,
      render: (text: string) => (
        <Typography.Text
          strong
          style={{whiteSpace: "nowrap"}}
          className="shipper-table-strong shipper-unassigned-tracking"
        >
          {text}
        </Typography.Text>
      ),
    },
    {
      title: "Thông tin điểm giao",
      key: "recipient",
      width: 460,
      render: (_, record) => {
        const destinationType =
          (record as any).destinationType === "SENDER_RETURN"
            ? "SENDER_RETURN"
            : "RECIPIENT";

        // Ưu tiên các trường displayContact do backend sinh ra theo destinationType.
        const name =
          (record as any).displayContactName ||
          (destinationType === "SENDER_RETURN"
            ? record.senderName
            : record.recipientName);
        const phone =
          (record as any).displayContactPhone ||
          (destinationType === "SENDER_RETURN"
            ? record.senderPhone
            : record.recipientPhone);
        const address =
          (record as any).displayContactAddress ||
          (destinationType === "SENDER_RETURN"
            ? (record as any).senderFullAddress || record.senderAddress
            : record.recipientFullAddress ||
              (typeof record.recipientAddress === "string"
                ? record.recipientAddress
                : (record.recipientAddress as any)?.fullAddress) ||
              "");
        const contactType =
          (record as any).displayContactType ||
          (destinationType === "SENDER_RETURN"
            ? "Shop/Người gửi"
            : "Người nhận");
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text strong className="shipper-table-strong">
              {name || "—"}
            </Typography.Text>
            <Typography.Text type="secondary" style={{fontSize: 11}}>
              {contactType}
            </Typography.Text>
            <Typography.Text className="shipper-table-muted">
              {phone}
            </Typography.Text>
            <Typography.Text className="shipper-table-muted">
              {address}
            </Typography.Text>
          </Space>
        );
      },
    },
    {
      title: "Dịch vụ & COD",
      key: "serviceCod",
      width: 220,
      render: (_, record) => {
        const serviceName =
          typeof record.serviceType === "string"
            ? record.serviceType
            : (record.serviceType as any)?.name ?? "";
        return (
          <Space direction="vertical" size={2}>
            <Typography.Text className="shipper-table-strong">
              {serviceName || "—"}
            </Typography.Text>
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
      width: 160,
      render: (s: string) => (
        <Tag
          color={getStatusColor(s)}
          style={{fontWeight: 600, textTransform: "uppercase"}}
        >
          {getStatusText(s)}
        </Tag>
      ),
    },
    {
      title: "Mức phù hợp",
      key: "recommendation",
      width: 220,
      render: (_, record: any) => {
        const score = record.recommendationScore;
        const level = record.recommendationLevel as RecommendationLevel | undefined;
        const reasons: string[] | undefined = record.recommendationReasons;

        if (
          score == null ||
          level == null ||
          !Array.isArray(reasons) ||
          reasons.length === 0
        ) {
          return (
            <Typography.Text type="secondary" className="shipper-table-muted">
              Chưa có đánh giá
            </Typography.Text>
          );
        }

        const isOverCapacity = level === "OVER_CAPACITY";
        const tooltipTitle = isOverCapacity
          ? "Đơn vượt tải trọng còn lại của phương tiện — bấm để xem chi tiết"
          : `Mức phù hợp ${score}% (${levelLabel(level)}) — bấm để xem chi tiết`;

        return (
          <Tooltip title={tooltipTitle}>
            <Tag
              color={levelColor(level)}
              icon={isOverCapacity ? <WarningOutlined /> : <StarFilled />}
              onClick={(e) => {
                e.stopPropagation();
                openRecommendation(record);
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  openRecommendation(record);
                }
              }}
              role="button"
              tabIndex={0}
              aria-label={`Xem chi tiết mức phù hợp ${score}%`}
              style={{
                cursor: "pointer",
                fontWeight: 600,
                margin: 0,
                userSelect: "none",
              }}
              className="shipper-recommendation-badge"
            >
              {recommendationBadgeLabel(score, level)}
            </Tag>
          </Tooltip>
        );
      },
    },
    {
      title: "Thao tác",
      key: "action",
      width: 240,
      render: (_, record) => (
        <Space>
          <Button
            icon={<EyeOutlined />}
            onClick={() => navigate(`/shipper/orders/${record.id}`)}
          >
            Chi tiết
          </Button>
          <Button
            type="primary"
            className="primary-button"
            onClick={() => onClaim(record.id!)}
          >
            Nhận đơn
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Table
        rowKey="id"
        loading={loading}
        columns={columns}
        dataSource={orders}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          onChange: onPageChange,
        }}
        scroll={{x: "max-content"}}
        className="list-page-table"
      />
      <UnassignedRecommendationModal
        open={modalOrderId !== null}
        order={modalOrder}
        onClose={closeRecommendation}
      />
    </>
  );
};

export default UnassignedOrdersTable;