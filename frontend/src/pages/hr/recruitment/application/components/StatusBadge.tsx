import React from "react";
import type { JobApplicationStatus } from "../../../../../types/recruitment";
import { applicationStatusLabelMap } from "../../../../common/recruitment/recruitmentHelpers";
import "./ApplicationComponents.css";

interface StatusBadgeProps {
  status: JobApplicationStatus;
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  return <span className="hr-application-status">{applicationStatusLabelMap[status]}</span>;
};

export default StatusBadge;
