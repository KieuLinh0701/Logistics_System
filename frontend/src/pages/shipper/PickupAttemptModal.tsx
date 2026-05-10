import React from "react";
import { Modal, Form, Select, Input } from "antd";
import { PICKUP_FAIL_REASONS, translatePickupFailReason } from "../../utils/orderUtils";

const { TextArea } = Input;

interface PickupAttemptModalProps {
  open: boolean;
  loading?: boolean;
  onCancel: () => void;
  onSubmit: (values: { failReason: string; note?: string }) => void;
}

const PickupAttemptModal: React.FC<PickupAttemptModalProps> = ({
  open,
  loading,
  onCancel,
  onSubmit,
}) => {
  const [form] = Form.useForm();

  return (
    <Modal
      className="modal-hide-scrollbar shipper-modal"
      width={520}
      bodyStyle={{ minHeight: 180 }}
      title={<div className="modal-title">Báo lấy hàng thất bại</div>}
      open={open}
      onCancel={() => {
        form.resetFields();
        onCancel();
      }}
      okText="Gửi báo cáo"
      cancelText="Hủy"
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      confirmLoading={loading}
      onOk={() => {
        form
          .validateFields()
          .then((values) => {
            onSubmit(values);
            form.resetFields();
          })
          .catch(() => undefined);
      }}
    >
      <Form className="shipper-modal-form" form={form} layout="vertical">
        <div className="modal-section">
          <div className="modal-grid">
            <Form.Item
              name="failReason"
              label={<span className="modal-lable">Lý do thất bại</span>}
              rules={[{ required: true, message: "Vui lòng chọn lý do thất bại" }]}
              className="form-item"
              style={{ gridColumn: "1 / 3" }}
            >
              <Select
                className="modal-custom-select"
                placeholder="Chọn lý do"
                options={PICKUP_FAIL_REASONS.map((value) => ({
                  value,
                  label: translatePickupFailReason(value),
                }))}
              />
            </Form.Item>
            <Form.Item name="note" label={<span className="modal-lable">Ghi chú</span>} className="form-item" style={{ gridColumn: "1 / 3" }}>
              <TextArea rows={3} placeholder="Nhập ghi chú (nếu có)" />
            </Form.Item>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default PickupAttemptModal;
