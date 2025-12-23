import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Card, Descriptions, Drawer, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message, Typography } from "antd";
import vehicleApi from "../../api/vehicleApi";
import type { AdminVehicle } from "../../types/vehicle";

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
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState<AdminVehicle | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingVehicle, setEditingVehicle] = useState<AdminVehicle | null>(null);
  const [form] = Form.useForm();

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

  const onViewDetails = (record: AdminVehicle) => {
    setSelectedVehicle(record);
    setOpen(true);
  };

  const onEdit = useCallback(
    (record: AdminVehicle) => {
      setEditingVehicle(record);
      form.setFieldsValue({
        type: record.type,
        capacity: record.capacity,
        status: record.status,
        description: record.description,
        officeId: record.officeId,
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
            <Button size="small" onClick={() => onViewDetails(record)}>
              Xem
            </Button>
            <Button size="small" onClick={() => onEdit(record)}>
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
        title={editingVehicle ? "Cập nhật phương tiện" : "Thêm phương tiện mới"}
        open={modalOpen}
        onOk={submitForm}
        onCancel={() => setModalOpen(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
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
            <Input type="number" placeholder="Nhập ID bưu cục" />
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
          <Form.Item name="description" label="Mô tả">
            <Input.TextArea placeholder="Nhập mô tả (tùy chọn)" rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminVehicles;



