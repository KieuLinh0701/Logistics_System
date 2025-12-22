import React from "react";
import { Card, Input } from "antd";
import { type OrderCreatorType, type OrderStatus } from "../../../../../utils/orderUtils";
import { canManagerEditOrderField } from "../../../../../utils/managerOrderEditRules";

interface Props {
  notes: string;
  onChange?: (value: string) => void;
  status: OrderStatus;
  creator: OrderCreatorType;
}

const NoteCard: React.FC<Props> = ({ notes, onChange, status, creator }) => {
  return (
    <div className="create-order-card-container">
      <Card className="create-order-custom-card">
        <div className="create-order-custom-card-title">Ghi chú</div>
        <Input.TextArea
          className="modal-custom-input-textarea"
          placeholder="Nhập ghi chú cho đơn hàng..."
          autoSize={{ minRows: 3, maxRows: 6 }}
          disabled={!canManagerEditOrderField('notes', status, creator)}
          value={notes}
          onChange={(e) => onChange?.(e.target.value)}
        />
      </Card>
    </div>
  );
};

export default NoteCard;