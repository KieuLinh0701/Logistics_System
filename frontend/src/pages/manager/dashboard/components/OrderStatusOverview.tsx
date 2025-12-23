import React from "react";
import type { ManagerOrderStats } from "../../../../types/dashboard";
import { Link } from "react-router-dom";
import { ShoppingOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerOrderStats;
}

export const OrderStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng đơn hàng", value: data.total, key: "total" },
    { label: "Chờ xác nhận", value: data.pending, key: "pending" },          // PENDING
    { label: "Đã xác nhận", value: data.confirmed, key: "confirmed" },       // CONFIRMED
    { label: "Sẵn sàng lấy", value: data.readyForPickup, key: "readyForPickup" }, // READY_FOR_PICKUP
    { label: "Đang lấy/đã lấy", value: data.pickingOrPicked, key: "pickingOrPicked" }, // PICKING_UP / PICKED_UP
    { label: "Trong kho", value: data.inWarehouse, key: "inWarehouse" },     // AT_ORIGIN_OFFICE / AT_DEST_OFFICE
    { label: "Khách tại bưu cục", value: data.customerAtOffice, key: "customerAtOffice" }, // CONFIRMED + pickupType = AT_OFFICE
    { label: "Đang giao", value: data.delivering, key: "delivering" },      // DELIVERING
    { label: "Đã giao", value: data.delivered, key: "delivered" },          // DELIVERED
    { label: "Đang hoàn", value: data.returning, key: "returning" },        // RETURNING
    { label: "Đã hoàn", value: data.returned, key: "returned" },            // RETURNED
    { label: "Giao thất bại", value: data.failedDelivery, key: "failedDelivery" }, // FAILED_DELIVERY
  ];

  return (
    <div className="manager-dashboard-six-overview">
      <div className="manager-dashboard-six-header">
        <h3 className="manager-dashboard-six-title">
          <ShoppingOutlined className="manager-dashboard-six-icon"/>
          Thống kê đơn hàng
          </h3>
        <Link to="/orders/list" className="manager-dashboard-six-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="manager-dashboard-six-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="manager-dashboard-six-item">
            <div className="manager-dashboard-six-value">{stat.value}</div>
            <div className="manager-dashboard-six-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};