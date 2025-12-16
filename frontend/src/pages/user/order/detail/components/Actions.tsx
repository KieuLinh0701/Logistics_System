import React from "react";
import { CloseCircleOutlined, EditOutlined, PlayCircleOutlined, PrinterOutlined, CustomerServiceOutlined } from "@ant-design/icons";

interface Props {
  canPublic: boolean;
  canEdit: boolean;
  canCancel: boolean;
  canPrint: boolean;
  canDelete: boolean;
  canRequest: boolean;
  canReady: boolean;
  onPublic: () => void;
  onEdit: () => void;
  onCancel: () => void;
  onPrint: () => void;
  onDelete: () => void;
  onReady: () => void;
  onCreateRequest: () => void;
}

const Actions: React.FC<Props> = ({
  canPublic,
  canEdit,
  canCancel,
  canPrint,
  canDelete,
  canRequest,
  canReady,
  onPublic,
  onEdit,
  onCancel,
  onPrint,
  onDelete,
  onCreateRequest,
  onReady,
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

      <button
        className={`order-detail-support-btn ${!canRequest ? "disabled" : ""}`}
        onClick={canRequest ? onCreateRequest : undefined}
        disabled={!canRequest}
      >
        <CustomerServiceOutlined /> Yêu cầu hỗ trợ
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

      {!canDelete && (
        <button
          className={`order-detail-cancel-btn ${!canCancel ? "disabled" : ""}`}
          onClick={canCancel ? onCancel : undefined}
          disabled={!canCancel}
        >
          <CloseCircleOutlined /> Hủy
        </button>
      )}

      {canDelete && (
        <button
          className="order-detail-cancel-btn" 
          onClick={onDelete}
        >
          <CloseCircleOutlined /> Xóa
        </button>
      )}

      {canReady && (
        <button className="order-detail-public-btn" onClick={onReady}>
          <PlayCircleOutlined /> Sẵn sàng để lấy
        </button>
      )}

      {canPublic && (
        <button className="order-detail-public-btn" onClick={onPublic}>
          <PlayCircleOutlined /> Chuyển xử lý
        </button>
      )}
    </div>
  </div>
);

export default Actions;