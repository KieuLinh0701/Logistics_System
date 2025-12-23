import React, { useEffect, useState } from "react";
import { Table, Button, InputNumber, Input, Modal, Space, Spin } from "antd";
import typedAxios from "../../../api/axiosClient";
import type { AdminPaymentSubmissionListDto } from "../../../types/paymentSubmission";

const SubmissionReview: React.FC = () => {
  const [data, setData] = useState<AdminPaymentSubmissionListDto[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [actual, setActual] = useState<number | undefined>(undefined);
  const [note, setNote] = useState<string>("");
  const [loading, setLoading] = useState<boolean>(false);

  const fetch = async () => {
    setLoading(true);
    try {
      const res = await typedAxios.get<any>("/admin/financial/submissions?status=PENDING");
      const payload = res.data || res;
      setData(payload.list || payload.data || payload);
    } catch (e) {
      console.error(e);
      Modal.error({ title: 'Lỗi', content: 'Không thể tải danh sách nộp tiền' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetch(); }, []);

  const handleConfirm = async (id: number) => {
    try {
      await typedAxios.put(`/admin/financial/submissions/${id}`, { status: "MATCHED", actualAmount: actual, notes: note });
      Modal.success({ title: "Đã xác nhận" });
      fetch();
    } catch (e) { Modal.error({ title: "Lỗi" }); }
  };

  const exportCsv = async () => {
    try {
      const res = await typedAxios.get(`/admin/financial/submissions/export`, { responseType: 'arraybuffer' });
      const data = res.data || res;
      const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `submissions_export.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (e) {
      console.error(e);
      Modal.error({ title: 'Lỗi', content: 'Không thể xuất file' });
    }
  };

  const translateStatus = (s?: string) => {
    if (!s) return '';
    switch (s) {
      case 'PENDING': return 'Chờ xử lý';
      case 'MATCHED': return 'Đã đối soát';
      case 'MISMATCHED': return 'Sai lệch';
      case 'IN_BATCH': return 'Trong batch';
      case 'COMPLETED': return 'Hoàn tất';
      case 'CHECKING': return 'Đang kiểm';
      case 'CANCELLED': return 'Đã huỷ';
      case 'PARTIAL': return 'Một phần';
      default: return s;
    }
  };

  const columns = [
    { title: "Mã", dataIndex: "code", key: "code" },
    { title: "Mã vận đơn", dataIndex: ["order","trackingNumber"], key: "order" },
    { title: "Số tiền hệ thống", dataIndex: "systemAmount", key: "systemAmount", render: (v: any) => v ? new Intl.NumberFormat('vi-VN').format(v) : 0 },
    { title: "Số tiền thực", dataIndex: "actualAmount", key: "actualAmount", render: (v: any) => v ? new Intl.NumberFormat('vi-VN').format(v) : 0 },
    { title: "Trạng thái", dataIndex: "status", key: "status", render: (v: any) => translateStatus(v) },
    { title: "Hành động", key: "action", render: (_: any, record: any) => (
      <Space>
        <Button onClick={() => { setSelected(record.id); setActual(record.actualAmount); setNote(record.notes || ""); }}>Xử lý</Button>
        <Button danger onClick={() => handleConfirm(record.id)}>Xác nhận</Button>
      </Space>
    ) }
  ];

  return (
    <div>
        <div style={{ marginBottom: 12 }}>
          <Button className="admin-export-btn" onClick={exportCsv} style={{ float: 'right' }}>Xuất Excel</Button>
        </div>
      <Spin spinning={loading}>
        <Table rowKey="id" columns={columns} dataSource={data} />
      </Spin>

      <Modal open={selected !== null} onCancel={() => setSelected(null)} onOk={() => { if (selected) handleConfirm(selected); setSelected(null); }} title="Xử lý nộp tiền">
        <div>
          <div>Số tiền thực:</div>
          <InputNumber value={actual} onChange={(v) => setActual(v as number)} style={{ width: '100%' }} />
          <div style={{ marginTop: 8 }}>Ghi chú:</div>
          <Input.TextArea value={note} onChange={(e) => setNote(e.target.value)} />
        </div>
      </Modal>
    </div>
  );
};

export default SubmissionReview;
