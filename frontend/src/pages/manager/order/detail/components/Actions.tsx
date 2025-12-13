import React from "react";
import { CloseCircleOutlined, EditOutlined, PlayCircleOutlined, PrinterOutlined, CustomerServiceOutlined } from "@ant-design/icons";

interface Props {
  canEdit: boolean;
  canCancel: boolean;
  canPrint: boolean;
  onEdit: () => void;
  onCancel: () => void;
  onPrint: () => void;
}

const Actions: React.FC<Props> = ({
  canEdit,
  canCancel,
  canPrint,
  onEdit,
  onCancel,
  onPrint,
}) => (
  <div className="order-detail-actions-container">
    {/* Nhóm nút bên trái */}
    <div className="order-detail-left-buttons">
      <button
        className={`order-detail-print-btn ${!canPrint ? "disabled" : ""}`}
        onClick={canPrint ? onPrint : undefined}
        disabled={!canPrint}
      >
        <PrinterOutlined /> In phiếu hàng
      </button>
    </div>

    {/* Nhóm nút bên phải */}
    <div className="order-detail-right-buttons">
      <button
        className={`order-detail-edit-btn ${!canEdit ? "disabled" : ""}`}
        onClick={canEdit ? onEdit : undefined}
        disabled={!canEdit}
      >
        <EditOutlined /> Chỉnh sửa
      </button>

      <button
        className={`order-detail-cancel-btn ${!canCancel ? "disabled" : ""}`}
        onClick={canCancel ? onCancel : undefined}
        disabled={!canCancel}
      >
        <CloseCircleOutlined /> Hủy
      </button>
    </div>
  </div>
);

export default Actions;