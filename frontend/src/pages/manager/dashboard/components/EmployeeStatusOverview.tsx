import React from "react";
import type { ManagerEmployeeStats } from "../../../../types/dashboard";
import { Link } from "react-router-dom";
import { TeamOutlined } from "@ant-design/icons";

interface Props {
  data: ManagerEmployeeStats;
}

export const EmployeeStatusOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    { label: "Tổng nhân viên", value: data.total, key: "total" },
    { label: "Đang làm việc", value: data.active, key: "active" },
    { label: "Tạm nghỉ việc", value: data.inactive, key: "inactive" },
    { label: "Đã nghỉ việc", value: data.leave, key: "leave" },
  ];

  return (
    <div className="manager-dashboard-employee-status-overview">
      <div className="manager-dashboard-employee-status-header">
        <h3 className="manager-dashboard-employee-status-title">
          <TeamOutlined className="manager-dashboard-five-icon"/>
          Thống kê nhân viên</h3>
        <Link to="/employees/list" className="manager-dashboard-employee-status-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="manager-dashboard-employee-status-grid">
        {stats.map((stat) => (
          <div key={stat.key} className="manager-dashboard-employee-status-item">
            <div className="manager-dashboard-employee-status-value">{stat.value}</div>
            <div className="manager-dashboard-employee-status-label">{stat.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
};