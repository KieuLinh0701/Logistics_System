import React from "react";
import { Card, Button, Tag } from "antd";
import type { Job } from "../../../../types/recruitment";
import { roleTypeLabelMap, postingStatusLabelMap, postingStatusColorMap } from "../../../common/recruitment/recruitmentHelpers";
import { shiftLabel } from "../../../../utils/recruitmentHelpers";
import "../styles/recruitment.css";

interface JobCardProps {
  job: Job;
  onView: (id: number) => void;
  onApply: (id: number) => void;
}

const JobCard: React.FC<JobCardProps> = ({ job, onView, onApply }) => {
  const statusLabel = postingStatusLabelMap[job.status as keyof typeof postingStatusLabelMap] || job.status;
  const statusColor = postingStatusColorMap[job.status as keyof typeof postingStatusColorMap] || undefined;
  const roleLabel = roleTypeLabelMap[job.roleType as keyof typeof roleTypeLabelMap] || job.roleType;

  return (
    <Card className="public-job-card" hoverable onClick={() => onView(job.id)}>
      <div className="public-job-card-head">
        <h3 className="public-job-title">{job.title}</h3>
        <div>
          <Tag color={statusColor} style={{ fontWeight: 600 }}>{statusLabel}</Tag>
        </div>
      </div>
      <div className="public-job-meta">
        <div className="public-job-role">{roleLabel}</div>
        <div className="public-job-office">{job.officeName || `#${job.officeId}`}</div>
        <div className="public-job-quantity-shift">{(job.quantityNeeded ?? 1) + ' vị trí • ' + shiftLabel(job.shift)}</div>
      </div>
      <div className="public-job-actions">
        <Button type="default" onClick={(e) => { e.stopPropagation(); onView(job.id); }}>Xem chi tiết</Button>
        <Button type="primary" onClick={(e) => { e.stopPropagation(); onApply(job.id); }}>Ứng tuyển</Button>
      </div>
    </Card>
  );
};

export default JobCard;
