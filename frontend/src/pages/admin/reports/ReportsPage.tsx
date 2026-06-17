import React, { useEffect, useMemo, useState } from 'react';
import { Button, Card, Col, DatePicker, Row, Table, Tabs } from 'antd';
import {
  ResponsiveContainer, LineChart, Line, XAxis, YAxis, Tooltip, Legend, BarChart, Bar, CartesianGrid,
} from 'recharts';
import dayjs from 'dayjs';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { reportApi } from '../../../api/reportApi';
import "../../../styles/ListPage.css";

type AnyRecord = Record<string, any>;
const { RangePicker } = DatePicker;

interface KpiCardProps {
  title: string;
  value: string | number;
  color?: string;
  loading?: boolean;
}

const KpiCard: React.FC<KpiCardProps> = ({ title, value, color, loading }) => {
  return (
    <Card
      loading={loading}
      bordered={true}
      style={{
        borderRadius: 8,
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        background: '#fff',
        border: '1px solid #f0f0f0',
        height: '100%',
      }}
      bodyStyle={{ padding: '16px 20px' }}
    >
      <div style={{ fontSize: 13, color: '#8c8c8c', marginBottom: 4, fontWeight: 500 }}>
        {title}
      </div>
      <div style={{ fontSize: 22, fontWeight: 600, color: color || '#111827' }}>
        {value}
      </div>
    </Card>
  );
};

const cleanLocale = {
  emptyText: (
    <div style={{ padding: '30px 0', color: '#999', fontSize: 14, textAlign: 'center' }}>
      Không có dữ liệu
    </div>
  )
};

export default function ReportsPage() {
  const [range, setRange] = useState<any>([dayjs().subtract(7, 'day'), dayjs()]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('overview');
  const [financial, setFinancial] = useState<any[]>([]);
  const [shippers, setShippers] = useState<any[]>([]);
  const [transferred, setTransferred] = useState<any[]>([]);
  const [fees, setFees] = useState<any[]>([]);
  const [operations, setOperations] = useState<any[]>([]);
  const [offices, setOffices] = useState<any[]>([]);
  const [overview, setOverview] = useState<any | null>(null);
  const [financeReport, setFinanceReport] = useState<any | null>(null);

  const exportElementToPdf = async (elementId: string, filename: string) => {
    const el = document.getElementById(elementId);
    if (!el) return;
    const canvas = await html2canvas(el, { scale: 2 });
    const imgData = canvas.toDataURL('image/jpeg', 1.0);
    const pdf = new jsPDF('p', 'mm', 'a4');
    const imgProps = (pdf as any).getImageProperties(imgData);
    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width;
    pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);
    pdf.save(filename);
  };

  const load = async () => {
    setLoading(true);
    try {
      const start = range?.[0]?.format('YYYY-MM-DD');
      const end = range?.[1]?.format('YYYY-MM-DD');

      const [
        resFinancial,
        resShippers,
        resTransferred,
        resFees,
        resOperations,
        resOffices,
        _resShops,
        resOverview,
        resFinance
      ] = await Promise.all([
        reportApi.getFinancial(start, end),
        reportApi.getShippers(start, end),
        reportApi.getTransferred(start, end),
        reportApi.getFees(start, end),
        reportApi.getOperations(start, end),
        reportApi.getOffice(start, end),
        reportApi.getShop(start, end),
        reportApi.getOverview(start, end),
        reportApi.getFinance(start, end)
      ]);

      setFinancial(resFinancial || []);
      setShippers(resShippers || []);
      setTransferred(resTransferred || []);
      setFees(resFees || []);
      setOperations(resOperations || []);
      setOffices(resOffices || []);
      setOverview(resOverview || null);
      setFinanceReport(resFinance || null);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const sumBig = (arr: AnyRecord[], key: string) => arr.reduce((acc, it) => acc + Number(it[key] || 0), 0);
  const totalOrders = operations.reduce((acc, it) => acc + Number(it.totalOrders || 0), 0);
  const deliveredOrders = operations.reduce((acc, it) => acc + Number(it.delivered || 0), 0);
  const deliverySuccessRate = totalOrders > 0 ? (deliveredOrders / totalOrders) * 100 : 0;
  const codDifference = sumBig(financial, 'systemAmount') - sumBig(financial, 'actualAmount');

  const currency = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });

  const sortedOffices = useMemo(
    () => [...offices].sort((a, b) => Number(b.totalOrders || 0) - Number(a.totalOrders || 0)),
    [offices],
  );
  const sortedOfficesByRevenue = useMemo(
    () => [...offices].sort((a, b) => Number(b.shippingRevenue || 0) - Number(a.shippingRevenue || 0)),
    [offices],
  );
  const sortedOfficesBySuccess = useMemo(
    () => [...offices].sort((a, b) => Number((b.successRate || 0)) - Number((a.successRate || 0))),
    [offices],
  );
  const sortedShippers = useMemo(
    () => [...shippers].sort((a, b) => Number(b.totalOrders || b.ordersCount || 0) - Number(a.totalOrders || a.ordersCount || 0)),
    [shippers],
  );
  const sortedShippersBySuccess = useMemo(
    () => [...shippers].sort((a, b) => Number((b.successRate || 0)) - Number((a.successRate || 0))),
    [shippers],
  );
  const sortedShippersByCodHeld = useMemo(
    () => [...shippers].sort((a, b) => Number((b.codHeldByShipper || b.codHeld || 0)) - Number((a.codHeldByShipper || a.codHeld || 0))),
    [shippers],
  );

  const totalCodHeld = useMemo(() => {
    return shippers.reduce((acc, s) => acc + Number(s.codHeldByShipper || s.codHeld || 0), 0);
  }, [shippers]);

  const shipperCols = [
    { title: 'Shipper', dataIndex: 'shipperName', key: 'shipperName', width: 150, render: (_: any, r: AnyRecord) => r.shipperName || r.fullName || r.name || '' },
    { title: 'SĐT', dataIndex: 'phone', key: 'phone', width: 120, render: (_: any, r: AnyRecord) => r.phone || r.phoneNumber || '' },
    { title: 'Bưu cục', dataIndex: 'branchName', key: 'branchName', width: 150, render: (_: any, r: AnyRecord) => r.branchName || r.officeName || '' },
    { title: 'Tổng đơn', dataIndex: 'totalOrders', key: 'totalOrders', width: 90, render: (_: any, r: AnyRecord) => r.totalOrders || r.ordersCount || 0 },
    { title: 'Thành công', dataIndex: 'delivered', key: 'delivered', width: 100, render: (_: any, r: AnyRecord) => r.delivered || 0 },
    { title: 'Thất bại', dataIndex: 'failed', key: 'failed', width: 90, render: (_: any, r: AnyRecord) => r.failed || 0 },
    { title: 'Trả về', dataIndex: 'returnedOrders', key: 'returnedOrders', width: 90, render: (_: any, r: AnyRecord) => r.returnedOrders || r.returned || 0 },
    { title: 'Đang xử lý', dataIndex: 'inProgress', key: 'inProgress', width: 100, render: (_: any, r: AnyRecord) => r.inProgress || 0 },
    { title: 'Tỷ lệ thành công', dataIndex: 'successRate', key: 'successRate', width: 130, render: (v: any, r: AnyRecord) => `${Number(v || r.successRate || 0).toFixed(2)}%` },
    { title: 'COD thu hộ', dataIndex: 'codCollected', key: 'codCollected', width: 140, render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codCollected || r.systemCod || r.systemAmount || 0)) },
    { title: 'COD đã nộp', dataIndex: 'codSubmittedToCompany', key: 'codSubmittedToCompany', width: 140, render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codSubmittedToCompany || r.actualCod || r.totalSubmitted || 0)) },
    { title: 'COD còn giữ', dataIndex: 'codHeldByShipper', key: 'codHeldByShipper', width: 140, render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codHeldByShipper || r.codHeld || 0)) },
  ];

  const OverviewTab = () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Tổng đơn" value={overview ? overview.totalOrders : totalOrders} color="#1C3D90" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Giao thành công" value={overview ? overview.delivered : deliveredOrders} color="#2f9e44" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Thất bại" value={overview ? overview.failed : 0} color="#e03131" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Trả về" value={overview ? overview.returnedOrders : 0} color="#f76707" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Tỷ lệ giao thành công" value={`${overview ? Number(overview.successRate).toFixed(2) : deliverySuccessRate.toFixed(2)}%`} color="#2f9e44" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Doanh thu vận chuyển" value={overview ? currency.format(Number(overview.shippingRevenue || 0)) : currency.format(sumBig(fees, 'systemAmount'))} color="#1C3D90" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="Tổng COD thu hộ" value={overview ? currency.format(Number(overview.totalCodCollected || 0)) : currency.format(sumBig(financial, 'systemAmount'))} color="#111827" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="COD nộp công ty" value={overview ? currency.format(Number(overview.codSubmittedToCompany || 0)) : currency.format(sumBig(transferred, 'systemAmount'))} color="#2f9e44" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="COD đã chuyển shop" value={financeReport ? currency.format(Number((financeReport.codSummary && financeReport.codSummary.codTransferredToShop) || 0)) : currency.format(0)} color="#1C3D90" loading={loading} />
        </Col>
        <Col xs={24} sm={12} md={8} lg={6} xl={4.8}>
          <KpiCard title="COD còn phải đối soát" value={financeReport ? currency.format(Number(((financeReport.codSummary && financeReport.codSummary.codHeldByCompany) || 0))) : currency.format(codDifference)} color="#111827" loading={loading} />
        </Col>
      </Row>

      <div className="list-page-table">
        <Table
          dataSource={operations}
          pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (t) => `Tổng cộng ${t} ngày` }}
          rowKey={(r: AnyRecord) => r.date}
          locale={cleanLocale}
          columns={[
            { title: 'Ngày', dataIndex: 'date', key: 'date', width: 130 },
            { title: 'Tổng đơn', dataIndex: 'totalOrders', key: 'totalOrders', width: 110 },
            { title: 'Giao thành công', dataIndex: 'delivered', key: 'delivered', width: 150 },
            { title: 'Thất bại', dataIndex: 'failed', key: 'failed', width: 110 },
            { title: 'Đang trả', dataIndex: 'returning', key: 'returning', width: 110 },
            { title: 'Đã trả', dataIndex: 'returned', key: 'returned', width: 110 },
            { title: 'Tỷ lệ trả', dataIndex: 'returnRate', key: 'returnRate', width: 120, render: (v: any) => (typeof v === 'number' ? `${(v * 100).toFixed(2)}%` : '0.00%') },
            { title: 'Thời gian trung bình (s)', dataIndex: 'avgDeliverySeconds', key: 'avgDeliverySeconds', width: 180 },
          ]}
        />
      </div>

      <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 15 }}>Vận hành: Giao / Thất bại / Trả về</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
        <div style={{ width: '100%', height: 260 }}>
          <ResponsiveContainer>
            <BarChart data={operations} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Bar dataKey="delivered" name="Giao thành công" fill="#2f9e44" />
              <Bar dataKey="failed" name="Thất bại" fill="#e03131" />
              <Bar dataKey="returned" name="Đã trả" fill="#f76707" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </Card>
    </div>
  );

  const OfficeTab = () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Tổng số bưu cục" value={offices.length} color="#111827" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Tổng đơn" value={sumBig(offices, 'totalOrders')} color="#111827" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Doanh thu vận chuyển" value={currency.format(sumBig(offices, 'shippingRevenue'))} color="#1C3D90" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Tỷ lệ thành công trung bình" value={`${offices.length > 0 ? (sumBig(offices, 'successRate') / offices.length).toFixed(2) : '0.00'}%`} color="#2f9e44" loading={loading} />
        </Col>
      </Row>

      <div className="list-page-table">
        <Table
          dataSource={offices}
          pagination={{ pageSize: 8, showSizeChanger: true, showTotal: (t) => `Tổng cộng ${t} bưu cục` }}
          rowKey={(r: AnyRecord) => r.officeId}
          scroll={{ x: 1600 }}
          tableLayout="fixed"
          locale={cleanLocale}
          columns={[
            { title: 'Bưu cục', dataIndex: 'officeName', key: 'officeName', width: 180 },
            { title: 'Tổng đơn', dataIndex: 'totalOrders', key: 'totalOrders', width: 100 },
            { title: 'Thành công', dataIndex: 'delivered', key: 'delivered', width: 110 },
            { title: 'Thất bại', dataIndex: 'failed', key: 'failed', width: 100 },
            { title: 'Trả về', dataIndex: 'returnedOrders', key: 'returnedOrders', width: 100, render: (_: any, r: AnyRecord) => r.returnedOrders || r.returned || 0 },
            { title: 'Đang xử lý', dataIndex: 'inProgress', key: 'inProgress', width: 110 },
            { title: 'Tỷ lệ thành công', dataIndex: 'successRate', key: 'successRate', width: 150, render: (v: any) => `${Number(v || 0).toFixed(2)}%` },
            { title: 'Doanh thu vận chuyển', dataIndex: 'shippingRevenue', key: 'shippingRevenue', width: 180, render: (v: any, r: AnyRecord) => currency.format(Number(v || r.shippingRevenue || r.shipping_fee || 0)) },
            { title: 'COD thu hộ', dataIndex: 'totalCodCollected', key: 'totalCodCollected', width: 180, render: (v: any, r: AnyRecord) => currency.format(Number(v || r.totalCodCollected || 0)) },
            { title: 'COD nộp công ty', dataIndex: 'codSubmittedToCompany', key: 'codSubmittedToCompany', width: 180, render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codSubmittedToCompany || 0)) },
            { title: 'Nhân viên', dataIndex: 'totalEmployees', key: 'totalEmployees', width: 110 },
            { title: 'Shipper', dataIndex: 'totalShippers', key: 'totalShippers', width: 100 },
          ]}
        />
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 14 }}>Top bưu cục theo số đơn</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
            <Table
              dataSource={sortedOffices.slice(0, 5)}
              pagination={false}
              rowKey={(r: AnyRecord) => r.officeId}
              locale={cleanLocale}
              size="small"
              columns={[
                { title: 'Bưu cục', dataIndex: 'officeName', key: 'officeName' },
                { title: 'Số đơn', dataIndex: 'totalOrders', key: 'totalOrders', align: 'right', width: 90 },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 14 }}>Top bưu cục theo doanh thu</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
            <Table
              dataSource={sortedOfficesByRevenue.slice(0, 5)}
              pagination={false}
              rowKey={(r: AnyRecord) => r.officeId}
              locale={cleanLocale}
              size="small"
              columns={[
                { title: 'Bưu cục', dataIndex: 'officeName', key: 'officeName' },
                { title: 'Doanh thu', dataIndex: 'shippingRevenue', key: 'shippingRevenue', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.shippingRevenue || 0)) },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 14 }}>Top bưu cục theo tỷ lệ thành công</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
            <Table
              dataSource={sortedOfficesBySuccess.slice(0, 5)}
              pagination={false}
              rowKey={(r: AnyRecord) => r.officeId}
              locale={cleanLocale}
              size="small"
              columns={[
                { title: 'Bưu cục', dataIndex: 'officeName', key: 'officeName' },
                { title: 'Tỷ lệ', dataIndex: 'successRate', key: 'successRate', align: 'right', render: (v: any) => `${Number(v || 0).toFixed(2)}%` },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );

  const HumanResourceTab = () => (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Tổng Shipper" value={shippers.length} color="#111827" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Shipper đang hoạt động" value={shippers.filter((s: any) => s.status === 'ACTIVE' || s.isActive !== false).length} color="#2f9e44" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Tổng COD còn giữ" value={currency.format(totalCodHeld)} color="#111827" loading={loading} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <KpiCard title="Tỷ lệ thành công trung bình" value={`${shippers.length > 0 ? (sumBig(shippers, 'successRate') / shippers.length).toFixed(2) : '0.00'}%`} color="#2f9e44" loading={loading} />
        </Col>
      </Row>

      <div className="list-page-table">
        <Table dataSource={shippers} columns={shipperCols} rowKey={(r: AnyRecord) => r.shipperId} pagination={{ pageSize: 8, showSizeChanger: true, showTotal: (t) => `Tổng cộng ${t} shipper` }} scroll={{ x: 1440 }} tableLayout="fixed" locale={cleanLocale} />
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} md={8}>
          <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 14 }}>Top shipper theo số đơn</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
            <Table
              dataSource={sortedShippers.slice(0, 5)}
              pagination={false}
              rowKey={(r: AnyRecord) => r.shipperId}
              locale={cleanLocale}
              size="small"
              columns={[
                { title: 'Shipper', dataIndex: 'shipperName', key: 'shipperName', render: (_: any, r: AnyRecord) => r.shipperName || r.fullName || r.name || '' },
                { title: 'Số đơn', dataIndex: 'totalOrders', key: 'totalOrders', align: 'right', render: (_: any, r: AnyRecord) => r.totalOrders || r.ordersCount || 0 },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 14 }}>Top shipper theo tỷ lệ thành công</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
            <Table
              dataSource={sortedShippersBySuccess.slice(0, 5)}
              pagination={false}
              rowKey={(r: AnyRecord) => r.shipperId}
              locale={cleanLocale}
              size="small"
              columns={[
                { title: 'Shipper', dataIndex: 'shipperName', key: 'shipperName', render: (_: any, r: AnyRecord) => r.shipperName || r.fullName || r.name || '' },
                { title: 'Tỷ lệ', dataIndex: 'successRate', key: 'successRate', align: 'right', render: (v: any) => `${Number(v || 0).toFixed(2)}%` },
              ]}
            />
          </Card>
        </Col>

        <Col xs={24} md={8}>
          <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 14 }}>Top shipper còn giữ COD cao nhất</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
            <Table
              dataSource={sortedShippersByCodHeld.slice(0, 5)}
              pagination={false}
              rowKey={(r: AnyRecord) => r.shipperId}
              locale={cleanLocale}
              size="small"
              columns={[
                { title: 'Shipper', dataIndex: 'shipperName', key: 'shipperName', render: (_: any, r: AnyRecord) => r.shipperName || r.fullName || r.name || '' },
                { title: 'COD còn giữ', dataIndex: 'codHeldByShipper', key: 'codHeldByShipper', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codHeldByShipper || r.codHeld || 0)) },
              ]}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );

  const FinanceTab = () => {
    const revenueByDay = (financeReport && financeReport.revenueByDay) || [];
    const revenueByBranch = (financeReport && financeReport.revenueByBranch) || [];
    const codByDay = (financeReport && financeReport.codByDay) || [];
    const codByBranch = (financeReport && financeReport.codByBranch) || [];
    const codSummary = (financeReport && financeReport.codSummary) || {};

    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={12} lg={4.8} xl={4.8}>
            <KpiCard title="Doanh thu vận chuyển" value={currency.format(Number((financeReport && financeReport.shippingRevenue) || overview?.shippingRevenue || sumBig(fees, 'systemAmount')))} color="#1C3D90" loading={loading} />
          </Col>
          <Col xs={24} sm={12} lg={4.8} xl={4.8}>
            <KpiCard title="COD thu hộ" value={currency.format(Number(codSummary.totalCodCollected || overview?.totalCodCollected || 0))} color="#111827" loading={loading} />
          </Col>
          <Col xs={24} sm={12} lg={4.8} xl={4.8}>
            <KpiCard title="COD nộp công ty" value={currency.format(Number(codSummary.codSubmittedToCompany || overview?.codSubmittedToCompany || 0))} color="#2f9e44" loading={loading} />
          </Col>
          <Col xs={24} sm={12} lg={4.8} xl={4.8}>
            <KpiCard title="COD chuyển shop" value={currency.format(Number(codSummary.codTransferredToShop || 0))} color="#1C3D90" loading={loading} />
          </Col>
          <Col xs={24} sm={12} lg={4.8} xl={4.8}>
            <KpiCard title="COD còn đối soát" value={currency.format(Number(codSummary.codHeldByCompany || codDifference || 0))} color="#111827" loading={loading} />
          </Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 15 }}>Biểu đồ doanh thu theo ngày</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
              <div style={{ width: '100%', height: 260 }}>
                <ResponsiveContainer>
                  <LineChart data={revenueByDay} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                    <XAxis dataKey="date" />
                    <YAxis tickFormatter={(v: any) => currency.format(Number(v)).replace('₫', '')} />
                    <Tooltip formatter={(v: any) => currency.format(Number(v || 0))} />
                    <Legend />
                    <Line type="monotone" dataKey="shippingRevenue" name="Doanh thu" stroke="#1C3D90" dot={false} strokeWidth={2} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={12}>
            <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 15 }}>Biểu đồ COD theo ngày</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
              <div style={{ width: '100%', height: 260 }}>
                <ResponsiveContainer>
                  <LineChart data={codByDay} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                    <XAxis dataKey="date" />
                    <YAxis tickFormatter={(v: any) => currency.format(Number(v)).replace('₫', '')} />
                    <Tooltip formatter={(v: any) => currency.format(Number(v || 0))} />
                    <Legend />
                    <Line type="monotone" dataKey="codCollected" name="COD thu hộ" stroke="#1c7ed6" dot={false} strokeWidth={2} />
                    <Line type="monotone" dataKey="codSubmittedToCompany" name="COD nộp công ty" stroke="#2f9e44" dot={false} strokeWidth={2} />
                    <Line type="monotone" dataKey="codTransferredToShop" name="COD chuyển shop" stroke="#e03131" dot={false} strokeWidth={2} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 15 }}>Doanh thu theo bưu cục</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
              <div className="list-page-table">
                <Table dataSource={revenueByBranch} rowKey={(r: AnyRecord) => r.officeId || r.officeName} pagination={{ pageSize: 6 }} scroll={{ x: 'max-content' }} locale={cleanLocale} columns={[
                  { title: 'Bưu cục', dataIndex: 'officeName', key: 'officeName' },
                  { title: 'Doanh thu', dataIndex: 'shippingRevenue', key: 'shippingRevenue', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.shippingRevenue || 0)) },
                ]} />
              </div>
            </Card>
          </Col>

          <Col xs={24} lg={12}>
            <Card title={<span style={{ color: '#1C3D90', fontWeight: 600, fontSize: 15 }}>COD theo bưu cục</span>} style={{ borderRadius: 8, border: '1px solid #f0f0f0', boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)' }}>
              <div className="list-page-table">
                <Table dataSource={codByBranch} rowKey={(r: AnyRecord) => r.officeId || r.officeName} pagination={{ pageSize: 6 }} scroll={{ x: 'max-content' }} locale={cleanLocale} columns={[
                  { title: 'Bưu cục', dataIndex: 'officeName', key: 'officeName' },
                  { title: 'COD thu hộ', dataIndex: 'codCollected', key: 'codCollected', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codCollected || 0)) },
                  { title: 'COD nộp công ty', dataIndex: 'codSubmittedToCompany', key: 'codSubmittedToCompany', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codSubmittedToCompany || 0)) },
                  { title: 'COD đã chuyển shop', dataIndex: 'codTransferredToShop', key: 'codTransferredToShop', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codTransferredToShop || 0)) },
                  { title: 'COD còn phải đối soát', dataIndex: 'codHeldByCompany', key: 'codHeldByCompany', align: 'right', render: (v: any, r: AnyRecord) => currency.format(Number(v || r.codHeldByCompany || 0)) },
                ]} />
              </div>
            </Card>
          </Col>
        </Row>
      </div>
    );
  };

  const tabItems = [
    { key: 'overview', label: 'Tổng quan', children: <OverviewTab /> },
    { key: 'office', label: 'Bưu cục', children: <OfficeTab /> },
    { key: 'hr', label: 'Nhân sự', children: <HumanResourceTab /> },
    { key: 'finance', label: 'Tài chính', children: <FinanceTab /> },
  ];

  return (
    <div className="list-page-layout reports-page">
      <div className="list-page-content" style={{ padding: '24px 30px' }}>
        
        <div className="list-page-header" style={{ marginBottom: 24 }}>
          <div>
            <h3 className="list-page-title-main" style={{ color: '#1C3D90', fontWeight: 600, margin: 0 }}>Báo cáo</h3>
          </div>
        </div>

        {/* Global Filter Bar */}
        <div className="search-filters-container" style={{ marginBottom: 24 }}>
          <Row justify="space-between" align="middle" gutter={[16, 16]}>
            <Col style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <RangePicker value={range} onChange={(v: any) => v && setRange(v)} className="date-picker" />
              <Button type="primary" onClick={load} loading={loading} className="primary-button" style={{ height: 36 }}>
                Tải báo cáo
              </Button>
            </Col>
            <Col style={{ display: 'flex', gap: 8 }}>
              {activeTab === 'overview' && (
                <Button
                  style={{
                    borderRadius: 8,
                    height: 36,
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    background: '#fff5f5',
                    color: '#e03131',
                    border: '1px solid #ffa8a8'
                  }}
                  onClick={() =>
                    exportElementToPdf(
                      'report-content',
                      `reports_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.pdf`,
                    )
                  }
                >
                  Xuất PDF
                </Button>
              )}
              <Button
                className="primary-button"
                style={{ height: 36, display: 'inline-flex', alignItems: 'center', justifyContent: 'center' }}
                onClick={async () => {
                  try {
                    let blob;
                    let filename;
                    const start = range[0].format('YYYY-MM-DD');
                    const end = range[1].format('YYYY-MM-DD');
                    if (activeTab === 'overview') {
                      blob = await reportApi.exportOverview(start, end);
                      filename = `overview_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                    } else if (activeTab === 'office') {
                      blob = await reportApi.exportOfficesDetailed(start, end);
                      filename = `offices_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                    } else if (activeTab === 'hr') {
                      blob = await reportApi.exportShippers(start, end);
                      filename = `shippers_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                    } else if (activeTab === 'finance') {
                      blob = await reportApi.exportFinance(start, end);
                      filename = `finance_${range[0].format('YYYYMMDD')}_${range[1].format('YYYYMMDD')}.xlsx`;
                    }
                    if (blob) {
                      const url = URL.createObjectURL(blob);
                      const a = document.createElement('a');
                      a.href = url;
                      a.download = filename || 'report.xlsx';
                      document.body.appendChild(a);
                      a.click();
                      a.remove();
                      setTimeout(() => URL.revokeObjectURL(url), 1000);
                    }
                  } catch (err) {
                    console.error(err);
                  }
                }}
              >
                Xuất Excel
              </Button>
            </Col>
          </Row>
        </div>

        <div id="report-content">
          <Tabs defaultActiveKey="overview" activeKey={activeTab} onChange={(k) => setActiveTab(k)} items={tabItems} tabBarStyle={{ marginBottom: 20 }} />
        </div>
      </div>
    </div>
  );
}