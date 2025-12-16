import React, { useEffect } from "react";
import {
  Modal,
  Form,
  Select,
  Input,
  Descriptions,
  Typography,
} from "antd";
import dayjs from "dayjs";
import { useNavigate } from "react-router-dom";
import type { ManagerPaymentSubmissionBatch } from "../../../../types/paymentSubmissionBatch";
import { getAllowedManagerStatuses, translatePaymentSubmissionBatchStatus } from "../../../../utils/paymentSubmissionBatchUtils";

const { Text } = Typography;
const { TextArea } = Input;

interface Props {
  visible: boolean;
  batch: ManagerPaymentSubmissionBatch | null;
  onClose: () => void;
  onSubmit: (status: string, notes: string) => Promise<void>;
  loading: boolean;
}

const ProcessPaymentSubmissionModal: React.FC<Props> = ({
  visible,
  batch,
  onClose,
  onSubmit,
  loading
}) => {
  const [form] = Form.useForm();
  const navigate = useNavigate();
  const mismatch = batch?.totalSystemAmount !== batch?.totalActualAmount;

  useEffect(() => {
    if (!visible) return;
    form.resetFields();
  }, [visible, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    await onSubmit(values.status, values.notes);
    onClose();
  };

  if (!batch) return null;

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
          Đối soát #{batch.code}
        </span>
      }
    >
      <Form form={form} layout="vertical">

        {/* THÔNG TIN ĐỐI SOÁT */}
        <Descriptions bordered column={1} size="middle">

          <Descriptions.Item label="Trạng thái hiện tại">
            {translatePaymentSubmissionBatchStatus(batch.status)}
          </Descriptions.Item>

          <Descriptions.Item label="Ngày nộp">
            {batch.createdAt
              ? dayjs(batch.createdAt).format("DD/MM/YYYY HH:mm:ss")
              : "N/A"}
          </Descriptions.Item>

          <Descriptions.Item label="Người nộp">
            {batch.shipper ? (
              <div>
                {batch.shipper.lastName} {batch.shipper.firstName}<br />
                <span className="text-muted">{batch.shipper.phoneNumber}</span>
              </div>
            ) : (
              <Text className="text-muted">Chưa xác nhận</Text>
            )}
          </Descriptions.Item>

          <Descriptions.Item label="Tổng tiền hệ thống">
            <Text
              className={`${mismatch ? 'custom-table-content-error' : 'custom-table-content-strong'}`}
            >
              {batch.totalSystemAmount.toLocaleString()} VNĐ
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Tổng tiền thực thu">
            <Text
              className={`${mismatch ? 'custom-table-content-error' : 'custom-table-content-strong'}`}
            >
              {batch.totalActualAmount.toLocaleString()} VNĐ
            </Text>
          </Descriptions.Item>

          <Descriptions.Item label="Người xác nhận">
            {batch.checkedBy ? (
              <>
                <div>
                  {batch.checkedBy.lastName} {batch.checkedBy.firstName}<br />
                  <span className="text-muted">{batch.checkedBy.phoneNumber}</span>
                </div>
              </>
            ) : (
              <Text className="text-muted">Chưa xác nhận</Text>
            )}
          </Descriptions.Item>

          {batch.checkedBy ? (
            <Descriptions.Item label="Ngày xác nhận">
              {batch.checkedAt
                ? dayjs(batch.checkedAt).format("DD/MM/YYYY HH:mm:ss")
                : "N/A"}
            </Descriptions.Item>
          ) : null}

        </Descriptions>

        <div className="manager-batchs-divide" />

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
            {getAllowedManagerStatuses(batch.status).map(s => (
              <Select.Option key={s} value={s}>
                {translatePaymentSubmissionBatchStatus(s)}
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