import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message, Typography, Spin, List } from "antd";
import orderApi from "../../api/orderApi";
import officeApi from "../../api/officeApi";
import type { AdminOrder } from "../../types/order";


const { Title } = Typography;

type QueryState = { page: number; limit: number; search: string; status?: string };

const statusOptions = [
  { label: "Bản nháp", value: "DRAFT" },
  { label: "Chờ xử lý", value: "PENDING" },
  { label: "Đã xác nhận", value: "CONFIRMED" },
];

const AdminOrders: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminOrder[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState<any | null>(null);
  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [offices, setOffices] = useState<any[]>([]);
  const [detailLoading, setDetailLoading] = useState(false);
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

  useEffect(() => {
    // preload admin offices for assign dropdown
    (async () => {
      try {
        const res = await officeApi.listAdminOffices({ page: 1, limit: 200 });
        if (res && res.data && Array.isArray(res.data.data)) {
          setOffices(res.data.data);
        }
      } catch (e) {
        // ignore
      }
    })();
  }, []);

  // Prioritize offices by selectedOrder sender city/ward (suggest local offices first)
  const officeOptions = useMemo(() => {
    if (!selectedOrder || !Array.isArray(offices) || offices.length === 0) return offices;
    const sel: any = selectedOrder;
    const senderCity = (sel?.senderCityCode !== undefined && sel?.senderCityCode !== null)
      ? sel.senderCityCode
      : sel.senderAddress?.cityCode;
    const senderWard = (sel?.senderWardCode !== undefined && sel?.senderWardCode !== null)
      ? sel.senderWardCode
      : sel.senderAddress?.wardCode;

    const scored = offices.map((o) => {
      let score = 0;
      if (senderCity && o.cityCode === senderCity) score += 2;
      if (senderWard && o.wardCode === senderWard) score += 1;
      return { office: o, score };
    });

    scored.sort((a, b) => b.score - a.score);
    return scored.map(s => s.office);
  }, [offices, selectedOrder]);

  const onViewDetails = (record: AdminOrder) => {
    setSelectedOrder(null);
    setOpen(true);
    setDetailLoading(true);
    // try to fetch full order detail by admin endpoint
    (async () => {
      try {
        const res = await orderApi.getAdminOrderById(record.id);
        if (res && res.success && res.data) {
          setSelectedOrder(res.data);
        } else {
          setSelectedOrder(record);
        }
      } catch (e: any) {
        setSelectedOrder(record);
      } finally {
        setDetailLoading(false);
      }
    })();
  };

  const onUpdateStatus = useCallback(
    async (record: AdminOrder) => {
      setSelectedOrder(null);
      setDetailLoading(true);
      try {
        const res = await orderApi.getAdminOrderById(record.id);
        const full = res && res.success && res.data ? res.data : record;
        setSelectedOrder(full);

        // Try to fetch local offices matching sender city/ward so admin sees same options as user
        try {
          const senderCity = full.senderCityCode ?? full.senderAddress?.cityCode;
          const senderWard = full.senderWardCode ?? full.senderAddress?.wardCode;
          if (senderCity) {
            const offRes = await officeApi.listLocalOffices({ city: senderCity, ward: senderWard });
            if (offRes && offRes.success && Array.isArray(offRes.data) && offRes.data.length > 0) {
              setOffices(offRes.data);
            }
          }
        } catch (err) {
          // ignore and keep preloaded offices
        }

        form.setFieldsValue({ status: full.status, fromOfficeId: full.fromOffice ? full.fromOffice.id : undefined });
        setStatusModalOpen(true);
      } catch (e: any) {
        setSelectedOrder(record);
        form.setFieldsValue({ status: record.status, fromOfficeId: record.fromOffice ? record.fromOffice.id : undefined });
        setStatusModalOpen(true);
      } finally {
        setDetailLoading(false);
      }
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
        // send optional fromOfficeId when provided
        await orderApi.updateAdminOrderStatus(selectedOrder.id, values);
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
      FAILED_DELIVERY: "red",
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
      FAILED_DELIVERY: "Giao thất bại",
      AT_DEST_OFFICE: "Tại văn phòng đích",
      IN_TRANSIT: "Đang vận chuyển",
      DELIVERED: "Đã giao",
      CANCELLED: "Đã hủy",
    };
    return texts[key] || status;
  };

  const getPaymentText = (paymentStatus?: string, payer?: string) => {
    const statusKey = (paymentStatus || "").toString().toUpperCase();
    const payerKey = (payer || "").toString().toUpperCase();

    const statusMap: Record<string, string> = {
      PAID: "Đã thanh toán",
      UNPAID: "Chưa thanh toán",
      REFUNDED: "Đã hoàn tiền",
    };

    const payerMap: Record<string, string> = {
      CUSTOMER: "Người gửi",
      SHOP: "Cửa hàng",
    };

    if (statusKey && statusMap[statusKey]) return statusMap[statusKey];
    if (payerKey && payerMap[payerKey]) return payerMap[payerKey];
    return paymentStatus || payer || "-";
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
            <Button size="small" onClick={() => onViewDetails(record)} style={{ color: '#1C3D90', borderColor: '#E6F0FF' }}>
              Xem
            </Button>
            <Button type="primary" size="small" onClick={() => onUpdateStatus(record)}>
              Cập nhật
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
          {detailLoading ? (
            <div style={{ textAlign: "center", padding: 40 }}>
              <Spin />
            </div>
          ) : selectedOrder ? (
              <Descriptions column={1} bordered>
                <Descriptions.Item label="Mã vận đơn">{selectedOrder.trackingNumber}</Descriptions.Item>
                <Descriptions.Item label="Trạng thái">
                  <Tag color={getStatusColor(selectedOrder.status)}>{getStatusText(selectedOrder.status)}</Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Bưu cục xuất ">{selectedOrder.fromOffice?.name || '-'}</Descriptions.Item>
                <Descriptions.Item label="Bưu cục nhận ">{selectedOrder.toOffice?.name || '-'}</Descriptions.Item>
                <Descriptions.Item label="Người gửi">{selectedOrder.senderName}</Descriptions.Item>
                <Descriptions.Item label="SĐT người gửi">{selectedOrder.senderPhone || "-"}</Descriptions.Item>
                <Descriptions.Item label="Địa chỉ người gửi">{
                  selectedOrder.senderAddress ? `${selectedOrder.senderAddress.detail || selectedOrder.senderDetail || ""}, ${selectedOrder.senderAddress.wardName || selectedOrder.senderWardCode || ""}, ${selectedOrder.senderAddress.cityName || selectedOrder.senderCityCode || ""}` : (selectedOrder.senderDetail || "-")
                }</Descriptions.Item>
                <Descriptions.Item label="Người nhận">{(selectedOrder.recipientAddress && (selectedOrder.recipientAddress.name || selectedOrder.recipientAddress.name === "" ? selectedOrder.recipientAddress.name : null)) || selectedOrder.recipientName || "-"}</Descriptions.Item>
                <Descriptions.Item label="SĐT người nhận">{(selectedOrder.recipientAddress && (selectedOrder.recipientAddress.phoneNumber || selectedOrder.recipientAddress.phoneNumber === "" ? selectedOrder.recipientAddress.phoneNumber : null)) || selectedOrder.recipientPhone || "-"}</Descriptions.Item>
                <Descriptions.Item label="Địa chỉ người nhận">{
                  selectedOrder.recipientAddress ? `${selectedOrder.recipientAddress.detail || selectedOrder.recipientDetail || ""}, ${selectedOrder.recipientAddress.wardName || selectedOrder.recipientWardCode || ""}, ${selectedOrder.recipientAddress.cityName || selectedOrder.recipientCityCode || ""}` : (selectedOrder.recipientDetail || "-")
                }</Descriptions.Item>
                <Descriptions.Item label="Dịch vụ">{selectedOrder.serviceTypeName || selectedOrder.serviceType?.name || "-"}</Descriptions.Item>
                <Descriptions.Item label="Trọng lượng">{selectedOrder.weight ? `${selectedOrder.weight} kg` : "-"}</Descriptions.Item>
                <Descriptions.Item label="Phí vận chuyển">{
                  selectedOrder.shippingFee != null ? `${selectedOrder.shippingFee.toLocaleString()} VNĐ` : (selectedOrder.totalFee != null ? `${selectedOrder.totalFee.toLocaleString()} VNĐ` : "-")
                }</Descriptions.Item>
                <Descriptions.Item label="COD">{selectedOrder.cod ? `${selectedOrder.cod.toLocaleString()} VNĐ` : "0 VNĐ"}</Descriptions.Item>
                <Descriptions.Item label="Tổng phí">{selectedOrder.totalFee ? `${selectedOrder.totalFee.toLocaleString()} VNĐ` : "0 VNĐ"}</Descriptions.Item>
                <Descriptions.Item label="Thanh toán">{getPaymentText(selectedOrder.paymentStatus, selectedOrder.payer)}</Descriptions.Item>
                <Descriptions.Item label="Sản phẩm">
                  {Array.isArray(selectedOrder.orderProducts) && selectedOrder.orderProducts.length > 0 ? (
                    <List size="small" dataSource={selectedOrder.orderProducts} renderItem={(p: any) => (
                      <List.Item>
                        <div style={{ display: 'flex', justifyContent: 'space-between', width: '100%' }}>
                          <div>{p.name} x{p.quantity}</div>
                          <div>{p.price ? `${p.price.toLocaleString()}đ` : ''}</div>
                        </div>
                      </List.Item>
                    )} />
                  ) : "-"}
                </Descriptions.Item>
                {/* Lịch sử trạng thái removed for admin detail view */}
                <Descriptions.Item label="Ghi chú">{selectedOrder.notes || "-"}</Descriptions.Item>
                <Descriptions.Item label="Ngày tạo">{selectedOrder.createdAt ? new Date(selectedOrder.createdAt).toLocaleString() : "-"}</Descriptions.Item>
              </Descriptions>
          ) : null}
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
            {/* If admin confirms and order is PICKUP_BY_COURIER, allow selecting fromOffice */}
            {selectedOrder && selectedOrder.pickupType === "PICKUP_BY_COURIER" && (
              <Form.Item noStyle shouldUpdate={(prev, cur) => prev.status !== cur.status}>
                {({ getFieldValue }) => {
                  const statusVal = getFieldValue("status");
                  const rules = statusVal === "CONFIRMED" ? [{ required: true, message: "Vui lòng chọn bưu cục lấy hàng" }] : [];
                  return (
                    <Form.Item name="fromOfficeId" label="Chọn bưu cục lấy hàng" rules={rules}>
                      <Select showSearch placeholder="Chọn bưu cục">
                        {officeOptions.map((o) => (
                          <Select.Option key={o.id} value={o.id}>{o.name + (o.cityCode ? ` - TP ${o.cityCode}` : '')}</Select.Option>
                        ))}
                      </Select>
                    </Form.Item>
                  );
                }}
              </Form.Item>
            )}
          </Form>
        </Modal>
      </Card>
    </div>
  );
};

export default AdminOrders;



