import React from "react";
import "./RecruitmentShared.css";

interface RecruitmentStatsCardProps {
  label: string;
  value: number;
}

const RecruitmentStatsCard: React.FC<RecruitmentStatsCardProps> = ({ label, value }) => {
  return (
    <div className="hr-recruitment-stats-card">
      <div className="hr-recruitment-stats-label">{label}</div>
      <div className="hr-recruitment-stats-value">{value}</div>
    </div>
  );
};

export default RecruitmentStatsCard;
