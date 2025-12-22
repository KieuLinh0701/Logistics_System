import React from "react";
import type { UserProductStats } from "../../../../types/dashboard";
import { Link } from "react-router-dom";

interface Props {
  data: UserProductStats;
}

export const ProductStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng sản phẩm", value: data.total, key: "total" },
    { label: "Hết hàng", value: data.outOfStock, key: "outOfStock" },
    { label: "Sắp hết hàng", value: data.lowStock, key: "lowStock" },
    { label: "Đang hoạt động", value: data.active, key: "active" },
  ];

  return (
    <div className="dashboard-product-stats-overview">
      <div className="dashboard-product-status-header">
        <h3 className="dashboard-product-status-title">Thống kê sản phẩm</h3>
        <Link to="/products" className="dashboard-product-status-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="dashboard-product-stats-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="dashboard-product-stats-item">
            <div className="dashboard-product-stats-value">{stat.value}</div>
            <div className="dashboard-product-stats-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};