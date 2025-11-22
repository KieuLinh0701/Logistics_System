import React from "react";
import { Button, Divider, Tooltip, Typography } from "antd";
import { GiftOutlined, TagOutlined, DeleteOutlined, InfoCircleOutlined } from "@ant-design/icons";
import type { Promotion } from "../../../../../types/promotion";

const { Text } = Typography;

interface Props {
  shippingFee: number;
  discountAmount: number;
  cod: number;
  orderValue: number;
  totalFee: number;
  selectedPromo: Promotion | null;
  setSelectedPromo: (value: Promotion | null) => void;
  setShowPromoModal: (value: boolean) => void;
  disabled: boolean;
}

const PromotionCard: React.FC<Props> = ({
  shippingFee,
  discountAmount,
  cod,
  orderValue,
  totalFee,
  selectedPromo,
  setSelectedPromo,
  setShowPromoModal,
  disabled,
}) => {
  return (
    <div>
      {/* Phí vận chuyển cơ bản */}
      <div style={{ marginBottom: 16 }}>
        <Text strong>
          Phí vận chuyển cơ bản
          <Tooltip title="Tính theo khối lượng, khoảng cách và loại dịch vụ; chưa bao gồm phí COD, phí bảo hiểm và các khoản giảm giá">
            <span style={{ marginLeft: 4, cursor: 'pointer' }}><InfoCircleOutlined /></span>
          </Tooltip>
        </Text>
        <div>{shippingFee.toLocaleString()} VNĐ</div>
      </div>

      {/* Giảm giá */}
      {discountAmount > 0 && (
        <div style={{ marginBottom: 16 }}>
          <Text strong>
            Giảm giá
            <Tooltip title="Số tiền giảm trừ trực tiếp vào phí dịch vụ">
              <span style={{ marginLeft: 4, cursor: 'pointer' }}><InfoCircleOutlined /></span>
            </Tooltip>
          </Text>
          <div>-{discountAmount.toLocaleString()} VNĐ</div>
        </div>
      )}

      <Divider style={{ margin: "8px 0" }} />

      {/* Phí dịch vụ sau VAT */}
      <div style={{ marginBottom: 16 }}>
        <Text strong>
          Phí dịch vụ sau VAT 
          <Tooltip title="Phí vận chuyển sau khi trừ giảm giá, cộng thêm 10% VAT">
            <span style={{ marginLeft: 4, cursor: 'pointer' }}><InfoCircleOutlined /></span>
          </Tooltip>
        </Text>
        <div>{Math.ceil(Math.max(((shippingFee || 0) - (discountAmount || 0)), 0) * 1.1).toLocaleString()} VNĐ</div>
      </div>

      {/* Phí bảo hiểm */}
      <div style={{ marginBottom: 16 }}>
        <Text strong>
          Phí bảo hiểm
          <Tooltip title="Tính dựa trên giá trị hàng hóa, tỉ lệ 0.5%">
            <span style={{ marginLeft: 4, cursor: 'pointer' }}><InfoCircleOutlined /></span>
          </Tooltip>
        </Text>
        <div>+{(orderValue ? orderValue * 0.005 : 0).toLocaleString()} VNĐ</div>
      </div>

      {/* Phí thu COD */}
      <div style={{ marginBottom: 16 }}>
        <Text strong>
          Phí thu COD
          <Tooltip title="Tính 2% trên số tiền thu hộ">
            <span style={{ marginLeft: 4, cursor: 'pointer' }}><InfoCircleOutlined /></span>
          </Tooltip>
        </Text>
        <div>+{(cod ? cod * 0.02 : 0).toLocaleString()} VNĐ</div>
      </div>

      {/* Tổng phí */}
      <Divider style={{ margin: "8px 0" }} />
      <div style={{ marginBottom: 16 }}>
        <Text strong style={{ fontSize: 16 }}>
          Tổng phí
          <Tooltip title="Tổng các khoản phí dịch vụ sau VAT, cộng thêm phí COD và phí bảo hiểm">
            <span style={{ marginLeft: 4, cursor: 'pointer' }}><InfoCircleOutlined /></span>
          </Tooltip>
        </Text>
        <div style={{ fontSize: 18, color: "#FF4D4F" }}>
          {totalFee.toLocaleString()} VNĐ
        </div>
      </div>

      <Button
        type="dashed"
        block
        style={{ marginBottom: 16, borderColor: "#1C3D90", color: "#1C3D90" }}
        icon={selectedPromo ? <TagOutlined /> : <GiftOutlined />}
        onClick={() => setShowPromoModal(true)}
        disabled={disabled}
      >
        {selectedPromo ? "Đổi mã khuyến mãi" : "Chọn mã khuyến mãi"}
      </Button>

      {selectedPromo && (
        <div
          style={{
            marginBottom: 16,
            padding: "8px 12px",
            background: "#f6ffed",
            border: "1px solid #b7eb8f",
            borderRadius: 6,
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div>
            <Text strong style={{ color: "#389e0d" }}>
              1 mã giảm giá đã áp dụng:
            </Text>
            <div>{selectedPromo.code}</div>
          </div>
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            onClick={() => setSelectedPromo(null)}
          />
        </div>
      )}
    </div>
  );
};

export default PromotionCard;