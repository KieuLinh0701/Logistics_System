import React from "react";
import { Link } from "react-router-dom";
import type { ManagerPaymentSubmissionBatchStats } from "../../../../types/dashboard";
import { CheckCircleOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerPaymentSubmissionBatchStats;
}

export const PaymentBatchStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng số", value: data.total, key: "total" },
    { label: "Chờ xử lý", value: data.pending, key: "pending" },
    { label: "Đang kiểm tra", value: data.checking, key: "checking" },
    { label: "Hoàn thành", value: data.completed, key: "completed" },
    { label: "Một phần", value: data.partial, key: "partial" },
    { label: "Đã hủy", value: data.cancelled, key: "cancelled" },
  ];

  return (
    <div className="manager-dashboard-six-overview">
      <div className="manager-dashboard-six-header">
        <h3 className="manager-dashboard-six-title">
          <CheckCircleOutlined className="manager-dashboard-six-icon"/>
          Thống kê phiên đối soát</h3>
        <Link to="/settlements" className="manager-dashboard-six-view-detail">
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