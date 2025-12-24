import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message, Typography } from "antd";
import { CheckCircleOutlined } from "@ant-design/icons";
import vehicleApi from "../../api/vehicleApi";
import axiosClient from "../../api/axiosClient";
import type { AdminVehicle } from "../../types/vehicle";
import "./AdminModal.css";

const { Title } = Typography;

type QueryState = { page: number; limit: number; search: string; type?: string; status?: string };

const typeOptions = [
  { label: "Xe tải", value: "TRUCK" },
  { label: "Xe van", value: "VAN" },
  { label: "Container", value: "CONTAINER" },
];

const statusOptions = [
  { label: "Sẵn sàng", value: "AVAILABLE" },
  { label: "Đang sử dụng", value: "IN_USE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
  { label: "Lưu trữ", value: "ARCHIVED" },
];

const AdminVehicles: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminVehicle[]>([]);
  const [total, setTotal] = useState(0);
  const [offices, setOffices] = useState<{ id: number; name: string }[]>([]);
  const [trackingsModalOpen, setTrackingsModalOpen] = useState(false);
  const [trackingPoints, setTrackingPoints] = useState<Array<{ id: number; latitude: number; longitude: number; recordedAt: string }>>([]);
  const [trackingLoading, setTrackingLoading] = useState(false);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<AdminVehicle | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState<AdminVehicle | null>(null);
  const [form] = Form.useForm();
  const [canSubmit, setCanSubmit] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await vehicleApi.listAdminVehicles({
        page: query.page,
        limit: query.limit,
        search: query.search,
        type: query.type,
        status: query.status,
      });
      if (res.success && res.data) {
        setRows(res.data.data || []);
        setTotal(res.data.pagination?.total || 0);
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  }, [query.page, query.limit, query.search, query.type, query.status]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    const fetchOffices = async () => {
      try {
        const res = await axiosClient.get<any>("/admin/offices");
        if (res && typeof res === 'object' && 'success' in res) {
          const inner = res.data;
          if (inner && Array.isArray(inner.data)) setOffices(inner.data);
          else if (Array.isArray(inner)) setOffices(inner);
          else setOffices([]);
        } else if (Array.isArray(res)) setOffices(res);
        else setOffices(res?.data || []);
      } catch (e) {
        setOffices([]);
      }
    };
    fetchOffices();
  }, []);

  useEffect(() => {
    if (modalOpen) {
      form.validateFields().then(() => setCanSubmit(true)).catch(() => setCanSubmit(false));
    } else {
      setCanSubmit(false);
    }
  }, [modalOpen]);

  const onViewDetails = (record: AdminVehicle) => {
    setSelectedVehicle(record);
    setOpen(true);
  };

  const fetchTrackings = async (vehicleId: number) => {
    try {
      setTrackingLoading(true);
      const res = await axiosClient.get<any>(`/admin/vehicles/${vehicleId}/trackings`);
      let list: any[] = [];
      if (res && typeof res === 'object' && 'success' in res) {
        list = res.data?.data || [];
      } else if (res && res.data) {
        list = res.data?.data || res.data || [];
      } else if (Array.isArray(res)) {
        list = res;
      }
      const normalized = (list || []).map((t: any) => ({
        id: t.id,
        latitude: Number(t.latitude),
        longitude: Number(t.longitude),
        recordedAt: t.recordedAt || t.recorded_at || t.recordedAt,
      }));
      setTrackingPoints(normalized);
    } catch (e) {
      setTrackingPoints([]);
    } finally {
      setTrackingLoading(false);
    }
  };

  const onEdit = useCallback(
    (record: AdminVehicle) => {
      setEditingVehicle(record);
      form.setFieldsValue({
        type: record.type,
        capacity: record.capacity,
        status: record.status,
        description: record.description,
        officeId: record.office?.id || record.officeId,
        gpsDeviceId: (record as any).gpsDeviceId,
        nextMaintenanceDue: (record as any).nextMaintenanceDue,
      });
      setModalOpen(true);
    },
    [form]
  );

  const onAdd = useCallback(() => {
    setEditingVehicle(null);
    form.resetFields();
    setModalOpen(true);
  }, [form]);

  const onDelete = useCallback(
    async (id: number) => {
      try {
      await vehicleApi.deleteAdminVehicle(id);
        message.success("Đã xóa");
        fetchData();
      } catch (e: any) {
        message.error(e?.response?.data?.message || "Xóa thất bại");
      }
    },
    [fetchData]
  );

  const submitForm = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      if (editingVehicle) {
        await vehicleApi.updateAdminVehicle(editingVehicle.id, values);
        message.success("Cập nhật phương tiện thành công");
      } else {
        await vehicleApi.createAdminVehicle({
          licensePlate: values.licensePlate,
          type: values.type,
          capacity: values.capacity,
          status: values.status,
          description: values.description,
          officeId: values.officeId,
        });
        message.success("Thêm phương tiện thành công");
      }
      setModalOpen(false);
      fetchData();
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Thao tác thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      AVAILABLE: "green",
      IN_USE: "blue",
      MAINTENANCE: "orange",
      ARCHIVED: "default",
    };
    return colors[status] || "default";
  };

  const getStatusText = (status: string) => {
    const texts: Record<string, string> = {
      AVAILABLE: "Sẵn sàng",
      IN_USE: "Đang sử dụng",
      MAINTENANCE: "Bảo trì",
      ARCHIVED: "Lưu trữ",
    };
    return texts[status] || status;
  };

  const getTypeText = (type: string) => {
    const texts: Record<string, string> = {
      TRUCK: "Xe tải",
      VAN: "Xe van",
      CONTAINER: "Container",
    };
    return texts[type] || type;
  };

  const columns = useMemo(
    () => [
      { title: "Biển số xe", dataIndex: "licensePlate", render: (v: string) => v || "-" },
      {
        title: "Loại xe",
        dataIndex: "type",
        render: (v: string) => getTypeText(v) || "-",
      },
      {
        title: "Tải trọng",
        dataIndex: "capacity",
        render: (v: number | undefined) => (v != null ? `${v} kg` : "-"),
      },
      {
        title: "Bưu cục",
        dataIndex: ["office", "name"],
        render: (_: any, record: AdminVehicle) => record?.office?.name || "Chưa phân công",
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string | undefined) => (v ? <Tag color={getStatusColor(v)}>{getStatusText(v)}</Tag> : "-"),
      },
      {
        title: "Thao tác",
        render: (_: any, record: AdminVehicle) => (
          <Space>
            <Button size="small" onClick={() => onViewDetails(record)} style={{ color: '#1C3D90', borderColor: '#E6F0FF' }}>
              Xem
            </Button>
            <Button size="small" type="primary" onClick={() => onEdit(record)}>
              Sửa
            </Button>
            <Popconfirm title="Xóa phương tiện này?" onConfirm={() => onDelete(record.id)}>
              <Button size="small" danger>
                Xóa
              </Button>
            </Popconfirm>
          </Space>
        ),
      },
    ],
    [onDelete, onEdit]
  );

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>
          Quản lý phương tiện
        </Title>
      </div>

      <Card
        style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}
        extra={
          <Space>
            <Button type="primary" onClick={onAdd}>
              Thêm phương tiện
            </Button>
            <Input.Search
              allowClear
              placeholder="Tìm kiếm"
              onSearch={(v) => setQuery({ ...query, page: 1, search: v })}
            />
            <Select
              placeholder="Loại xe"
              allowClear
              style={{ width: 120 }}
              value={query.type}
              onChange={(v) => setQuery({ ...query, page: 1, type: v })}
            >
              {typeOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
            <Select
              placeholder="Trạng thái"
              allowClear
              style={{ width: 120 }}
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
      </Card>

      <Drawer title="Chi tiết phương tiện" placement="right" width={600} open={open} onClose={() => setOpen(false)}>
        {selectedVehicle && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="Biển số xe">{selectedVehicle.licensePlate}</Descriptions.Item>
            <Descriptions.Item label="Loại xe">{getTypeText(selectedVehicle.type)}</Descriptions.Item>
            <Descriptions.Item label="Tải trọng">{selectedVehicle.capacity} kg</Descriptions.Item>
            <Descriptions.Item label="Bưu cục">{selectedVehicle.office?.name || "Chưa phân công"}</Descriptions.Item>
            {selectedVehicle.gpsDeviceId && (
              <Descriptions.Item label="Thiết bị GPS">{selectedVehicle.gpsDeviceId}</Descriptions.Item>
            )}
            {(selectedVehicle.lastMaintenanceAt || selectedVehicle.nextMaintenanceDue) && (
              <Descriptions.Item label="Bảo trì">
                <div>
                  {selectedVehicle.lastMaintenanceAt && <div>Lần trước: {new Date(selectedVehicle.lastMaintenanceAt).toLocaleString()}</div>}
                  {selectedVehicle.nextMaintenanceDue && <div>Lần tiếp theo: {new Date(selectedVehicle.nextMaintenanceDue).toLocaleString()}</div>}
                </div>
              </Descriptions.Item>
            )}
            {(selectedVehicle.latitude != null || selectedVehicle.longitude != null) && (
              <Descriptions.Item label="Toạ độ">{`${selectedVehicle.latitude ?? '-'} , ${selectedVehicle.longitude ?? '-'}`}</Descriptions.Item>
            )}

            <Descriptions.Item label="Hành động">
              <Space>
                <Button size="small" onClick={() => {
                  if (selectedVehicle) {
                    fetchTrackings(selectedVehicle.id);
                    setTrackingsModalOpen(true);
                  }
                }}>
                  Xem hành trình
                </Button>
              </Space>
            </Descriptions.Item>

            {/* history removed per request */}
            <Descriptions.Item label="Trạng thái">
              <Tag color={getStatusColor(selectedVehicle.status)}>{getStatusText(selectedVehicle.status)}</Tag>
            </Descriptions.Item>
            {selectedVehicle.description && (
              <Descriptions.Item label="Mô tả">{selectedVehicle.description}</Descriptions.Item>
            )}
            <Descriptions.Item label="Ngày tạo">
              {new Date(selectedVehicle.createdAt).toLocaleString()}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Drawer>

      <Modal
        title="Hành trình xe"
        open={trackingsModalOpen}
        onCancel={() => setTrackingsModalOpen(false)}
        footer={null}
        width={800}
      >
        {trackingLoading ? (
          <div>Đang tải...</div>
        ) : trackingPoints.length === 0 ? (
          <div>Không có dữ liệu hành trình</div>
        ) : (
          <div>
            <div style={{ maxHeight: 420, overflow: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr style={{ textAlign: 'left', borderBottom: '1px solid #e5e7eb' }}>
                    <th style={{ padding: 8 }}>Thời gian</th>
                    <th style={{ padding: 8 }}>Toạ độ</th>
                  </tr>
                </thead>
                <tbody>
                  {trackingPoints.map(p => (
                    <tr key={p.id} style={{ borderBottom: '1px solid #f3f4f6' }}>
                      <td style={{ padding: 8 }}>{new Date(p.recordedAt).toLocaleString()}</td>
                      <td style={{ padding: 8 }}>{p.latitude}, {p.longitude} <a style={{ marginLeft: 8 }} href={`https://www.google.com/maps/search/?api=1&query=${p.latitude},${p.longitude}`} target="_blank" rel="noreferrer">Mở bản đồ</a></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </Modal>

      <Modal
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
            <CheckCircleOutlined style={{ color: '#1C9CD5', fontSize: 20 }} />
            <span style={{ textAlign: 'center' }}>{editingVehicle ? "Cập nhật phương tiện" : "Thêm phương tiện mới"}</span>
          </div>
        }
        open={modalOpen}
        closable={false}
        maskClosable={false}
        centered
        className="admin-user-modal"
        width={600}
        footer={[
          <Button key="cancel" onClick={() => setModalOpen(false)}>Hủy</Button>,
          <Button key="submit" type="primary" onClick={submitForm} disabled={!canSubmit || submitting} loading={submitting}>{editingVehicle ? "Cập nhật" : "Tạo"}</Button>
        ]}
      >
        <Form form={form} layout="vertical" onValuesChange={async () => {
          try {
            await form.validateFields();
            setCanSubmit(true);
          } catch {
            setCanSubmit(false);
          }
        }}>
          {!editingVehicle && (
            <Form.Item
              name="licensePlate"
              label="Biển số xe"
              rules={[{ required: true, message: "Vui lòng nhập biển số xe" }]}
            >
              <Input placeholder="Nhập biển số xe" />
            </Form.Item>
          )}
          <Form.Item name="type" label="Loại xe" rules={[{ required: true, message: "Vui lòng chọn loại xe" }]}>
            <Select placeholder="Chọn loại xe">
              {typeOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="capacity"
            label="Tải trọng (kg)"
            rules={[{ required: true, message: "Vui lòng nhập tải trọng" }]}
          >
            <Input type="number" placeholder="Nhập tải trọng" min={0} />
          </Form.Item>
          <Form.Item name="officeId" label="Bưu cục">
            <Select
              showSearch
              placeholder="Chọn bưu cục"
              optionFilterProp="children"
              allowClear
            >
              {offices.map((o) => (
                <Select.Option key={o.id} value={o.id}>{o.name}</Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" rules={[{ required: true, message: "Vui lòng chọn trạng thái" }]}>
            <Select placeholder="Chọn trạng thái">
              {statusOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          {editingVehicle && (
            <>
              <Form.Item name="gpsDeviceId" label="Thiết bị GPS">
                <Input placeholder="ID thiết bị GPS" disabled />
              </Form.Item>
              <Form.Item name="nextMaintenanceDue" label="Ngày bảo trì tiếp theo">
                <Input placeholder="Ngày bảo trì tiếp theo" disabled />
              </Form.Item>
            </>
          )}
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea placeholder="Nhập mô tả (tùy chọn)" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminVehicles;



