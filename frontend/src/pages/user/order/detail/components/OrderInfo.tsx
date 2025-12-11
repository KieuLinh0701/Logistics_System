import React from "react";
import { translateOrderStatus } from "../../../../../utils/orderUtils";
import Title from "antd/es/typography/Title";
import type { Order } from "../../../../../types/order";

interface OrderInfoProps {
  order: Order;
}

const OrderInfo: React.FC<OrderInfoProps> = ({ order }) => {
  const renderField = (label: string, value: any) => {
    const displayValue = value === null || value === undefined || value === "" 
      ? <span className="order-detail-card-na-tag">N/A</span>
      : value;

    return (
      <div className="order-detail-card-field">
        <strong className="order-detail-card-label">{label}</strong>
        <span className="order-detail-card-value">
          {displayValue}
        </span>
      </div>
    );
  };

  return (
    <>
      {/* Card thông tin đơn hàng */}
      <div className="order-detail-card">
        <Title level={5} className="order-detail-card-title order-detail-card-title-main">
          Thông tin đơn hàng
        </Title>

        <div className="order-detail-card-inner">
          <div className="order-detail-card-column">
            {renderField("Ngày tạo:", order.createdAt ? new Date(order.createdAt).toLocaleString() : null)}
            {renderField("Ngày giao hàng:", order.deliveredAt ? new Date(order.deliveredAt).toLocaleString() : null)}
            {renderField("Trạng thái:", translateOrderStatus(order.status))}
          </div>

          <div className="order-detail-card-column">
            {renderField("Dịch vụ:", order.serviceType?.name)}
            {renderField("Trọng lượng:", `${order.weight} kg`)}
            {renderField("Giá trị:", `${order.orderValue.toLocaleString()} VNĐ`)}
            {renderField("COD:", `${(order.cod || 0).toLocaleString()} VNĐ`)}
          </div>
        </div>
      </div>

      {/* Card Notes */}
      {order.notes && (
        <div className="order-detail-card">
          <Title level={5} className="order-detail-card-title order-detail-card-title-main">
            Ghi chú
          </Title>
          <div className="order-detail-card-field">
            <span className="order-detail-card-value">
              {order.notes}
            </span>
          </div>
        </div>
      )}
    </>
  );
};

export default OrderInfo;