import React from "react";
import type { UserRevenueStats } from "../../../../types/dashboard";
import {
  WalletOutlined,
  ClockCircleOutlined,
  MoneyCollectOutlined,
  CalendarOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons';
import { Tooltip } from 'antd';
import { Link } from "react-router-dom";

interface Props {
  data: UserRevenueStats;
}

export const RevenueOverview: React.FC<Props> = ({ data }) => {
  const stats = [
    {
      label: "Đã nhận",
      value: data.received.toLocaleString(),
      key: "received",
      icon: <WalletOutlined />,
      suffix: "₫",
      color: "#1C3D90",
      tooltip: "Tổng số tiền bạn đã nhận được từ các phiên đối soát đã hoàn thành"
    },
    {
      label: "Sắp nhận",
      value: data.nextSettlement.toLocaleString(),
      key: "nextSettlement",
      icon: <ClockCircleOutlined />,
      suffix: "₫",
      color: "#fa8c16",
      tooltip: "Số tiền tạm tính cho kỳ đối soát tiếp theo (COD − phí). Âm nghĩa là phí dịch vụ lớn hơn COD, bạn cần thanh toán cho hệ thống."
    },
    {
      label: "Còn nợ",
      value: data.pendingDebt.toLocaleString(),
      key: "pendingDebt",
      icon: <MoneyCollectOutlined />,
      suffix: "₫",
      color: "#f5222d",
      tooltip: "Tổng số tiền bạn chưa thanh toán cho các phiên đối soát"
    },
    {
      label: "Ngày đối soát tiếp theo",
      value: data.nextSettlementDate,
      key: "nextSettlementDate",
      icon: <CalendarOutlined />,
      suffix: "",
      color: "#52c41a",
      tooltip: "Ngày hệ thống sẽ thực hiện đối soát và chuyển tiền tiếp theo theo lịch đối soát của bạn"
    },
  ];

  return (
    <div className="dashboard-revenue-overview">
      <div className="dashboard-revenue-header">
        <h3 className="dashboard-revenue-title">Thống kê doanh thu</h3>
        <Link to="/settlements" className="dashboard-revenue-view-detail">
          Xem chi tiết
        </Link>
      </div>
      <div className="dashboard-revenue-stats">
        {stats.map((stat) => (
          <Tooltip
            key={stat.key}
            title={stat.tooltip}
            placement="top"
            color="#1C3D90"
          >
            <div className="dashboard-revenue-stat">
              <div className="dashboard-revenue-stat-icon" style={{ color: stat.color }}>
                {stat.icon}
              </div>
              <div className="dashboard-revenue-stat-content">
                <div className="dashboard-revenue-stat-value">
                  {stat.value}
                  {stat.suffix && <span className="dashboard-revenue-stat-suffix">{stat.suffix}</span>}
                </div>
                <div className="dashboard-revenue-stat-label">
                  {stat.label}
                  <QuestionCircleOutlined className="dashboard-revenue-stat-tooltip-icon" />
                </div>
              </div>
            </div>
          </Tooltip>
        ))}
      </div>
    </div>
  );
};