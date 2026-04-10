import React from "react";
import "./RecruitmentShared.css";

interface RecruitmentFilterPanelProps {
  children: React.ReactNode;
}

const RecruitmentFilterPanel: React.FC<RecruitmentFilterPanelProps> = ({ children }) => {
  return <div className="hr-recruitment-filter-panel">{children}</div>;
};

export default RecruitmentFilterPanel;
