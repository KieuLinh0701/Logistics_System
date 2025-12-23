import React, { useEffect, useState } from 'react';
import { Card, DatePicker, Row, Col, Button, Table, Typography } from 'antd';
import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, Legend, BarChart, Bar, CartesianGrid } from 'recharts';
import dayjs from 'dayjs';
import FinancialTable from './FinancialTable';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { reportApi } from '../../../api/reportApi';

const { RangePicker } = DatePicker;
const { Title } = Typography;

async function exportElementToPdf(elementId: string, fileName = 'report.pdf') {
  const el = document.getElementById(elementId);
  if (!el) return;
  const canvas = await html2canvas(el, { scale: 2 });
  const imgData = canvas.toDataURL('image/png');
  const pdf = new jsPDF({ unit: 'pt', format: 'a4' });
  const pdfWidth = pdf.internal.pageSize.getWidth();
  const pdfHeight = pdf.internal.pageSize.getHeight();
  const imgWidth = pdfWidth;
  const imgHeight = (canvas.height * imgWidth) / canvas.width;

  let position = 0;
  pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight);

  if (imgHeight > pdfHeight) {
    let remainingHeight = imgHeight - pdfHeight;
    let offset = pdfHeight;
    while (remainingHeight > 0) {
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 0, -offset, imgWidth, imgHeight);
      remainingHeight -= pdfHeight;
      offset += pdfHeight;
    }
  }

  pdf.save(fileName);
}

export default function ReportsPage() {
  const [range, setRange] = useState<[any, any]>([dayjs().subtract(30, 'day'), dayjs()]);
  const [financial, setFinancial] = useState<any[]>([]);
  const [shippers, setShippers] = useState<any[]>([]);
  const [transferred, setTransferred] = useState<any[]>([]);
  const [fees, setFees] = useState<any[]>([]);
  const [operations, setOperations] = useState<any[]>([]);
  const [offices, setOffices] = useState<any[]>([]);
  const [shops, setShops] = useState<any[]>([]);

  const load = async () => {
    try {
      const start = range[0].format('YYYY-MM-DD');
      const end = range[1].format('YYYY-MM-DD');
      
      const f: any = await reportApi.getFinancial(start, end);
      setFinancial((f || []).map((p: any) => ({ ...p, date: p.date, systemAmount: Number(p.systemAmount || 0), actualAmount: Number(p.actualAmount || 0) })));
      const s: any = await reportApi.getShippers(start, end);
      setShippers(s || []);
      const t: any = await reportApi.getTransferred(start, end);
      setTransferred(t || []);
      const fee: any = await reportApi.getFees(start, end);
      setFees(fee || []);
      const ops: any = await reportApi.getOperations(start, end);
      setOperations((ops || []).map((r: any) => ({
        ...r,
        totalOrders: Number(r.totalOrders || 0),
        delivered: Number(r.delivered || 0),
        failed: Number(r.failed || 0),
        returning: Number(r.returning || 0),
        returned: Number(r.returned || 0),
        avgDeliverySeconds: Number(r.avgDeliverySeconds || 0),
        returnRate: Number(r.returnRate || 0),
      })));
      const of: any = await reportApi.getOffice(start, end);
      setOffices(of || []);
      const sh: any = await reportApi.getShop(start, end);
      setShops(sh || []);
    } catch (e) {
      console.error('[ReportsPage] load error', e);
      try {
        // If axios error, log status and response data for easier debugging
        // @ts-ignore
        if (e && e.response) {
          // @ts-ignore
          console.error('[ReportsPage] response status', e.response.status, e.response.data);
        }
      } catch (ee) { /* ignore */ }
    }
  };

  useEffect(() => { load(); }, []);

  const sumBig = (arr: any[], key: string) => arr.reduce((acc, it) => acc + (Number(it[key] || 0)), 0);

  const shipperCols = [
    { title: 'Tên Shipper', dataIndex: 'fullName', key: 'fullName' },
    { title: 'SĐT', dataIndex: 'phoneNumber', key: 'phoneNumber' },
    { title: 'Số đơn', dataIndex: 'ordersCount', key: 'ordersCount' },
    { title: 'COD hệ thống', dataIndex: 'systemCod', key: 'systemCod', render: (v:any)=>v?.toLocaleString() },
    { title: 'COD thực', dataIndex: 'actualCod', key: 'actualCod', render: (v:any)=>v?.toLocaleString() },
    { title: 'Chênh lệch', key: 'diff', render: (_:any, r:any) => (r.systemCod - r.actualCod).toLocaleString() }
  ];

  return (
    <div style={{ padding: 24, background: '#F9FAFB', borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: '#1C3D90' }}>Báo cáo</Title>
      </div>

      <Card style={{ borderRadius: 12, boxShadow: 'none' }} headStyle={{ background: 'transparent', borderBottom: 'none', paddingTop: 0 }} bodyStyle={{ background: '#FFFFFF', borderRadius: 8, padding: 24 }}>
        <Row gutter={16} style={{ marginBottom: 12, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Col>
            <RangePicker value={range} onChange={(v:any)=>v && setRange(v)} />
          </Col>
          <Col style={{ textAlign: 'right' }}>
            <Button type="primary" onClick={load}>Tải</Button>
            <Button className="admin-pdf-btn" style={{ marginLeft: 8 }} onClick={() => exportElementToPdf('report-content', `reports_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.pdf`)}>
              Xuất PDF
            </Button>
            <Button className="admin-export-btn" style={{ marginLeft: 8 }} onClick={async () => {
              try {
                const blob = await reportApi.exportOperations(range[0].format('YYYY-MM-DD'), range[1].format('YYYY-MM-DD'));
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `operations_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                setTimeout(() => URL.revokeObjectURL(url), 1000);
              } catch (err) { console.error(err); }
            }}>
              Xuất Excel
            </Button>
          </Col>
        </Row>

        <div id="report-content">
          <Row gutter={16} style={{ marginBottom: 12 }}>
            <Col span={8}>
              <Card>
                <div style={{ fontSize: 14 }}>Tổng COD hệ thống</div>
                <div style={{ fontSize: 20, fontWeight: 600 }}>{sumBig(financial, 'systemAmount').toLocaleString()}</div>
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <div style={{ fontSize: 14 }}>Tổng đã chuyển (lô)</div>
                <div style={{ fontSize: 20, fontWeight: 600 }}>{sumBig(transferred, 'systemAmount').toLocaleString()}</div>
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <div style={{ fontSize: 14 }}>Tổng phí vận chuyển</div>
                <div style={{ fontSize: 20, fontWeight: 600 }}>{sumBig(fees, 'systemAmount').toLocaleString()}</div>
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 12 }}>
            <Col span={24}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>Tổng quan vận hành</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }}>
                <Table dataSource={operations} pagination={{ pageSize: 10 }} rowKey={(r:any)=>r.date} columns={[
                  { title: 'Ngày', dataIndex: 'date', key: 'date' },
                  { title: 'Tổng đơn', dataIndex: 'totalOrders', key: 'totalOrders' },
                  { title: 'Giao thành công', dataIndex: 'delivered', key: 'delivered' },
                  { title: 'Thất bại', dataIndex: 'failed', key: 'failed' },
                  { title: 'Đang trả', dataIndex: 'returning', key: 'returning' },
                  { title: 'Đã trả', dataIndex: 'returned', key: 'returned' },
                  { title: 'Tỷ lệ trả', dataIndex: 'returnRate', key: 'returnRate', render:(v:any)=> (typeof v === 'number' ? (v*100).toFixed(2) + '%' : '0%') },
                  { title: 'Thời gian trung bình (s)', dataIndex: 'avgDeliverySeconds', key: 'avgDeliverySeconds' }
                ]} />
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 12 }}>
            <Col span={12}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>COD theo ngày</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }}>
                <div style={{ width: '100%', height: 240 }}>
                  <ResponsiveContainer>
                    <LineChart data={financial} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                      <XAxis dataKey="date" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Line type="monotone" dataKey="systemAmount" name="COD hệ thống" stroke="#8884d8" dot={false} />
                      <Line type="monotone" dataKey="actualAmount" name="COD thực" stroke="#82ca9d" dot={false} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </Card>
            </Col>

            <Col span={12}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>Vận hành: Giao / Thất bại / Trả về</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }}>
                <div style={{ width: '100%', height: 240 }}>
                  <ResponsiveContainer>
                    <BarChart data={operations} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="date" />
                      <YAxis />
                      <Tooltip />
                      <Legend />
                      <Bar dataKey="delivered" name="Giao thành công" fill="#82ca9d" />
                      <Bar dataKey="failed" name="Thất bại" fill="#ff7f7f" />
                      <Bar dataKey="returned" name="Đã trả" fill="#8884d8" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 12 }}>
            <Col span={12}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>Báo cáo chi nhánh</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }} extra={<Button style={{ backgroundColor: '#107C10', borderColor: '#107C10', color: '#fff' }} onClick={async ()=>{
                try{
                  const blob = await reportApi.exportOffice(range[0].format('YYYY-MM-DD'), range[1].format('YYYY-MM-DD'));
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `office_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                  document.body.appendChild(a);
                  a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(url),1000);
                }catch(e){console.error(e)}
              }}>Xuất Excel</Button>}>
                <Table dataSource={offices} pagination={{ pageSize: 6 }} rowKey={(r:any)=>r.officeId} columns={[
                  { title: 'Chi nhánh', dataIndex: 'officeName', key: 'officeName' },
                  { title: 'Tổng đơn', dataIndex: 'totalOrders', key: 'totalOrders' }
                ]} />
              </Card>
            </Col>

            <Col span={12}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>Báo cáo cửa hàng</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }} extra={<Button style={{ backgroundColor: '#107C10', borderColor: '#107C10', color: '#fff' }} onClick={async ()=>{
                try{
                  const blob = await reportApi.exportShop(range[0].format('YYYY-MM-DD'), range[1].format('YYYY-MM-DD'));
                  const url = URL.createObjectURL(blob);
                  const a = document.createElement('a');
                  a.href = url;
                  a.download = `shop_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                  document.body.appendChild(a);
                  a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(url),1000);
                }catch(e){console.error(e)}
              }}>Xuất Excel</Button>}>
                <Table dataSource={shops} pagination={{ pageSize: 6 }} rowKey={(r:any)=>r.shopId} columns={[
                  { title: 'Cửa hàng', dataIndex: 'shopName', key: 'shopName' },
                  { title: 'Đơn', dataIndex: 'ordersCount', key: 'ordersCount' },
                  { title: 'Tổng giá trị', dataIndex: 'totalOrderValue', key: 'totalOrderValue', render:(v:any)=>v?.toLocaleString() },
                  { title: 'Tổng phí vận chuyển', dataIndex: 'totalShippingFee', key: 'totalShippingFee', render:(v:any)=>v?.toLocaleString() }
                ]} />
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 12 }}>
            <Col span={24}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>Tài chính (COD theo ngày)</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }}>
                <FinancialTable data={financial} />
              </Card>
            </Col>
          </Row>

          <Row gutter={16} style={{ marginTop: 12 }}>
            <Col span={24}>
              <Card title={<div style={{ color: '#1C3D90', fontWeight: 600 }}>Tổng hợp Shipper</div>} headStyle={{ background: 'transparent', borderBottom: 'none' }}>
                <Table dataSource={shippers} columns={shipperCols} rowKey={(r:any)=>r.shipperId} pagination={{ pageSize: 6 }} />
              </Card>
            </Col>
          </Row>

        </div>
      </Card>
    </div>
  );
}
