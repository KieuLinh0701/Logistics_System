import React from "react";
import { Card, Col, Row, Statistic } from "antd";
import { DollarOutlined } from "@ant-design/icons";

interface SubmissionSummary {
  totalSubmitted: number;
  totalDiscrepancy: number;
  totalSubmissions: number;
}

interface CODSubmissionsStatsProps {
  summary: SubmissionSummary;
}

const CODSubmissionsStats: React.FC<CODSubmissionsStatsProps> = ({ summary }) => {
  return (
    <Row gutter={16}>
      <Col xs={24} sm={8}>
        <Card>
          <Statistic
            title="Tổng đã nộp"
            value={summary.totalSubmitted}
            prefix={<DollarOutlined />}
            formatter={(value) => `${value?.toLocaleString()}đ`}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card>
          <Statistic
            title="Tổng chênh lệch"
            value={summary.totalDiscrepancy}
            formatter={(value) => `${value?.toLocaleString()}đ`}
          />
        </Card>
      </Col>
      <Col xs={24} sm={8}>
        <Card>
          <Statistic title="Số lần nộp" value={summary.totalSubmissions} />
        </Card>
      </Col>
    </Row>
  );
};

export default CODSubmissionsStats;
