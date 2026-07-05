import React from "react";
import { Form, Input, InputNumber, Modal } from "antd";

interface SubmitCODModalProps {
  open: boolean;
  loading: boolean;
  form: any;
  totalAmount: number;
  onOk: () => void;
  onCancel: () => void;
}

const SubmitCODModal: React.FC<SubmitCODModalProps> = ({
  open,
  loading,
  form,
  totalAmount,
  onOk,
  onCancel,
}) => {
  return (
    <Modal
      title="Nộp tiền thu được"
      open={open}
      onOk={onOk}
      onCancel={onCancel}
      width={600}
    >
      <Form form={form} layout="vertical" onFinish={onOk}>
        <Form.Item label="Tổng số tiền hệ thống">
          <InputNumber
            value={totalAmount}
            formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",") + "đ"}
            disabled
            style={{ width: "100%" }}
          />
        </Form.Item>
        <Form.Item
          name="totalAmount"
          label="Số tiền thực nộp"
          rules={[{ required: true, message: "Vui lòng nhập số tiền thực nộp" }]}
        >
          <InputNumber<number>
            formatter={(value) => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",") + "đ"}
            parser={(value) => Number(value?.replace(/[^\d]/g, "") || 0)}
            style={{ width: "100%" }}
            min={0}
          />
        </Form.Item>
        <Form.Item name="notes" label="Ghi chú">
          <Input.TextArea rows={3} placeholder="Ghi chú về việc nộp tiền (nếu có)" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default SubmitCODModal;
