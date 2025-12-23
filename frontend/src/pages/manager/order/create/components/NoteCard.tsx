import React from "react";
import { Card, Form, Input } from "antd";

interface Props {
  notes?: string;
  onChange?: (value: string) => void;
}

const NoteCard: React.FC<Props> = ({ notes, onChange }) => {
  const [form] = Form.useForm();

  return (
    <div className="create-order-card-container">
    <Card className="create-order-custom-card">
      <div className="create-order-custom-card-title">Ghi chú</div>
      <Form
        form={form}
        layout="vertical"
        initialValues={{ notes: notes }}
        onValuesChange={(changedValues) => {
          if (changedValues.notes !== undefined) {
            onChange?.(changedValues.notes);
          }
        }}
      >
        <Form.Item name="notes">
          <Input.TextArea
           className="modal-custom-input-textarea"
            placeholder="Nhập ghi chú cho đơn hàng..."
            autoSize={{ minRows: 3, maxRows: 6 }}
          />
        </Form.Item>
      </Form>
    </Card>
    </div>
  );
};

export default NoteCard;