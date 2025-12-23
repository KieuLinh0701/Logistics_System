import React from "react";
import { Divider, Tooltip, Typography } from "antd";
import { InfoCircleOutlined } from "@ant-design/icons";

const { Text } = Typography;

interface PromotionCardProps {
  discountAmount: number;
  totalFee: number;
  serviceFee: number;
}

const PromotionCard: React.FC<PromotionCardProps> = ({
  discountAmount,
  totalFee,
  serviceFee,
}) => {

  const renderInfoIcon = () => (
    <span className="create-order-promotion-card info-icon">
      <InfoCircleOutlined />
    </span>
  );

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
    </div>
  );
};

export default PromotionCard;