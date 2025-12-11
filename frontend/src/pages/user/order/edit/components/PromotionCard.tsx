import React, { useEffect } from "react";
import { Button, Divider, Tooltip, Typography } from "antd";
import { GiftOutlined, TagOutlined, DeleteOutlined, InfoCircleOutlined } from "@ant-design/icons";
import type { Promotion } from "../../../../../types/promotion";

const { Text } = Typography;

interface PromotionCardProps {
  discountAmount: number;
  totalFee: number;
  serviceFee: number;
  selectedPromotion: Promotion | null;
  setSelectedPromotion: (value: Promotion | null) => void;
  setShowPromoModal: (value: boolean) => void;
  disabled: boolean;
}

const PromotionCard: React.FC<PromotionCardProps> = ({
  discountAmount,
  totalFee,
  serviceFee,
  selectedPromotion,
  setSelectedPromotion,
  setShowPromoModal,
  disabled,
}) => {
  
  const renderInfoIcon = () => (
    <span className="create-order-promotion-card info-icon">
      <InfoCircleOutlined />
    </span>
  );

   useEffect(() => {
    console.log("promotion", selectedPromotion);
    }, [selectedPromotion]);
  

  return (
    <div className="create-order-promotion-card">
      <div className="create-order-promotion-card section">
        <Text strong>
          Phí dịch vụ
          <Tooltip title="Tổng các khoản phí dịch vụ sau VAT, cộng thêm phí COD và phí bảo hiểm">
            {renderInfoIcon()}
          </Tooltip>
        </Text>
        <div>{serviceFee.toLocaleString()} VNĐ</div>
      </div>

      {/* Discount Section */}
      {discountAmount > 0 && (
        <div className="create-order-promotion-card section">
          <Text strong>
            Giảm giá
            <Tooltip title="Số tiền giảm trừ trực tiếp vào phí dịch vụ">
              {renderInfoIcon()}
            </Tooltip>
          </Text>
          <div>-{discountAmount.toLocaleString()} VNĐ</div>
        </div>
      )}

      <Divider className="create-order-promotion-card divider" />

      {/* Total Fee Section */}
      <div className="create-order-promotion-card section">
        <Text strong>
          Tổng phí
          <Tooltip title="Phí dịch vụ thực tế cần thanh toán">
            {renderInfoIcon()}
          </Tooltip>
        </Text>
        <div>
          <span className="create-order-promotion-card total-fee">{totalFee.toLocaleString()} VNĐ</span></div>
      </div>

      {/* Promotion Button */}
      <Button
        type="dashed"
        block
        className="create-order-promotion-card promo-button"
        icon={selectedPromotion ? <TagOutlined /> : <GiftOutlined />}
        onClick={() => setShowPromoModal(true)}
        disabled={disabled}
      >
        {selectedPromotion ? "Đổi mã khuyến mãi" : "Chọn mã khuyến mãi"}
      </Button>

      {/* Selected Promotion Display */}
      {selectedPromotion && (
        <div className="create-order-promotion-card selected-promotion">
          <div>
            <Text className="create-order-promotion-card promotion-code">
              1 mã giảm giá đã áp dụng:
            </Text>
            <div>{selectedPromotion.code}</div>
          </div>
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            onClick={() => setSelectedPromotion(null)}
            aria-label="Xóa mã khuyến mãi"
          />
        </div>
      )}
    </div>
  );
};

export default PromotionCard;