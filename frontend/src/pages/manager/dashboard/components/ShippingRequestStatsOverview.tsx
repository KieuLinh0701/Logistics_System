import React from "react";
import { Link } from "react-router-dom";
import type { ManagerShippingRequestStats } from "../../../../types/dashboard";
import { GlobalOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerShippingRequestStats;
}

export const ShippingRequestStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng số", value: data.total, key: "total" },
    { label: "Chờ xử lý", value: data.pending, key: "pending" },
    { label: "Đang xử lý", value: data.processing, key: "processing" },
    { label: "Đã xử lý", value: data.resolved, key: "resolved" },
    { label: "Từ chối", value: data.rejected, key: "rejected" },
    { label: "Đã hủy", value: data.cancelled, key: "cancelled" },
  ];

  return (
    <div className="manager-dashboard-six-overview">
      <div className="manager-dashboard-six-header">
        <h3 className="manager-dashboard-six-title">
          <GlobalOutlined className="manager-dashboard-six-icon"/>
          Thống kê yêu cầu hỗ trợ và khiếu nại</h3>
        <Link to="/supports" className="manager-dashboard-six-view-detail">
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