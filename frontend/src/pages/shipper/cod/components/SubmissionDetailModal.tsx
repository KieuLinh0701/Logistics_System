import React from "react";
import {Descriptions, Modal, Tag, Typography} from "antd";
import dayjs from "dayjs";

const { Text } = Typography;

interface PaymentSubmissionItem {
  id: number;
  code?: string;
  orderId?: number;
  trackingNumber?: string;
  systemAmount: number;
  actualAmount: number;
  discrepancy?: number;
  status: string;
  notes?: string;
  paidAt?: string;
  checkedAt?: string;
}

interface SubmissionDetailModalProps {
  open: boolean;
  submission: PaymentSubmissionItem | null;
  onClose: () => void;
}

const getSubmissionStatusColor = (status: string) => {
  switch (status.toUpperCase()) {
    case "PENDING":
    case "IN_BATCH":
      return "orange";
    case "MATCHED":
      return "success";
    case "ADJUSTED":
      return "blue";
    case "MISMATCHED":
      return "red";
    default:
      return "default";
  }
};

const getSubmissionStatusText = (status: string) => {
  switch (status.toUpperCase()) {
    case "PENDING":
      return "Chờ xác nhận";
    case "IN_BATCH":
      return "Đang nộp";
    case "MATCHED":
      return "Khớp";
    case "ADJUSTED":
      return "Đã điều chỉnh";
    case "MISMATCHED":
      return "Không khớp";
    default:
      return status;
  }
};

const SubmissionDetailModal: React.FC<SubmissionDetailModalProps> = ({
  open,
  submission,
  onClose,
}) => {
  if (!submission) return null;

  const disc = submission.discrepancy ?? 0;

  return (
    <Modal
      title="Chi tiết nộp tiền thu được"
      open={open}
      onCancel={onClose}
      footer={null}
      width={600}
    >
      <Descriptions column={1} bordered>
        <Descriptions.Item label="Mã đơn hàng">{submission.trackingNumber}</Descriptions.Item>
        <Descriptions.Item label="Số tiền hệ thống">{submission.systemAmount.toLocaleString()}đ</Descriptions.Item>
        <Descriptions.Item label="Số tiền thực nộp">{submission.actualAmount.toLocaleString()}đ</Descriptions.Item>
        <Descriptions.Item label="Chênh lệch">
          <Text style={{ color: disc !== 0 ? "#f50" : "#52c41a" }}>
            {disc > 0 ? "+" : ""}
            {disc.toLocaleString()}đ
          </Text>
        </Descriptions.Item>
        <Descriptions.Item label="Trạng thái">
          <Tag color={getSubmissionStatusColor(submission.status)}>
            {getSubmissionStatusText(submission.status)}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="Ngày nộp">
          {submission.paidAt ? dayjs(submission.paidAt).format("DD/MM/YYYY HH:mm") : "—"}
        </Descriptions.Item>
        {submission.checkedAt && (
          <Descriptions.Item label="Ngày xác nhận">
            {dayjs(submission.checkedAt).format("DD/MM/YYYY HH:mm")}
          </Descriptions.Item>
        )}
        {submission.notes && (
          <Descriptions.Item label="Ghi chú">{submission.notes}</Descriptions.Item>
        )}
      </Descriptions>
    </Modal>
  );
};

export default SubmissionDetailModal;
