import React from "react";
import { CloseCircleOutlined, EditOutlined, PrinterOutlined, CheckCircleOutlined } from "@ant-design/icons";

interface Props {
  canEdit: boolean;
  canCancel: boolean;
  canPrint: boolean;
  canConfirm: boolean;
  canSetAtOriginOffice: boolean;
  onEdit: () => void;
  onCancel: () => void;
  onPrint: () => void;
  onConfirm: () => void;
  onSetAtOriginOffice: () => void;
}

const Actions: React.FC<Props> = ({
  canEdit,
  canCancel,
  canPrint,
  canConfirm,
  canSetAtOriginOffice,
  onEdit,
  onCancel,
  onPrint,
  onConfirm,
  onSetAtOriginOffice
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

      {canSetAtOriginOffice && (
        <button className="order-detail-public-btn" onClick={onSetAtOriginOffice}>
          <CheckCircleOutlined /> Đã đến bưu cục
        </button>
      )}

      {canConfirm && (
        <button className="order-detail-public-btn" onClick={onConfirm}>
          <CheckCircleOutlined /> Xác nhận
        </button>
      )}
    </div>
  </div>
);

export default Actions;