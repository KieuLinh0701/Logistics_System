import React from "react";
import { Card, Col, Row, Statistic } from "antd";
import { CheckCircleOutlined, ClockCircleOutlined, DollarOutlined } from "@ant-design/icons";

interface Summary {
  totalCollected: number;
  totalSubmitted: number;
  totalPending: number;
  transactionCount: number;
}

interface CODTransactionsStatsProps {
  summary: Summary;
}

const CODTransactionsStats: React.FC<CODTransactionsStatsProps> = ({ summary }) => {
  return (
    <Row gutter={16}>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Tổng đã thu"
            value={summary.totalCollected}
            prefix={<DollarOutlined />}
            formatter={(value) => `${value?.toLocaleString()}đ`}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Đã nộp"
            value={summary.totalSubmitted}
            prefix={<CheckCircleOutlined />}
            formatter={(value) => `${value?.toLocaleString()}đ`}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic
            title="Còn nợ"
            value={summary.totalPending}
            prefix={<ClockCircleOutlined />}
            formatter={(value) => `${value?.toLocaleString()}đ`}
          />
        </Card>
      </Col>
      <Col xs={24} sm={12} lg={6}>
        <Card>
          <Statistic title="Tổng giao dịch" value={summary.transactionCount} />
        </Card>
      </Col>
    </Row>
  );
};

export default CODTransactionsStats;
