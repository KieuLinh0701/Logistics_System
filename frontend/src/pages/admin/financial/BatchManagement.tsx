import React, { useEffect, useState } from "react";
import { Table, Button, Modal, Input, Space, Select, Pagination } from "antd";
import typedAxios from "../../../api/axiosClient";

const { Option } = Select;

const BatchManagement: React.FC = () => {
  const [batches, setBatches] = useState<any[]>([]);
  const [search, setSearch] = useState<string>("");
  const [status, setStatus] = useState<string | undefined>(undefined);
  const [page, setPage] = useState<number>(1);
  const [limit, setLimit] = useState<number>(10);
  const [total, setTotal] = useState<number>(0);
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [selectedBatch, setSelectedBatch] = useState<any | null>(null);
  const [loadingModal, setLoadingModal] = useState<boolean>(false);

  const fetch = async (p = page, l = limit) => {
    try {
      const res = await typedAxios.get<any>(`/admin/financial/batches?page=${p}&limit=${l}&search=${encodeURIComponent(search || '')}${status ? `&status=${status}` : ''}`);
      const data = res.data || res;
      // handle ListResponse
      if (data && data.list) {
        setBatches(data.list);
        setTotal(data.pagination ? data.pagination.total : 0);
      } else {
        setBatches([]);
        setTotal(0);
      }
    } catch (e) { console.error(e); }
  };

  useEffect(() => { fetch(1, limit); }, [search, status]);

  const complete = async (id: number) => {
    try {
      await typedAxios.post(`/admin/financial/batches/${id}/complete`);
      Modal.success({ title: "Đã hoàn tất" });
      fetch();
    } catch (e) { Modal.error({ title: "Lỗi" }); }
  };

  const viewBatch = async (id: number) => {
    setLoadingModal(true);
    try {
      const res = await typedAxios.get<any>(`/admin/financial/batches/${id}`);
      const payload = res.data || res;
      const batch = payload.data || payload;
      setSelectedBatch(batch);
      setModalVisible(true);
    } catch (e: any) {
      Modal.error({ title: 'Lỗi', content: e?.message || 'Không thể tải batch' });
    } finally {
      setLoadingModal(false);
    }
  };

  const exportCsv = async () => {
    try {
      const res = await typedAxios.get(`/admin/financial/batches/export?search=${encodeURIComponent(search || '')}${status ? `&status=${status}` : ''}`, { responseType: 'arraybuffer' });
      const data = (res as any).data || res;
      const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `batches_export.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (e) { Modal.error({ title: 'Lỗi khi xuất' }); }
  };

  const columns: any = [
    { title: "Mã phiên", dataIndex: "code", key: "code" },
    { title: "Shipper", dataIndex: ["shipper","phoneNumber"], key: "shipper" },
    { title: "Tổng hệ thống", dataIndex: "totalSystemAmount", key: "totalSystemAmount", render: (v: any) => v ? new Intl.NumberFormat('vi-VN').format(v) : 0 },
    { title: "Tổng thực", dataIndex: "totalActualAmount", key: "totalActualAmount", render: (v: any) => v ? new Intl.NumberFormat('vi-VN').format(v) : 0 },
    { title: "Trạng thái", dataIndex: "status", key: "status", render: (v:any) => {
      switch (v) {
        case 'PENDING': return 'Chờ xử lý';
        case 'CHECKING': return 'Đang kiểm';
        case 'COMPLETED': return 'Hoàn tất';
        case 'PARTIAL': return 'Một phần';
        case 'CANCELLED': return 'Đã huỷ';
        default: return v;
      }
    } },
    { title: "Hành động", key: "action", render: (_: any, record: any) => (
      <Space>
        <Button onClick={() => viewBatch(record.id)}>Chi tiết</Button>
        <Button onClick={() => complete(record.id)}>Hoàn tất</Button>
      </Space>
    ) }
  ];

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <Input.Search placeholder="Tìm kiếm" value={search} onChange={e => setSearch(e.target.value)} onSearch={() => fetch(1, limit)} enterButton />
        <Select style={{ width: 200 }} placeholder="Trạng thái" allowClear onChange={(v) => { setStatus(v); fetch(1, limit); }}>
          <Option value="PENDING">Chờ xử lý</Option>
          <Option value="CHECKING">Đang kiểm</Option>
          <Option value="COMPLETED">Hoàn tất</Option>
          <Option value="PARTIAL">Một phần</Option>
          <Option value="CANCELLED">Đã huỷ</Option>
        </Select>
        <Button onClick={() => fetch(1, limit)}>Lọc</Button>
        <Button className="admin-export-btn" onClick={exportCsv}>Xuất Excel</Button>
      </Space>

      <Table rowKey="id" columns={columns} dataSource={batches} pagination={false} />
      <Modal title={selectedBatch ? selectedBatch.code : 'Chi tiết phiên'} open={modalVisible} onCancel={() => setModalVisible(false)} footer={null} width={900}>
        {loadingModal ? <div style={{ textAlign: 'center' }}>Đang tải...</div> : (
          selectedBatch ? (
            <div>
              <p><b>Shipper:</b> {selectedBatch.shipper ? `${selectedBatch.shipper.lastName || ''} ${selectedBatch.shipper.firstName || ''}` : ''}</p>
              <p><b>Created:</b> {selectedBatch.createdAt}</p>
              <p><b>Total System:</b> {selectedBatch.totalSystemAmount ? new Intl.NumberFormat('vi-VN').format(selectedBatch.totalSystemAmount) : 0}</p>
              <p><b>Total Actual:</b> {selectedBatch.totalActualAmount ? new Intl.NumberFormat('vi-VN').format(selectedBatch.totalActualAmount) : 0}</p>
              <Table dataSource={selectedBatch.submissions || []} rowKey="id" pagination={false}>
                <Table.Column title="Mã đối soát" dataIndex="code" key="code" />
                <Table.Column title="Mã vận đơn" dataIndex={["order","trackingNumber"]} key="order" />
                <Table.Column title="Số tiền hệ thống" dataIndex="systemAmount" key="systemAmount" render={(v: any) => new Intl.NumberFormat('vi-VN').format(v)} />
                <Table.Column title="Số tiền thực" dataIndex="actualAmount" key="actualAmount" render={(v: any) => new Intl.NumberFormat('vi-VN').format(v)} />
                <Table.Column title="Trạng thái" dataIndex="status" key="status" />
              </Table>
            </div>
          ) : null
        )}
      </Modal>
      <div style={{ marginTop: 12, textAlign: 'right' }}>
        <Pagination current={page} pageSize={limit} total={total} onChange={(p, ps) => { setPage(p); setLimit(ps); fetch(p, ps); }} />
      </div>
    </div>
  );
};

export default BatchManagement;
