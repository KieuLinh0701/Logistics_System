import React from "react";
import Title from "antd/es/typography/Title";
import type { Order } from "../../../../../types/order";
import { translateOrderCodStatus, translateOrderPayerType, translateOrderPaymentStatus } from "../../../../../utils/orderUtils";

interface PaymentInfoProps {
  order: Order;
}

const PaymentInfo: React.FC<PaymentInfoProps> = ({ order }) => {
  const renderPaymentField = (label: string, value: any) => (
    <div className="order-detail-card-field" style={{ justifyContent: 'space-between' }}>
      <strong className="order-detail-card-label">{label}</strong>
      <span 
        className="order-detail-card-value" 
      >
        {value}
      </span>
    </div>
  );

  return (
    <div className="order-detail-card">
      <Title level={5} className="order-detail-card-title" style={{ width: '100%' }}>
        Thông tin thanh toán
      </Title>

      <div className="order-detail-card-inner">
        {/* Cột 1 - Thông tin thanh toán */}
        <div className="order-detail-card-column">
          {renderPaymentField("Người thanh toán:", translateOrderPayerType(order.payer))}
          {renderPaymentField("Trạng thái:", translateOrderPaymentStatus(order.paymentStatus))}
          {renderPaymentField("Trạng thái COD:", translateOrderCodStatus(order.codStatus))}
        </div>

        {/* Cột 2 - Phí dịch vụ */}
        <div className="order-detail-card-column">
          {renderPaymentField("Phí vận chuyển:", `${(order.discountAmount + order.totalFee).toLocaleString()} VNĐ`)}
          {renderPaymentField("Giảm giá:", `${order.discountAmount.toLocaleString()} VNĐ`)}
          {renderPaymentField("Tổng phí:", `${order.totalFee.toLocaleString()} VNĐ`)}
        </div>
      </div>
    </div>
  );
};

export default PaymentInfo;