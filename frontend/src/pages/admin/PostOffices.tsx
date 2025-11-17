import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message,
  Typography,
} from "antd";
import adminApi from "../../api/adminApi";
import type { CreateOfficePayload, Office } from "../../api/adminApi";


const { Title } = Typography;

type QueryState = { page: number; limit: number; search: string };

const officeTypeOptions = [
  { label: "Trụ sở chính", value: "HEAD_OFFICE" },
  { label: "Bưu cục", value: "POST_OFFICE" },
];

const officeStatusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Tạm ngừng", value: "INACTIVE" },
  { label: "Bảo trì", value: "MAINTENANCE" },
];

const AdminPostOffices: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Office[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOffice, setEditingOffice] = useState<Office | null>(null);
  const [form] = Form.useForm();

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await adminApi.listOffices({
        page: query.page,
        limit: query.limit,
        search: query.search,
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
  }, [query.page, query.limit, query.search]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const onViewDetails = (record: Office) => {
    setSelectedOffice(record);
    setOpen(true);
  };

  const onAdd = () => {
    setEditingOffice(null);
    form.resetFields();
    setModalOpen(true);
  };

  const onEdit = (record: Office) => {
    setEditingOffice(record);
    form.setFieldsValue({
      code: record.code,
      postalCode: record.postalCode,
      name: record.name,
      latitude: record.latitude,
      longitude: record.longitude,
      email: record.email,
      phoneNumber: record.phoneNumber,
      openingTime: record.openingTime,
      closingTime: record.closingTime,
      type: record.type,
      status: record.status,
      capacity: record.capacity,
      notes: record.notes,
      wardCode: record.address?.wardCode,
      cityCode: record.address?.cityCode,
      detailAddress: record.address?.detail,
    });
    setModalOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await adminApi.deleteOffice(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const normalizePayload = (values: any): CreateOfficePayload => {
    return {
      code: values.code,
      postalCode: values.postalCode,
      name: values.name,
      latitude: Number(values.latitude),
      longitude: Number(values.longitude),
      email: values.email,
      phoneNumber: values.phoneNumber,
      openingTime: values.openingTime,
      closingTime: values.closingTime,
      type: values.type,
      status: values.status,
      capacity: values.capacity != null ? Number(values.capacity) : undefined,
      notes: values.notes,
      wardCode: Number(values.wardCode),
      cityCode: Number(values.cityCode),
      detailAddress: values.detailAddress,
    };
  };

  const submitForm = async () => {
    try {
      const values = await form.validateFields();
      const payload = normalizePayload(values);
      if (editingOffice) {
        await adminApi.updateOffice(editingOffice.id, payload);
        message.success("Cập nhật bưu cục thành công");
      } else {
        await adminApi.createOffice(payload);
        message.success("Thêm bưu cục thành công");
      }
      setModalOpen(false);
      fetchData();
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Thao tác thất bại");
      }
    }
  };

  const formatAddress = (office: Office) => {
    if (!office.address) return "Chưa có";
    const { detail, wardCode, cityCode } = office.address;
    return `${detail || ""}${wardCode ? ` - Phường ${wardCode}` : ""}${cityCode ? ` - TP ${cityCode}` : ""}`;
  };

  const columns = useMemo(
    () => [
      { title: "Mã", dataIndex: "code" },
      { title: "Tên bưu cục", dataIndex: "name" },
      { title: "Số điện thoại", dataIndex: "phoneNumber" },
      {
        title: "Loại",
        dataIndex: "type",
        render: (v: string) => {
          const label =
            officeTypeOptions.find((item) => item.value === v)?.label || v || "N/A";
          return <Tag color="blue">{label}</Tag>;
        },
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string) => {
          const label =
            officeStatusOptions.find((item) => item.value === v)?.label || v || "N/A";
          const color = v === "ACTIVE" ? "green" : v === "MAINTENANCE" ? "orange" : "default";
          return <Tag color={color}>{label}</Tag>;
        },
      },
      {
        title: "Địa chỉ",
        render: (_: any, record: Office) => formatAddress(record),
      },
      {
        title: "Thao tác",
        render: (_: any, record: Office) => (
          <Space>
            <Button size="small" onClick={() => onViewDetails(record)}>
              Xem
            </Button>
            <Button size="small" onClick={() => onEdit(record)}>
              Sửa
            </Button>
            <Popconfirm title="Xóa bưu cục này?" onConfirm={() => onDelete(record.id)}>
              <Button size="small" danger>
                Xóa
              </Button>
            </Popconfirm>
          </Space>
        ),
      },
    ],
    []
  );

  return (
    <div style={{ padding: 24, background: "#F9FAFB", borderRadius: 12 }}>
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ color: "#1C3D90" }}>
          Quản lý bưu cục
        </Title>
      </div>

      <Card
        style={{ borderRadius: 12, boxShadow: "0 2px 8px rgba(0,0,0,0.1)" }}
        extra={
          <Space>
            <Input.Search
              allowClear
              placeholder="Tìm kiếm theo tên/mã"
              onSearch={(v) => setQuery({ ...query, page: 1, search: v })}
            />
            <Button type="primary" onClick={onAdd}>
              Thêm bưu cục
            </Button>
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

      <Drawer
        title="Chi tiết bưu cục"
        placement="right"
        width={600}
        open={open}
        onClose={() => setOpen(false)}
      >
        {selectedOffice && (
          <Descriptions column={1} bordered>
            <Descriptions.Item label="Mã">{selectedOffice.code}</Descriptions.Item>
            <Descriptions.Item label="Tên">{selectedOffice.name}</Descriptions.Item>
            <Descriptions.Item label="Email">{selectedOffice.email || "N/A"}</Descriptions.Item>
            <Descriptions.Item label="SĐT">{selectedOffice.phoneNumber || "N/A"}</Descriptions.Item>
            <Descriptions.Item label="Loại">
              {officeTypeOptions.find((item) => item.value === selectedOffice.type)?.label ||
                selectedOffice.type ||
                "N/A"}
            </Descriptions.Item>
            <Descriptions.Item label="Trạng thái">
              <Tag color={selectedOffice.status === "ACTIVE" ? "green" : "default"}>
                {officeStatusOptions.find((item) => item.value === selectedOffice.status)?.label ||
                  selectedOffice.status ||
                  "N/A"}
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Địa chỉ">{formatAddress(selectedOffice)}</Descriptions.Item>
            <Descriptions.Item label="Tọa độ">
              {selectedOffice.latitude}, {selectedOffice.longitude}
            </Descriptions.Item>
            <Descriptions.Item label="Giờ hoạt động">
              {selectedOffice.openingTime} - {selectedOffice.closingTime}
            </Descriptions.Item>
            <Descriptions.Item label="Sức chứa">{selectedOffice.capacity || "N/A"}</Descriptions.Item>
            {selectedOffice.notes && (
              <Descriptions.Item label="Ghi chú">{selectedOffice.notes}</Descriptions.Item>
            )}
          </Descriptions>
        )}
      </Drawer>

      <Modal
        title={editingOffice ? "Cập nhật bưu cục" : "Thêm bưu cục"}
        open={modalOpen}
        onOk={submitForm}
        onCancel={() => setModalOpen(false)}
        width={720}
      >
        <Form layout="vertical" form={form}>
          {!editingOffice && (
            <Form.Item name="code" label="Mã bưu cục" rules={[{ required: true }]}>
              <Input placeholder="Nhập mã" />
            </Form.Item>
          )}
          <Form.Item name="postalCode" label="Mã bưu chính">
            <Input placeholder="Ví dụ: 700000" />
          </Form.Item>
          <Form.Item name="name" label="Tên bưu cục" rules={[{ required: true }]}>
            <Input placeholder="Nhập tên" />
          </Form.Item>
          <Form.Item name="email" label="Email">
            <Input type="email" placeholder="contact@example.com" />
          </Form.Item>
          <Form.Item name="phoneNumber" label="Số điện thoại" rules={[{ required: true }]}>
            <Input placeholder="Nhập số điện thoại" />
          </Form.Item>
          <Form.Item name="type" label="Loại bưu cục" rules={[{ required: true }]}>
            <Select placeholder="Chọn loại bưu cục">
              {officeTypeOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="status" label="Trạng thái" initialValue="ACTIVE">
            <Select placeholder="Chọn trạng thái">
              {officeStatusOptions.map((option) => (
                <Select.Option key={option.value} value={option.value}>
                  {option.label}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="latitude" label="Vĩ độ" rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} placeholder="Ví dụ: 10.762622" />
          </Form.Item>
          <Form.Item name="longitude" label="Kinh độ" rules={[{ required: true }]}>
            <InputNumber style={{ width: "100%" }} placeholder="Ví dụ: 106.660172" />
          </Form.Item>
          <Form.Item name="openingTime" label="Giờ mở cửa">
            <Input placeholder="07:00:00" />
          </Form.Item>
          <Form.Item name="closingTime" label="Giờ đóng cửa">
            <Input placeholder="17:00:00" />
          </Form.Item>
          <Form.Item name="capacity" label="Sức chứa">
            <InputNumber min={0} style={{ width: "100%" }} placeholder="Nhập sức chứa" />
          </Form.Item>
          <Form.Item name="notes" label="Ghi chú">
            <Input.TextArea rows={3} placeholder="Nhập ghi chú (tùy chọn)" />
          </Form.Item>
          <Form.Item name="wardCode" label="Mã phường/xã" rules={[{ required: true }]}>
            <InputNumber min={0} style={{ width: "100%" }} placeholder="Nhập mã phường/xã" />
          </Form.Item>
          <Form.Item name="cityCode" label="Mã tỉnh/thành phố" rules={[{ required: true }]}>
            <InputNumber min={0} style={{ width: "100%" }} placeholder="Nhập mã tỉnh/thành phố" />
          </Form.Item>
          <Form.Item name="detailAddress" label="Địa chỉ chi tiết" rules={[{ required: true }]}>
            <Input.TextArea rows={2} placeholder="Số nhà, đường..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminPostOffices;

