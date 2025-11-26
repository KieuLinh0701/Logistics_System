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
  Switch,
} from "antd";
import feeConfigurationApi from "../../api/feeConfigurationApi";
import serviceTypeApi from "../../api/serviceTypeApi";
import type { FeeConfiguration, CreateFeeConfigurationPayload } from "../../types/feeConfiguration";
import type { AdminServiceType } from "../../types/serviceType";

const { Title } = Typography;
const { TextArea } = Input;

type QueryState = { page: number; limit: number; search: string; feeType?: string; serviceTypeId?: number; active?: boolean };

const feeTypeOptions = [
  { label: "Phí thu hộ (COD)", value: "COD" },
  { label: "Phí đóng gói", value: "PACKAGING" },
  { label: "Phí bảo hiểm", value: "INSURANCE" },
  { label: "Thuế VAT", value: "VAT" },
];

const calculationTypeOptions = [
  { label: "Cố định", value: "FIXED" },
  { label: "Phần trăm (%)", value: "PERCENTAGE" },
];

const AdminFeeConfigurations: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<FeeConfiguration[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<FeeConfiguration | null>(null);
  const [form] = Form.useForm();
  const [serviceTypes, setServiceTypes] = useState<AdminServiceType[]>([]);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await feeConfigurationApi.listAdminFeeConfigurations({
        page: query.page,
        limit: query.limit,
        search: query.search,
        feeType: query.feeType,
        serviceTypeId: query.serviceTypeId,
        active: query.active,
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
  }, [query.page, query.limit, query.search, query.feeType, query.serviceTypeId, query.active]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  useEffect(() => {
    const loadServiceTypes = async () => {
      try {
        const res = await serviceTypeApi.listAdminServiceTypes({ page: 1, limit: 100 });
        if (res.success && res.data) {
          setServiceTypes(res.data.data || []);
        }
      } catch (e) {
        console.error("Failed to load service types", e);
      }
    };
    loadServiceTypes();
  }, []);

  const onCreate = () => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      active: true,
      calculationType: "PERCENTAGE",
    });
    setOpen(true);
  };

  const onEdit = (record: FeeConfiguration) => {
    setEditing(record);
    form.setFieldsValue({
      serviceTypeId: record.serviceTypeId,
      feeType: record.feeType,
      calculationType: record.calculationType,
      feeValue: record.feeValue,
      minOrderFee: record.minOrderFee,
      maxOrderFee: record.maxOrderFee,
      active: record.active,
      notes: record.notes,
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await feeConfigurationApi.deleteAdminFeeConfiguration(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      console.error("[AdminFeeConfigurations] Failed to delete", e);
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submit = async () => {
    try {
      const values = await form.validateFields();
      const payload: CreateFeeConfigurationPayload = {
        serviceTypeId: values.serviceTypeId,
        feeType: values.feeType,
        calculationType: values.calculationType,
        feeValue: values.feeValue,
        minOrderFee: values.minOrderFee,
        maxOrderFee: values.maxOrderFee,
        active: values.active,
        notes: values.notes,
      };

      if (editing) {
        await feeConfigurationApi.updateAdminFeeConfiguration(editing.id, payload);
        message.success("Cập nhật thành công");
      } else {
        await feeConfigurationApi.createAdminFeeConfiguration(payload);
        message.success("Tạo mới thành công");
      }
      setOpen(false);
      fetchData();
    } catch (e: any) {
      if (!e?.errorFields) {
        message.error(e?.response?.data?.message || "Lưu thất bại");
      }
    }
  };

  const getFeeTypeText = (feeType: string) => {
    const opt = feeTypeOptions.find((o) => o.value === feeType);
    return opt?.label || feeType;
  };

  const columns = useMemo(
    () => [
      {
        title: "Loại phí",
        dataIndex: "feeType",
        render: (feeType: string) => <Tag color="blue">{getFeeTypeText(feeType)}</Tag>,
      },
      {
        title: "Loại dịch vụ",
        dataIndex: "serviceTypeName",
        render: (name: string) => name || "Tất cả",
      },
      {
        title: "Cách tính",
        dataIndex: "calculationType",
        render: (type: string) => (
          <Tag color={type === "PERCENTAGE" ? "green" : "orange"}>
            {type === "PERCENTAGE" ? "Phần trăm" : "Cố định"}
          </Tag>
        ),
      },
      {
        title: "Giá trị",
        key: "value",
        render: (_: any, record: FeeConfiguration) => (
          <span>
            {record.calculationType === "PERCENTAGE"
              ? `${record.feeValue}%`
              : `${record.feeValue.toLocaleString("vi-VN")}đ`}
          </span>
        ),
      },
      {
        title: "Áp dụng từ",
        dataIndex: "minOrderFee",
        render: (val: number) => (val ? `${val.toLocaleString("vi-VN")}đ` : "-"),
      },
      {
        title: "Áp dụng đến",
        dataIndex: "maxOrderFee",
        render: (val: number) => (val ? `${val.toLocaleString("vi-VN")}đ` : "-"),
      },
      {
        title: "Trạng thái",
        dataIndex: "active",
        render: (active: boolean) => (
          <Tag color={active ? "green" : "red"}>{active ? "Hoạt động" : "Tạm dừng"}</Tag>
        ),
      },
      {
        title: "Thao tác",
        render: (_: any, record: FeeConfiguration) => (
          <Space>
            <Button size="small" onClick={() => onEdit(record)}>
              Sửa
            </Button>
            <Popconfirm title="Xóa cấu hình này?" onConfirm={() => onDelete(record.id)}>
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
          Quản lý cấu hình phí
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
              style={{ width: 200 }}
            />
            <Select
              placeholder="Loại phí"
              allowClear
              style={{ width: 150 }}
              value={query.feeType}
              onChange={(v) => setQuery({ ...query, page: 1, feeType: v })}
            >
              {feeTypeOptions.map((opt) => (
                <Select.Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Select.Option>
              ))}
            </Select>
            <Select
              placeholder="Trạng thái"
              allowClear
              style={{ width: 120 }}
              value={query.active}
              onChange={(v) => setQuery({ ...query, page: 1, active: v })}
            >
              <Select.Option value={true}>Hoạt động</Select.Option>
              <Select.Option value={false}>Tạm dừng</Select.Option>
            </Select>
            <Button type="primary" onClick={onCreate}>
              Thêm cấu hình
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
          title={editing ? "Cập nhật cấu hình phí" : "Thêm cấu hình phí"}
          open={open}
          onOk={submit}
          onCancel={() => setOpen(false)}
          destroyOnClose
          width={700}
        >
          <Form form={form} layout="vertical">
            <Form.Item name="serviceTypeId" label="Loại dịch vụ (để trống = áp dụng tất cả)">
              <Select placeholder="Chọn loại dịch vụ" allowClear>
                {serviceTypes.map((st) => (
                  <Select.Option key={st.id} value={st.id}>
                    {st.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              name="feeType"
              label="Loại phí"
              rules={[{ required: true, message: "Vui lòng chọn loại phí" }]}
            >
              <Select>
                {feeTypeOptions.map((opt) => (
                  <Select.Option key={opt.value} value={opt.value}>
                    {opt.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              name="calculationType"
              label="Cách tính"
              rules={[{ required: true, message: "Vui lòng chọn cách tính" }]}
            >
              <Select>
                {calculationTypeOptions.map((opt) => (
                  <Select.Option key={opt.value} value={opt.value}>
                    {opt.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
            <Form.Item
              name="feeValue"
              label="Giá trị phí"
              rules={[{ required: true, message: "Vui lòng nhập giá trị" }]}
            >
              <InputNumber style={{ width: "100%" }} min={0} placeholder="VD: 5 (5% hoặc 5,000đ)" />
            </Form.Item>
            <Form.Item name="minOrderFee" label="Áp dụng từ (đ)">
              <InputNumber style={{ width: "100%" }} min={0} placeholder="Không giới hạn" />
            </Form.Item>
            <Form.Item name="maxOrderFee" label="Áp dụng đến (đ)">
              <InputNumber style={{ width: "100%" }} min={0} placeholder="Không giới hạn" />
            </Form.Item>
            <Form.Item name="active" label="Hoạt động" valuePropName="checked" initialValue={true}>
              <Switch />
            </Form.Item>
            <Form.Item name="notes" label="Ghi chú">
              <TextArea rows={3} placeholder="Ghi chú (tùy chọn)" />
            </Form.Item>
          </Form>
        </Modal>
      </Card>
    </div>
  );
};

export default AdminFeeConfigurations;


