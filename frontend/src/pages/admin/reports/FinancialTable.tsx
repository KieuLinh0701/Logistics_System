import { Table } from 'antd';

type Point = {
  date: string; // ISO
  systemAmount: number;
  actualAmount: number;
};

export default function FinancialTable({ data }: { data: Point[] }) {
  const columns = [
    { title: 'Date', dataIndex: 'date', key: 'date' },
    { title: 'System COD', dataIndex: 'systemAmount', key: 'systemAmount', render: (v: number) => v?.toLocaleString() },
    { title: 'Actual COD', dataIndex: 'actualAmount', key: 'actualAmount', render: (v: number) => v?.toLocaleString() },
    { title: 'Diff', key: 'diff', render: (_: any, r: Point) => (r.systemAmount - r.actualAmount).toLocaleString() }
  ];

  return <Table rowKey={(r: any) => r.date} dataSource={data} columns={columns} pagination={false} />;
}
