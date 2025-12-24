import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message, Typography } from "antd";
import orderApi from "../../api/orderApi";
import type { AdminOrder } from "../../types/order";


const { Title } = Typography;

type QueryState = { page: number; limit: number; search: string; status?: string };

const statusOptions = [
  { label: "Bản nháp", value: "DRAFT" },
  { label: "Chờ xử lý", value: "PENDING" },
  { label: "Đã xác nhận", value: "CONFIRMED" },
  { label: "Đã lấy hàng", value: "PICKED_UP" },
  { label: "Đang vận chuyển", value: "IN_TRANSIT" },
  { label: "Đã giao", value: "DELIVERED" },
  { label: "Đã hủy", value: "CANCELLED" },
];

const AdminOrders: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminOrder[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<AdminOrder | null>(null);
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await orderApi.listAdminOrders({ 
        page: query.page,
        limit: query.limit,
        search: query.search,
        status: query.status,
      });
      if (!res || !res.success) {
        message.error("Lấy danh sách đơn hàng không thành công");
        setRows([]);
        setTotal(0);
        return;
      }
      if (res.success && res.data) {
        const payload = res.data as any;
        let list: AdminOrder[] = [];
        if (Array.isArray(payload.list)) {
          list = payload.list;
        } else if (Array.isArray(payload.data)) {
          list = payload.data;
        } else if (Array.isArray((payload as any).orders)) {
          list = (payload as any).orders;
        }

        setRows(list);
        setTotal(payload.pagination?.total || 0);
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [query.page, query.limit, query.search, query.status]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const onViewDetails = (record: AdminOrder) => {
    setSelectedOrder(record);
    setOpen(true);
  };

  const onUpdateStatus = useCallback(
    (record: AdminOrder) => {
      setSelectedOrder(record);
      form.setFieldsValue({ status: record.status });
      setStatusModalOpen(true);
    },
    [form]
  );

  const onDelete = useCallback(
    async (id: number) => {
      try {
      await orderApi.deleteAdminOrder(id);
        message.success("Đã xóa");
        fetchData();
      } catch (e: any) {
        message.error(e?.response?.data?.message || "Xóa thất bại");
      }
    },
    [fetchData]
  );

  const submitStatusUpdate = async () => {
    try {
      const values = await form.validateFields();
      if (selectedOrder) {
        await orderApi.updateAdminOrderStatus(selectedOrder.id, values.status);
        message.success("Cập nhật trạng thái thành công");
        setStatusModalOpen(false);
        fetchData();
      }
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Cập nhật thất bại");
      }
    }
  };

  const getStatusColor = (status: string) => {
    const key = (status || "").toString().toUpperCase();
    const colors: Record<string, string> = {
      PENDING: "orange",
      CONFIRMED: "blue",
      PICKED_UP: "cyan",
      READY_FOR_PICKUP: "cyan",
      DELIVERING: "purple",
      AT_DEST_OFFICE: "gold",
      IN_TRANSIT: "purple",
      DELIVERED: "green",
      CANCELLED: "red",
      DRAFT: "default",
    };
    return colors[key] || "default";
  };

  const getStatusText = (status: string) => {
    const key = (status || "").toString().toUpperCase();
    const texts: Record<string, string> = {
      DRAFT: "Nháp",
      PENDING: "Chờ xử lý",
      CONFIRMED: "Đã xác nhận",
      PICKED_UP: "Đã lấy hàng",
      READY_FOR_PICKUP: "Sẵn sàng lấy hàng",
      DELIVERING: "Đang giao",
      AT_DEST_OFFICE: "Tại văn phòng đích",
      IN_TRANSIT: "Đang vận chuyển",
      DELIVERED: "Đã giao",
      CANCELLED: "Đã hủy",
    };
    return texts[key] || status;
  };

  const columns = useMemo(
    () => [
      { title: "Mã vận đơn", dataIndex: "trackingNumber", render: (v: string) => v || "-" },
      { title: "Người gửi", dataIndex: "senderName", render: (v: string) => v || "-" },
      { title: "Người nhận", dataIndex: "recipientName", render: (v: string) => v || "-" },
      {
        title: "Tổng tiền",
        dataIndex: "totalFee",
        render: (v: number | undefined) => (v != null ? `${v.toLocaleString()} VNĐ` : "-"),
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string | undefined) => (v ? <Tag color={getStatusColor(v)}>{getStatusText(v)}</Tag> : "-"),
      },
      {
        title: "Thao tác",
        render: (_: any, record: AdminOrder) => (
          <Space>
            <Button size="small" onClick={() => onViewDetails(record)}>
              Xem
            </Button>
            <Button size="small" onClick={() => onUpdateStatus(record)}>
              Sửa trạng thái
            </Button>
            <Popconfirm title="Xóa đơn hàng này?" onConfirm={() => onDelete(record.id)}>
              <Button size="small" danger>
                Xóa
              </Button>
            </Popconfirm>
          </Space>
        ),
      },
    ],
    [onDelete, onUpdateStatus]
  );

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>
          Quản lý đơn hàng
        </Title>
      </div>

      <Card
        style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}
        extra={
          <Space>
            <Input.Search
              allowClear
              placeholder="Tìm kiếm"
              onSearch={(v) => setQuery({ ...query, page: 1, search: v })}
            />
            <Select
              placeholder="Lọc theo trạng thái"
              allowClear
              style={{ width: 150 }}
              value={query.status}
              onChange={(v) => setQuery({ ...query, page: 1, status: v })}
            >
              {statusOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
          </Space>
        }
      >
        <Table
          rowKey="id"
          loading={loading}
          columns={columns as any}
          dataSource={rows}
          pagination={{
            current: query.page,
            pageSize: query.limit,
            total,
            onChange: (p, ps) => setQuery({ ...query, page: p, limit: ps }),
          }}
        />

        <Drawer title="Chi tiết đơn hàng" placement="right" width={600} open={open} onClose={() => setOpen(false)}>
          {selectedOrder && (
            <Descriptions column={1} bordered>
              <Descriptions.Item label="Mã vận đơn">{selectedOrder.trackingNumber}</Descriptions.Item>
              <Descriptions.Item label="Người gửi">{selectedOrder.senderName}</Descriptions.Item>
              <Descriptions.Item label="Người nhận">{selectedOrder.recipientName}</Descriptions.Item>
              <Descriptions.Item label="Tổng tiền">
                {selectedOrder.totalFee?.toLocaleString() || "0"} VNĐ
              </Descriptions.Item>
              <Descriptions.Item label="Trạng thái">
                <Tag color={getStatusColor(selectedOrder.status)}>{getStatusText(selectedOrder.status)}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Ngày tạo">
                {new Date(selectedOrder.createdAt).toLocaleString()}
              </Descriptions.Item>
            </Descriptions>
          )}
        </Drawer>

        <Modal
          title="Cập nhật trạng thái đơn hàng"
          open={statusModalOpen}
          onOk={submitStatusUpdate}
          onCancel={() => setStatusModalOpen(false)}
        >
          <Form form={form} layout="vertical">
            <Form.Item name="status" label="Trạng thái" rules={[{ required: true }]}>
              <Select>
                {statusOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </Form>
        </Modal>
      </Card>
    </div>
  );
};

export default AdminOrders;



