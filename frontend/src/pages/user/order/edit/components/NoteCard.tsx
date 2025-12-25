import React from "react";
import { Card, Input } from "antd";
import { type OrderStatus } from "../../../../../utils/orderUtils";
import { canEditUserOrderField } from "../../../../../utils/userOrderEditRules";

interface Props {
  notes: string;
  onChange?: (value: string) => void;
  status: OrderStatus;
}

const NoteCard: React.FC<Props> = ({ notes, onChange, status }) => {
  return (
    <div className="create-order-card-container">
      <Card className="create-order-custom-card">
        <div className="create-order-custom-card-title">Ghi chú</div>
        <Input.TextArea
          className="modal-custom-input-textarea"
          placeholder="Nhập ghi chú cho đơn hàng..."
          autoSize={{ minRows: 3, maxRows: 6 }}
          disabled={!canEditUserOrderField('notes', status)}
          value={notes}
          onChange={(e) => onChange?.(e.target.value)}
        />
      </Card>
    </div>
  );
};

export default NoteCard;