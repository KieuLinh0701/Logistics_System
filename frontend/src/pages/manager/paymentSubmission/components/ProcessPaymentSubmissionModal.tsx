import React, { useEffect } from "react";
import {
  Modal,
  Form,
  Select,
  Input,
  Descriptions,
  Typography,
  Tooltip
} from "antd";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import type { ManagerPaymentSubmission } from "../../../../types/paymentSubmission";
import { getAllowedManagerStatuses, translatePaymentSubmissionStatus } from "../../../../utils/paymentSubmissionUtils";

const { Text } = Typography;
const { TextArea } = Input;

interface Props {
  visible: boolean;
  submission: ManagerPaymentSubmission | null;
  onClose: () => void;
  onSubmit: (status: string, notes: string) => Promise<void>;
  loading: boolean;
}

const ProcessPaymentSubmissionModal: React.FC<Props> = ({
  visible,
  submission,
  onClose,
  onSubmit,
  loading
}) => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const mismatch = submission?.systemAmount !== submission?.actualAmount;

  useEffect(() => {
    if (!visible) return;
    form.resetFields();
  }, [visible, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit(values.status, values.notes);
    onClose();
  };

  if (!submission) return null;

  return (
    <Modal
      centered
      open={visible}
      onCancel={onClose}
      onOk={handleOk}
      confirmLoading={loading}
      width={650}
      className="modal-hide-scrollbar"
      okText="Xác nhận"
      cancelText="Hủy"
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      title={
        <span className="modal-title">
          Đối soát #{submission.code}
        </span>
      }
    >
      <Form form={form} layout="vertical">

        {/* THÔNG TIN ĐỐI SOÁT */}
        <Descriptions bordered column={1} size="middle">

          <Descriptions.Item label="Trạng thái hiện tại">
            {translatePaymentSubmissionStatus(submission.status)}
          </Descriptions.Item>

          <Descriptions.Item label="Ngày tạo">
            {submission.paidAt
              ? dayjs(submission.paidAt).format("DD/MM/YYYY HH:mm:ss")
              : "N/A"}
          </Descriptions.Item>

          <Descriptions.Item label="Tiền hệ thống">
            <Text
              className={`${mismatch ? 'custom-table-content-error' : 'custom-table-content-strong'}`}
            >
              {submission.systemAmount.toLocaleString()} VNĐ
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Tiền thực thu">
            <Text
              className={`${mismatch ? 'custom-table-content-error' : 'custom-table-content-strong'}`}
            >
              {submission.actualAmount.toLocaleString()} VNĐ
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Đơn hàng">
            <Tooltip title="Click để xem chi tiết đơn hàng">
              <span
                className="navigate-link-default"
                onClick={() => navigate(`/orders/tracking/${submission.order.trackingNumber}`)}
              >
                {submission.order.trackingNumber}
              </span>
            </Tooltip>
          </Descriptions.Item>

          <Descriptions.Item label="Người xác nhận">
            {submission.checkedBy ? (
              <>
                <div>
                  {submission.checkedBy.lastName} {submission.checkedBy.firstName}<br/>
                  <span className="text-muted">{submission.checkedBy.phoneNumber}</span>
                  
                </div>
              </>
            ) : (
              <Text className="text-muted">Chưa xác nhận</Text>
            )}
          </Descriptions.Item>

          {submission.checkedBy ? (
            <Descriptions.Item label="Ngày xác nhận">
              {submission.checkedAt
                ? dayjs(submission.checkedAt).format("DD/MM/YYYY HH:mm:ss")
                : "N/A"}
            </Descriptions.Item>
          ) : null}

        </Descriptions>

        <div className="manager-settlements-divide" />

        {/* XỬ LÝ ĐỐI SOÁT */}
        <Form.Item
          label={<span className="modal-lable">Trạng thái mới</span>}
          name="status"
          rules={[{ required: true, message: "Vui lòng chọn trạng thái" }]}
        >
          <Select
            className="modal-custom-select"
            placeholder="Chọn trạng thái mới..."
          >
            {getAllowedManagerStatuses(submission.status).map(s => (
              <Select.Option key={s} value={s}>
                {translatePaymentSubmissionStatus(s)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Ghi chú</span>}
          name="notes"
        >
          <TextArea
            rows={4}
            className="modal-custom-input-textarea"
            placeholder="Nhập ghi chú xử lý..."
          />
        </Form.Item>

      </Form>
    </Modal>
  );
};

export default ProcessPaymentSubmissionModal;