import React from "react";
import { Link } from "react-router-dom";
import type { ManagerShipmentStats } from "../../../../types/dashboard";
import { TruckOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerShipmentStats;
}

export const ShipmentStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng", value: data.total, key: "total" },
    { label: "Chờ khởi hành", value: data.pending, key: "pending" },
    { label: "Đang vận chuyển", value: data.inTransit, key: "inTransit" },
    { label: "Hoàn thành", value: data.completed, key: "completed" },
    { label: "Đã hủy", value: data.cancelled, key: "cancelled" },
  ];

  return (
    <div className="manager-dashboard-five-overview">
      <div className="manager-dashboard-five-header">
        <h3 className="manager-dashboard-five-title">
          <TruckOutlined className="manager-dashboard-five-icon"/>
          Thống kê chuyến giao hàng</h3>
        <Link to="/shipments" className="manager-dashboard-five-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="manager-dashboard-five-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="manager-dashboard-five-item">
            <div className="manager-dashboard-five-value">{stat.value}</div>
            <div className="manager-dashboard-five-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};