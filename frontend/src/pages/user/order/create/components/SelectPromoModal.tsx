import React from "react";
import { Modal, Input, Radio, List, Button, Tag } from "antd";
import type { Promotion } from "../../../../../types/promotion";

interface Props {
  open: boolean;
  onCancel: () => void;
  promotions: Promotion[];
  selectedPromo: Promotion | null;
  setSelectedPromo: (value: Promotion | null) => void;
  onSearch: (value: string) => void;
  onLoadMore: () => void;
  nextCursor: number | null;
}

const SelectedPromoModal: React.FC<Props> = ({
  open,
  onCancel,
  promotions,
  selectedPromo,
  setSelectedPromo,
  onSearch,
  onLoadMore,
  nextCursor,
}) => {
  
  const handleConfirm = () => {
    // Không cần làm gì thêm vì selectedPromo đã được set trước đó
    onCancel();
  };

  const handleClearSelection = () => {
    setSelectedPromo(null);
  };

  return (
    <Modal 
      title="Chọn khuyến mãi" 
      open={open} 
      onCancel={onCancel} 
      footer={null}
      width={600}
    >
      <Input.Search
        placeholder="Tìm theo mã khuyến mãi, mô tả..."
        allowClear
        onSearch={onSearch}
        style={{ marginBottom: 16 }}
      />

      {/* Thông báo khi đã chọn */}
      {selectedPromo && (
        <div style={{ 
          marginBottom: 16, 
          padding: "12px", 
          background: "#f6ffed", 
          border: "1px solid #b7eb8f",
          borderRadius: 6
        }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div>
              <strong>Đã chọn: </strong>
              <Tag color="green">{selectedPromo.code}</Tag>
            </div>
            <Button 
              size="small" 
              type="text" 
              danger 
              onClick={handleClearSelection}
            >
              Bỏ chọn
            </Button>
          </div>
        </div>
      )}

      <div style={{ maxHeight: 400, overflow: 'auto', marginBottom: 16 }}>
        <Radio.Group
          style={{ width: "100%" }}
          value={selectedPromo?.id || null}
          onChange={(e) => {
            const selectedId = e.target.value;
            const promo = promotions.find((p) => p.id === selectedId) || null;
            setSelectedPromo(promo);
          }}
        >
          <List
            dataSource={promotions}
            renderItem={(promo) => (
              <List.Item
                style={{
                  padding: "12px 16px",
                  borderBottom: "1px solid #f0f0f0",
                  cursor: "pointer",
                  backgroundColor: selectedPromo?.id === promo.id ? "#f0f7ff" : "transparent",
                  borderRadius: 6,
                  marginBottom: 4
                }}
                onClick={() => {
                  const newSelection = selectedPromo?.id === promo.id ? null : promo;
                  setSelectedPromo(newSelection);
                }}
              >
                <Radio value={promo.id} style={{ width: "100%" }}>
                  <div style={{ display: "flex", flexDirection: "column", width: "100%" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
                      <strong style={{ fontSize: 14 }}>{promo.code}</strong>
                    </div>
                    <div style={{ fontSize: 12, color: "#666", marginTop: 4 }}>
                      {promo.description}
                    </div>
                    {promo.endDate && (
                      <div style={{ fontSize: 11, color: "#999", marginTop: 2 }}>
                        HSD: {new Date(promo.endDate).toLocaleDateString("vi-VN")}
                      </div>
                    )}
                  </div>
                </Radio>
              </List.Item>
            )}
          />
        </Radio.Group>
      </div>

      {nextCursor && (
        <div style={{ textAlign: "center", marginBottom: 16 }}>
          <Button
            onClick={onLoadMore}
            style={{
              background: "#1C3D90",
              color: "#fff",
              borderColor: "#1C3D90",
            }}
          >
            Xem thêm khuyến mãi
          </Button>
        </div>
      )}

      <div
        style={{
          display: "flex",
          justifyContent: "flex-end",
          gap: 8,
          marginTop: 16,
          borderTop: "1px solid #f0f0f0",
          paddingTop: 16
        }}
      >
        <Button 
          style={{ borderColor: "#1C3D90", color: "#1C3D90" }} 
          onClick={onCancel}
        >
          Hủy
        </Button>
        <Button
          type="primary"
          style={{ background: "#1C3D90", borderColor: "#1C3D90" }}
          onClick={handleConfirm}
        >
          Xác nhận
        </Button>
      </div>
    </Modal>
  );
};

export default SelectedPromoModal;