import React from "react";
import {Typography} from "antd";
import "./RecruitmentShared.css";

const { Title } = Typography;

interface RecruitmentHeaderProps {
  title: string;
  subtitle?: string;
  rightContent?: React.ReactNode;
}

const RecruitmentHeader: React.FC<RecruitmentHeaderProps> = ({ title, subtitle, rightContent }) => {
  return (
    <div className="hr-recruitment-header">
      <div>
        <Title level={3} className="hr-recruitment-title">
          {title}
        </Title>
        {subtitle ? <p className="hr-recruitment-subtitle">{subtitle}</p> : null}
      </div>
      {rightContent}
    </div>
  );
};

export default RecruitmentHeader;
