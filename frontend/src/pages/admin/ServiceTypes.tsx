import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Card,
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
import { CheckCircleOutlined } from "@ant-design/icons";
import serviceTypeApi from "../../api/serviceTypeApi";
import type { AdminServiceType } from "../../types/serviceType";
import "./AdminModal.css";

const { Title } = Typography;

type QueryState = { page: number; limit: number; search: string };

const statusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Tạm ngưng", value: "INACTIVE" },
];

const timeUnitOptions = [
  { label: "Ngày", value: "ngày" },
  { label: "Giờ", value: "giờ" },
];

const AdminServiceTypes: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminServiceType[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AdminServiceType | null>(null);
  const [form] = Form.useForm();
  const [canSubmit, setCanSubmit] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await serviceTypeApi.listAdminServiceTypes({
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

  useEffect(() => {
    if (open) {
      form.validateFields().then(() => setCanSubmit(true)).catch(() => setCanSubmit(false));
    } else {
      setCanSubmit(false);
    }
  }, [open]);

  const onCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const onEdit = (record: AdminServiceType) => {
    setEditing(record);
    const parsed = parseDeliveryTime(record.deliveryTime);
    form.setFieldsValue({
      name: record.name,
      description: record.description,
      status: record.status,
      timeFrom: parsed?.from,
      timeTo: parsed?.to,
      timeUnit: parsed?.unit,
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await serviceTypeApi.deleteAdminServiceType(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      const payload = {
        name: values.name,
        description: values.description,
        status: values.status,
        deliveryTimeFrom: values.timeFrom,
        deliveryTimeTo: values.timeTo,
        deliveryTimeUnit: values.timeUnit,
      };

      if (editing) {
        await serviceTypeApi.updateAdminServiceType(editing.id, payload);
        message.success("Cập nhật thành công");
      } else {
        await serviceTypeApi.createAdminServiceType(payload);
        message.success("Tạo mới thành công");
      }
      setOpen(false);
      fetchData();
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Lưu thất bại");
      }
    } finally {
      setSubmitting(false);
    }
  };

  const columns = useMemo(
    () => [
      { title: "Tên dịch vụ", dataIndex: "name" },
      { title: "Thời gian giao", dataIndex: "deliveryTime" },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (v: string | undefined) => (
          <Tag color={v === "ACTIVE" ? "green" : "default"}>
            {v === "ACTIVE" ? "Hoạt động" : v === "INACTIVE" ? "Tạm ngưng" : v}
          </Tag>
        ),
      },
      { title: "Mô tả", dataIndex: "description" },
      {
        title: "Thao tác",
        render: (_: any, record: AdminServiceType) => (
          <Space>
            <Button size="small" type="primary" onClick={() => onEdit(record)}>
              Sửa
            </Button>
            <Popconfirm title="Xóa loại dịch vụ này?" onConfirm={() => onDelete(record.id)}>
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
          Quản lý loại dịch vụ
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
            <Button type="primary" onClick={onCreate}>
              Thêm loại dịch vụ
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

        <Modal
          title={
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12 }}>
              <CheckCircleOutlined style={{ color: '#1C9CD5', fontSize: 20 }} />
              <span style={{ textAlign: 'center' }}>{editing ? "Cập nhật loại dịch vụ" : "Thêm loại dịch vụ"}</span>
            </div>
          }
          open={open}
          closable={false}
          maskClosable={false}
          centered
          className="admin-user-modal"
          destroyOnClose
          footer={[
            <Button key="cancel" onClick={() => setOpen(false)}>Hủy</Button>,
            <Button key="submit" type="primary" onClick={submit} disabled={!canSubmit || submitting} loading={submitting}>{editing ? "Cập nhật" : "Tạo"}</Button>
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
            <Form.Item name="name" label="Tên dịch vụ" rules={[{ required: true }]}>
              <Input placeholder="Nhập tên dịch vụ" />
            </Form.Item>
            <Form.Item name="description" label="Mô tả">
              <Input.TextArea rows={3} placeholder="Nhập mô tả (tùy chọn)" />
            </Form.Item>
            <Form.Item name="timeFrom" label="Thời gian từ" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: "100%" }} placeholder="Ví dụ: 5" />
            </Form.Item>
            <Form.Item name="timeTo" label="Thời gian đến" rules={[{ required: true }]}>
              <InputNumber min={0} style={{ width: "100%" }} placeholder="Ví dụ: 7" />
            </Form.Item>
            <Form.Item name="timeUnit" label="Đơn vị" rules={[{ required: true }]}>
              <Select placeholder="Chọn đơn vị">
                {timeUnitOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item name="status" label="Trạng thái" initialValue="ACTIVE">
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

function parseDeliveryTime(value?: string) {
  if (!value) return null;
  const match = value.match(/(\d+)\s*-\s*(\d+)\s*(.+)/);
  if (match) {
    return {
      from: Number(match[1]),
      to: Number(match[2]),
      unit: match[3].trim(),
    };
  }
  return null;
}

export default AdminServiceTypes;



