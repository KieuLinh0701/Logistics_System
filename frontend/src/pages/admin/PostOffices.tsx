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
  Row,
  Col,
  Typography,
} from "antd";
import { CheckCircleOutlined } from "@ant-design/icons";
import officeApi from "../../api/officeApi";
import locationApi from "../../api/locationApi";
import { formatAddress as formatAddressUtil } from "../../utils/locationUtils";
import type { AdminOffice, CreateOfficePayload } from "../../types/office";
import "./AdminModal.css";


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
  const [rows, setRows] = useState<AdminOffice[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [selectedOffice, setSelectedOffice] = useState<AdminOffice | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingOffice, setEditingOffice] = useState<AdminOffice | null>(null);
  const [form] = Form.useForm();
  const [canSubmit, setCanSubmit] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [cities, setCities] = useState<Array<{ code: number; name: string }>>([]);
  const [wards, setWards] = useState<Array<{ code: number; name: string }>>([]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await officeApi.listAdminOffices({
        page: query.page,
        limit: query.limit,
        search: query.search,
      });
      if (res.success && res.data) {
        const items: AdminOffice[] = res.data.data || [];
        // compute human-readable address for each office
        const mapped = await Promise.all(
          items.map(async (it) => {
            try {
              const displayAddress = await formatAddressUtil(it.detail || "", Number(it.wardCode) || 0, Number(it.cityCode) || 0);
              return { ...it, displayAddress } as AdminOffice & { displayAddress?: string };
            } catch {
              return { ...it, displayAddress: (it.detail ? it.detail + " - " : "") + (it.wardCode ? `Phường ${it.wardCode}` : "") + (it.cityCode ? ` - TP ${it.cityCode}` : "") } as AdminOffice & { displayAddress?: string };
            }
          })
        );
        setRows(mapped as any);
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

  useEffect(() => {
    if (modalOpen) {
      form.validateFields().then(() => setCanSubmit(true)).catch(() => setCanSubmit(false));
      // fetch city list when modal opens
      locationApi.getCities().then((cs) => setCities((cs || []).map((c: any) => ({ code: c.code, name: c.name })))).catch(() => setCities([]));
    } else {
      setCanSubmit(false);
    }
  }, [modalOpen]);

  const onViewDetails = (record: AdminOffice) => {
    setSelectedOffice(record);
    setOpen(true);
  };

  const onAdd = () => {
    setEditingOffice(null);
    form.resetFields();
    setModalOpen(true);
    setCanSubmit(false);
  };

  const onEdit = (record: AdminOffice) => {
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
      wardCode: record.wardCode,
      cityCode: record.cityCode,
      detailAddress: record.detail,
    });
    // load wards for the office's city so the ward select shows correct options
    if (record.cityCode) {
      locationApi.getWardsByCity(Number(record.cityCode)).then((ws) => {
        setWards((ws || []).map((w: any) => ({ code: w.code, name: w.name })));
      }).catch(() => setWards([]));
    }
    setModalOpen(true);
    // validate current values so button state updates when editing
    setTimeout(() => {
      form.validateFields().then(() => setCanSubmit(true)).catch(() => setCanSubmit(false));
    }, 0);
  };

  const onDelete = async (id: number) => {
    try {
      await officeApi.deleteAdminOffice(id);
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
      setSubmitting(true);
      const values = await form.validateFields();
      const payload = normalizePayload(values);
      if (editingOffice) {
        await officeApi.updateAdminOffice(editingOffice.id, payload);
        message.success("Cập nhật bưu cục thành công");
      } else {
        await officeApi.createAdminOffice(payload);
        message.success("Thêm bưu cục thành công");
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

  const formatAddress = (office: AdminOffice) => {
    if (!office.detail && !office.wardCode && !office.cityCode) return "Chưa có";
    const parts = [];
    if (office.detail) parts.push(office.detail);
    if (office.wardCode) parts.push(`Phường ${office.wardCode}`);
    if (office.cityCode) parts.push(`TP ${office.cityCode}`);
    return parts.join(" - ");
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
        render: (_: any, record: any) => record.displayAddress || "Chưa có",
      },
      {
        title: "Thao tác",
        render: (_: any, record: AdminOffice) => (
          <Space>
            <Button size="small" onClick={() => onViewDetails(record)} style={{ color: '#1C3D90', borderColor: '#E6F0FF' }}>
              Xem
            </Button>
            <Button size="small" type="primary" onClick={() => onEdit(record)}>
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
            <Descriptions.Item label="Địa chỉ">{(selectedOffice as any)?.displayAddress || formatAddress(selectedOffice)}</Descriptions.Item>
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
        title={
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
            <CheckCircleOutlined style={{ color: '#1C9CD5', fontSize: 20 }} />
            <span style={{ textAlign: 'center' }}>{editingOffice ? "Cập nhật bưu cục" : "Thêm bưu cục"}</span>
          </div>
        }
        open={modalOpen}
        closable={false}
        maskClosable={false}
        centered
        className="admin-user-modal"
        width={720}
        footer={
          [
            <Button key="cancel" onClick={() => setModalOpen(false)}>
              Hủy
            </Button>,
            <Button key="submit" type="primary" onClick={submitForm} loading={submitting}>
              {editingOffice ? "Cập nhật" : "Tạo"}
            </Button>,
          ]
        }
      >
        <Form
          layout="vertical"
          form={form}
          onValuesChange={async () => {
            try {
              await form.validateFields();
              setCanSubmit(true);
            } catch {
              setCanSubmit(false);
            }
          }}
        >
          {/* Row 1: Mã | Tên | Loại */}
          <Row gutter={[16, 12]}>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="code" label="Mã bưu cục" rules={[{ required: !editingOffice }]}>
                <Input placeholder="Nhập mã" disabled={!!editingOffice} />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="name" label="Tên bưu cục" rules={[{ required: true }]}>
                <Input placeholder="Nhập tên" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="type" label="Loại bưu cục" rules={[{ required: true }]}>
                <Select placeholder="Chọn loại bưu cục">
                  {officeTypeOptions.map((option) => (
                    <Select.Option key={option.value} value={option.value}>
                      {option.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          {/* Row 2: SĐT | Email | Trạng thái */}
          <Row gutter={[16, 12]}>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="phoneNumber" label="Số điện thoại" rules={[{ required: true }]}>
                <Input placeholder="Nhập số điện thoại" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="email" label="Email">
                <Input type="email" placeholder="contact@gmail.com" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="status" label="Trạng thái" initialValue="ACTIVE">
                <Select placeholder="Chọn trạng thái">
                  {officeStatusOptions.map((option) => (
                    <Select.Option key={option.value} value={option.value}>
                      {option.label}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          {/* Row 3: Giờ mở cửa | Giờ đóng cửa | Sức chứa */}
          <Row gutter={[16, 12]}>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="openingTime" label="Giờ mở cửa">
                <Input placeholder="07:00:00" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="closingTime" label="Giờ đóng cửa">
                <Input placeholder="17:00:00" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="capacity" label="Sức chứa">
                <InputNumber min={0} style={{ width: "100%" }} placeholder="Nhập sức chứa" />
              </Form.Item>
            </Col>
          </Row>

          {/* Row 4: Phường/xã | Tỉnh/Thành phố | Mã bưu chính (selects show names but values are codes) */}
          <Row gutter={[16, 12]}>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="wardCode" label="Phường/ xã" rules={[{ required: true }]}>
                <Select
                  showSearch
                  placeholder="Chọn phường/xã"
                  optionFilterProp="children"
                  onSearch={() => {}}
                  filterOption={(input, option) => (option?.children as unknown as string).toLowerCase().includes(input.toLowerCase())}
                >
                  {wards.map((w) => (
                    <Select.Option key={w.code} value={w.code}>
                      {w.name}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="cityCode" label="Tỉnh/Thành phố" rules={[{ required: true }]}>
                <Select
                  showSearch
                  placeholder="Chọn tỉnh/thành"
                  optionFilterProp="children"
                  onChange={(val: any) => {
                    // when city changes, load wards
                    if (val) {
                      locationApi.getWardsByCity(Number(val)).then((ws) => setWards((ws || []).map((w: any) => ({ code: w.code, name: w.name })))).catch(() => setWards([]));
                      form.setFieldsValue({ wardCode: undefined });
                    } else {
                      setWards([]);
                    }
                  }}
                  filterOption={(input, option) => (option?.children as unknown as string).toLowerCase().includes(input.toLowerCase())}
                >
                  {cities.map((c) => (
                    <Select.Option key={c.code} value={c.code}>
                      {c.name}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={8}>
              <Form.Item name="postalCode" label="Mã bưu chính">
                <Input placeholder="Ví dụ: 700000" />
              </Form.Item>
            </Col>
          </Row>

          {/* Row 5: Địa chỉ chi tiết (full width) */}
          <Row>
            <Col xs={24}>
              <Form.Item name="detailAddress" label="Địa chỉ chi tiết" rules={[{ required: true }]}>
                <Input.TextArea rows={2} placeholder="Số nhà, đường..." />
              </Form.Item>
            </Col>
          </Row>

          {/* Row 6: Vĩ độ | Kinh độ */}
          <Row gutter={[16, 12]}>
            <Col xs={24} sm={12} md={12}>
              <Form.Item name="latitude" label="Vĩ độ" rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} placeholder="Ví dụ: 10.762622" />
              </Form.Item>
            </Col>
            <Col xs={24} sm={12} md={12}>
              <Form.Item name="longitude" label="Kinh độ" rules={[{ required: true }]}>
                <InputNumber style={{ width: "100%" }} placeholder="Ví dụ: 106.660172" />
              </Form.Item>
            </Col>
          </Row>

          {/* Row 7: Ghi chú (full width) */}
          <Row>
            <Col xs={24}>
              <Form.Item name="notes" label="Ghi chú">
                <Input.TextArea rows={3} placeholder="Nhập ghi chú (tùy chọn)" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default AdminPostOffices;

