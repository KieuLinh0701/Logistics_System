import React, { useEffect, useState } from "react";
import { Card, Table, Button, Collapse } from "antd";
import typedAxios from "../../../api/axiosClient";
import type { AdminPaymentSubmissionListDto } from "../../../types/paymentSubmission";

const { Panel } = Collapse;

const CODPending: React.FC = () => {
  const [grouped, setGrouped] = useState<Record<string, AdminPaymentSubmissionListDto[]>>({});

  const fetch = async () => {
    try {
      const res = await typedAxios.get<any>(`/admin/financial/pending`);
      const data = res.data || res;
      // data is map keyed by shipper id
      setGrouped(data);
    } catch (e) { console.error(e); }
  };

  useEffect(() => { fetch(); }, []);

  return (
    <Card title="COD chưa nộp">
      <Collapse>
        {Object.entries(grouped).map(([shipperId, list]) => (
          <Panel header={`Shipper ${shipperId} — ${list.length} đơn`} key={shipperId}>
            <Table dataSource={list} rowKey="id" pagination={false}>
              <Table.Column title="Mã đối soát" dataIndex="code" key="code" />
              <Table.Column title="Mã vận đơn" dataIndex={["order","trackingNumber"]} key="order" />
              <Table.Column title="Số tiền hệ thống" dataIndex="systemAmount" key="systemAmount" render={(v:any)=> v ? new Intl.NumberFormat('vi-VN').format(v) : 0} />
              <Table.Column title="Số tiền thực" dataIndex="actualAmount" key="actualAmount" render={(v:any)=> v ? new Intl.NumberFormat('vi-VN').format(v) : 0} />
              <Table.Column title="Trạng thái" dataIndex="status" key="status" render={(v:any)=> {
                switch(v){
                  case 'PENDING': return 'Chờ xử lý';
                  case 'MATCHED': return 'Đã đối soát';
                  case 'MISMATCHED': return 'Sai lệch';
                  case 'IN_BATCH': return 'Trong batch';
                  default: return v;
                }
              }} />
              <Table.Column title="Hành động" key="action" render={(text: any, record: any) => (
                <Button type="link" href={`/financial/submissions`}>Xử lý</Button>
              )} />
            </Table>
          </Panel>
        ))}
      </Collapse>
    </Card>
  );
};

export default CODPending;
