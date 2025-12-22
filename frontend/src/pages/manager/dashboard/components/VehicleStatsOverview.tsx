import React from "react";
import type { ManagerVehicleStats } from "../../../../types/dashboard";
import { Link } from "react-router-dom";
import { CarOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerVehicleStats;
}

export const VehicleStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng xe", value: data.total, key: "total" },
    { label: "Sẵn sàng", value: data.available, key: "available" },
    { label: "Đang sử dụng", value: data.inUse, key: "inUse" },
    { label: "Bảo trì", value: data.maintenance, key: "maintenance" },
    { label: "Ngừng hoạt động", value: data.archived, key: "archived" },
  ];

  return (
    <div className="manager-dashboard-vehicle-status-overview">
      <div className="manager-dashboard-vehicle-status-header">
        <h3 className="manager-dashboard-vehicle-status-title">
          <CarOutlined className="manager-dashboard-five-icon"/>
          Thống kê phương tiện vận chuyển</h3>
        <Link to="/vehicles" className="manager-dashboard-vehicle-status-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="manager-dashboard-vehicle-status-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="manager-dashboard-vehicle-status-item">
            <div className="manager-dashboard-vehicle-status-value">{stat.value}</div>
            <div className="manager-dashboard-vehicle-status-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};