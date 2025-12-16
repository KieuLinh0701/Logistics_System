import React from "react";
import { translateOrderPickupType, translateOrderStatus } from "../../../../../utils/orderUtils";
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

   const timeLabel = order.status === "RETURNED" ? "Thời gian hoàn hàng:" : "Thời gian giao hàng:";

  return (
    <>
      {/* Card thông tin đơn hàng */}
      <div className="order-detail-card">
        <Title level={5} className="order-detail-card-title order-detail-card-title-main">
          Thông tin đơn hàng
        </Title>

        <div className="order-detail-card-inner">
          <div className="order-detail-card-column">
            {renderField("Trạng thái:", translateOrderStatus(order.status))}
            {renderField("Hình thức lấy hàng:", translateOrderPickupType(order.pickupType))}
            {renderField("Thời gian tạo đơn:", order.createdAt ? new Date(order.createdAt).toLocaleString() : null)}
            {order.deliveredAt && renderField(timeLabel, new Date(order.deliveredAt).toLocaleString())}
            {order.paidAt && renderField("Thời gian thanh toán:", new Date(order.paidAt).toLocaleString())}
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