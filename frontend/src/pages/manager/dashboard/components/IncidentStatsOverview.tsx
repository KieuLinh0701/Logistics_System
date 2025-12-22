import React from "react";
import { Link } from "react-router-dom";
import { Tooltip } from "antd";
import type { ManagerIncidentStats } from "../../../../types/dashboard";
import { WarningOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerIncidentStats;
}

export const IncidentStatsOverview: React.FC<Props> = ({ data }) => {
  const mainStats = [
    { label: "Tổng sự cố", value: data.total, key: "total" },
    {
      label: "Chờ xử lý",
      value: data.pending,
      key: "pending",
      tooltip: `Chờ xử lý: cao ${data.pendingHight}, trung bình ${data.pendingMedium}, thấp ${data.pendingLow}`
    },
    { label: "Đang xử lý", value: data.processing, key: "processing" },
    { label: "Đã giải quyết", value: data.resolved, key: "resolved" },
    { label: "Đã từ chối", value: data.rejected, key: "rejected" },
  ];

  return (
    <div className="manager-dashboard-five-overview">
      <div className="manager-dashboard-five-header">
        <h3 className="manager-dashboard-five-title">
          <WarningOutlined className="manager-dashboard-five-icon"/>
          Thống kê báo cáo sự cố</h3>
        <Link to="/orders/incidents" className="manager-dashboard-five-view-detail">
          Xem chi tiết
        </Link>
      </div>

      <div className="manager-dashboard-five-grid">
        {mainStats.map((stat) => (
          stat.tooltip ? (
            <Tooltip key={stat.key} title={stat.tooltip} placement="top" color="#1C3D90">
              <div className="manager-dashboard-five-item">
                <div className="manager-dashboard-five-value">{stat.value}</div>
                <div className="manager-dashboard-five-label">{stat.label}</div>
              </div>
            </Tooltip>
          ) : (
            <div key={stat.key} className="manager-dashboard-five-item">
              <div className="manager-dashboard-five-value">{stat.value}</div>
              <div className="manager-dashboard-five-label">{stat.label}</div>
            </div>
          )
        ))}
      </div>
    </div>
  );
};