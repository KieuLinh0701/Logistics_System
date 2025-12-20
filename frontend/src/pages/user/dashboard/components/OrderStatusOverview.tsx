import React from "react";
import type { UserOrderStats } from "../../../../types/dashboard";
import { Link } from "react-router-dom"; 

interface Props {
  data: UserOrderStats;
}

export const OrderStatusOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng", value: data.total, key: "total" },
    { label: "Nháp", value: data.draft, key: "draft" },
    { label: "Chờ xử lý", value: data.pending, key: "pending" },
    { label: "Đã xác nhận", value: data.confirmed, key: "confirmed" },
    { label: "Sẵn sàng lấy", value: data.readyForPickup, key: "readyForPickup" },
    { label: "Đang lấy hàng", value: data.pickingUp, key: "pickingUp" },
    { label: "Đang vận chuyển", value: data.shipping, key: "shipping" },
    { label: "Đang giao", value: data.delivering, key: "delivering" },
    { label: "Đã giao", value: data.delivered, key: "delivered" },
    { label: "Giao thất bại", value: data.failedDelivery, key: "failedDelivery" },
    { label: "Đang hoàn", value: data.returning, key: "returning" },
    { label: "Đã hủy/hoàn", value: data.returnedCancelled, key: "returnedCancelled" },
  ];

  return (
    <div className="dashboard-order-status-overview">
      <div className="dashboard-order-status-header">
        <h3 className="dashboard-order-status-title">Thống kê đơn hàng</h3>
        <Link to="/orders/list" className="dashboard-order-status-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="dashboard-order-status-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="dashboard-order-status-item">
            <div className="dashboard-order-status-value">{stat.value}</div>
            <div className="dashboard-order-status-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};