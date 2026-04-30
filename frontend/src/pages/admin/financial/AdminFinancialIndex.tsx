import React, { useState } from "react";
import { Button, DatePicker, Space, Tabs } from "antd";
import { ReloadOutlined } from "@ant-design/icons";
import CODPending from "./CODPending";
import SubmissionReview from "./SubmissionReview";
import BatchManagement from "./BatchManagement";
import RecruitmentFilterPanel from "../../hr/recruitment/components/RecruitmentFilterPanel";
import "../../../styles/ListPage.css";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "./FinancialPage.css";

const { RangePicker } = DatePicker;

const AdminFinancialIndex: React.FC = () => {
  const [range, setRange] = useState<any>(null);
  const [refreshSeed, setRefreshSeed] = useState(0);

  return (
    <div className="list-page-layout financial-page">
      <div className="list-page-content">
        <RecruitmentFilterPanel>
          <div className="financial-toolbar">
            <RangePicker value={range} onChange={(value) => setRange(value)} />
            <Space>
              <Button icon={<ReloadOutlined />} onClick={() => setRefreshSeed((seed) => seed + 1)}>
                Làm mới
              </Button>
            </Space>
          </div>
        </RecruitmentFilterPanel>

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <h3 className="list-page-title-main">Quản lý dòng tiền</h3>
            <div style={{ marginTop: 12 }}>
              <div className="list-page-tag">Kết quả trả về: 3 nhóm đối soát</div>
            </div>
          </div>
        </div>

        <div className="list-page-table">
          <Tabs
            defaultActiveKey="pending"
            destroyInactiveTabPane
            items={[
              { key: "pending", label: "COD chưa nộp", children: <CODPending key={`pending-${refreshSeed}`} /> },
              {
                key: "submissions",
                label: "Kiểm tra nộp tiền",
                children: <SubmissionReview key={`submission-${refreshSeed}`} />,
              },
              {
                key: "batches",
                label: "Batch đối soát",
                children: <BatchManagement key={`batch-${refreshSeed}`} />,
              },
            ]}
          />
        </div>
      </div>
    </div>
  );
};

export default AdminFinancialIndex;
