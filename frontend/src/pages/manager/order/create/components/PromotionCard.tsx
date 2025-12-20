import React from "react";
import { Tooltip, Typography } from "antd";
import { InfoCircleOutlined } from "@ant-design/icons";

const { Text } = Typography;

interface PromotionCardProps {
  totalFee: number;
}

const PromotionCard: React.FC<PromotionCardProps> = ({
  totalFee,
}) => {
  const renderInfoIcon = () => (
    <span className="create-order-promotion-card info-icon">
      <InfoCircleOutlined />
    </span>
  );

  return (
    <div className="create-order-promotion-card">
      {/* Total Fee Section */}
      <div className="create-order-promotion-card section">
        <Text strong>
          Tổng phí
          <Tooltip title="Tổng các khoản phí dịch vụ sau VAT, cộng thêm phí COD và phí bảo hiểm">
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