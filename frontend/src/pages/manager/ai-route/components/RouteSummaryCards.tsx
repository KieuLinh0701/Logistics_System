import React from "react";
import { Card, Col, Row, Statistic } from "antd";
import { CarOutlined, ClockCircleOutlined, DollarOutlined, NodeIndexOutlined, TeamOutlined } from "@ant-design/icons";
import { formatCurrency, formatMinutes } from "../utils/routeMapUtils";

interface RouteSummaryCardsProps {
  routeCount: number;
  totalOrders: number;
  totalKm: number;
  totalEtaMinutes: number;
  totalFuel: number;
  totalCod: number;
  compact?: boolean;
}

const RouteSummaryCards: React.FC<RouteSummaryCardsProps> = ({
  routeCount,
  totalOrders,
  totalKm,
  totalEtaMinutes,
  totalFuel,
  totalCod,
  compact,
}) => {
  const gutter: [number, number] = compact ? [8, 8] : [12, 12];
  const colProps = compact
    ? { span: 12 }
    : { xs: 12, sm: 8, lg: 4 };
  const valueStyle = compact ? { fontSize: 16 } : { fontSize: 20 };

  return (
    <div className={`ai-route-summary-cards${compact ? " is-compact" : ""}`}>
      <Row gutter={gutter}>
        <Col {...colProps}>
          <Card size="small" className="ai-route-stat-card">
            <Statistic title="Tuyến" value={routeCount} prefix={<CarOutlined />} valueStyle={valueStyle} />
          </Card>
        </Col>
        <Col {...colProps}>
          <Card size="small" className="ai-route-stat-card">
            <Statistic title="Đơn" value={totalOrders} prefix={<TeamOutlined />} valueStyle={valueStyle} />
          </Card>
        </Col>
        <Col {...colProps}>
          <Card size="small" className="ai-route-stat-card">
            <Statistic
              title="Tổng km"
              value={totalKm}
              precision={2}
              suffix="km"
              prefix={<NodeIndexOutlined />}
              valueStyle={valueStyle}
            />
          </Card>
        </Col>
        <Col {...colProps}>
          <Card size="small" className="ai-route-stat-card">
            <Statistic
              title="ETA"
              value={formatMinutes(totalEtaMinutes)}
              prefix={<ClockCircleOutlined />}
              valueStyle={valueStyle}
            />
          </Card>
        </Col>
        <Col {...colProps}>
          <Card size="small" className="ai-route-stat-card">
            <Statistic
              title="Chi phí xăng"
              value={formatCurrency(totalFuel)}
              prefix={<DollarOutlined />}
              valueStyle={valueStyle}
            />
          </Card>
        </Col>
        <Col {...colProps}>
          <Card size="small" className="ai-route-stat-card">
            <Statistic
              title="COD"
              value={formatCurrency(totalCod)}
              prefix={<DollarOutlined />}
              valueStyle={valueStyle}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default RouteSummaryCards;
