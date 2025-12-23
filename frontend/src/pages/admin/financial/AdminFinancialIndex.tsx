import React, { useState } from "react";
import { Card, Tabs, Row, Col, Typography, DatePicker, Button } from "antd";
import CODPending from "./CODPending";
import SubmissionReview from "./SubmissionReview";
import BatchManagement from "./BatchManagement";

const { Title } = Typography;
const { RangePicker } = DatePicker;

const AdminFinancialIndex: React.FC = () => {
  const [range, setRange] = useState<any>(null);

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>Quản lý dòng tiền</Title>
      </div>

      <Card style={{ borderRadius: 12, boxShadow: 'none' }} headStyle={{ background: 'transparent', borderBottom: 'none', paddingTop: 0 }} bodyStyle={{ background: '#FFFFFF', borderRadius: 8, padding: 24 }}>
        <Row justify="space-between" style={{ marginBottom: 12 }}>
          <Col>
            <RangePicker value={range} onChange={(v:any) => setRange(v)} />
          </Col>
          <Col style={{ textAlign: 'right' }}>
            <Button type="primary" onClick={() => window.location.reload()}>Tải</Button>
          </Col>
        </Row>

        <Tabs defaultActiveKey="pending" destroyInactiveTabPane>
          <Tabs.TabPane tab="COD chưa nộp" key="pending">
            <CODPending />
          </Tabs.TabPane>
          <Tabs.TabPane tab="Kiểm tra nộp tiền" key="submissions">
            <SubmissionReview />
          </Tabs.TabPane>
          <Tabs.TabPane tab="Batch đối soát" key="batches">
            <BatchManagement />
          </Tabs.TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default AdminFinancialIndex;
