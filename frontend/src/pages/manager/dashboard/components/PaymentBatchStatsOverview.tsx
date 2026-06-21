import React from "react";
import {Link} from "react-router-dom";
import type {ManagerPaymentSubmissionBatchStats} from "../../../../types/dashboard";
import {CheckCircleOutlined} from "@ant-design/icons";

interface Props {
  data: ManagerPaymentSubmissionBatchStats;
}

export const PaymentBatchStatsOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng số", value: data.total, key: "total" },
    { label: "Đang mở", value: data.open, key: "open" },
    { label: "Đang đối soát", value: data.processing, key: "processing" },
    { label: "Đã đối soát", value: data.completed, key: "completed" },
  ];

  return (
    <div className="manager-dashboard-four-overview">
      <div className="manager-dashboard-four-header">
        <h3 className="manager-dashboard-four-title">
          <CheckCircleOutlined className="manager-dashboard-four-icon"/>
          Thống kê phiên đối soát</h3>
        <Link to="/settlements" className="manager-dashboard-four-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="manager-dashboard-four-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="manager-dashboard-four-item">
            <div className="manager-dashboard-four-value">{stat.value}</div>
            <div className="manager-dashboard-four-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};