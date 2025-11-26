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
  DatePicker,
  Switch,
  Divider,
  Row,
  Col,
} from "antd";
import dayjs, { Dayjs } from "dayjs";
import promotionApi from "../../api/promotionApi";
import serviceTypeApi from "../../api/serviceTypeApi";
import userApi from "../../api/userApi";
import type { Promotion, CreatePromotionPayload } from "../../types/promotion";
import type { AdminServiceType } from "../../types/serviceType";
import type { AdminUser } from "../../types/user";

const { Title } = Typography;
const { TextArea } = Input;

type QueryState = { page: number; limit: number; search: string; status?: string; isGlobal?: boolean };

const statusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Tạm dừng", value: "INACTIVE" },
  { label: "Hết hạn", value: "EXPIRED" },
];

const discountTypeOptions = [
  { label: "Phần trăm (%)", value: "PERCENTAGE" },
  { label: "Số tiền cố định", value: "FIXED" },
];

const AdminPromotions: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<Promotion[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Promotion | null>(null);
  const [form] = Form.useForm();
  const [serviceTypes, setServiceTypes] = useState<AdminServiceType[]>([]);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [isGlobal, setIsGlobal] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const res = await promotionApi.listAdminPromotions({
        page: query.page,
        limit: query.limit,
        search: query.search,
        status: query.status,
        isGlobal: query.isGlobal,
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
  }, [query.page, query.limit, query.search, query.status, query.isGlobal]);

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

  useEffect(() => {
    const loadUsers = async () => {
      try {
        const res = await userApi.listAdminUsers({ page: 1, limit: 100 });
        if (res.success && res.data) {
          setUsers(res.data.data || []);
        }
      } catch (e) {
        console.error("Failed to load users", e);
      }
    };
    if (!isGlobal) {
      loadUsers();
    }
  }, [isGlobal]);

  const onCreate = () => {
    setEditing(null);
    setIsGlobal(true);
    form.resetFields();
    form.setFieldsValue({
      isGlobal: true,
      discountType: "PERCENTAGE",
      status: "ACTIVE",
      firstTimeUser: false,
    });
    setOpen(true);
  };

  const onEdit = (record: Promotion) => {
    setEditing(record);
    setIsGlobal(record.isGlobal);
    form.setFieldsValue({
      code: record.code,
      title: record.title,
      description: record.description,
      discountType: record.discountType,
      discountValue: record.discountValue,
      isGlobal: record.isGlobal,
      maxDiscountAmount: record.maxDiscountAmount,
      startDate: record.startDate ? dayjs(record.startDate) : null,
      endDate: record.endDate ? dayjs(record.endDate) : null,
      minOrderValue: record.minOrderValue,
      minWeight: record.minWeight,
      maxWeight: record.maxWeight,
      minOrdersCount: record.minOrdersCount,
      serviceTypeIds: record.serviceTypeIds,
      firstTimeUser: record.firstTimeUser,
      validMonthsAfterJoin: record.validMonthsAfterJoin,
      validYearsAfterJoin: record.validYearsAfterJoin,
      usageLimit: record.usageLimit,
      maxUsagePerUser: record.maxUsagePerUser,
      dailyUsageLimitGlobal: record.dailyUsageLimitGlobal,
      dailyUsageLimitPerUser: record.dailyUsageLimitPerUser,
      status: record.status,
      userIds: record.userIds,
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await promotionApi.deleteAdminPromotion(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      console.error("[AdminPromotions] Failed to delete promotion", e);
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submit = async () => {
    try {
      const values = await form.validateFields();
      const payload: CreatePromotionPayload = {
        code: values.code?.toUpperCase().trim(),
        title: values.title,
        description: values.description,
        discountType: values.discountType,
        discountValue: values.discountValue,
        isGlobal: values.isGlobal,
        maxDiscountAmount: values.maxDiscountAmount,
        startDate: values.startDate?.toISOString(),
        endDate: values.endDate?.toISOString(),
        minOrderValue: values.minOrderValue,
        minWeight: values.minWeight,
        maxWeight: values.maxWeight,
        minOrdersCount: values.minOrdersCount,
        serviceTypeIds: values.serviceTypeIds,
        firstTimeUser: values.firstTimeUser,
        validMonthsAfterJoin: values.validMonthsAfterJoin,
        validYearsAfterJoin: values.validYearsAfterJoin,
        usageLimit: values.usageLimit,
        maxUsagePerUser: values.maxUsagePerUser,
        dailyUsageLimitGlobal: values.dailyUsageLimitGlobal,
        dailyUsageLimitPerUser: values.dailyUsageLimitPerUser,
        status: values.status || "ACTIVE",
        userIds: !values.isGlobal ? values.userIds : undefined,
      };

      if (editing) {
        await promotionApi.updateAdminPromotion(editing.id, payload);
        message.success("Cập nhật thành công");
      } else {
        await promotionApi.createAdminPromotion(payload);
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

  const getStatusColor = (status: string) => {
    switch (status) {
      case "ACTIVE": return "green";
      case "INACTIVE": return "orange";
      case "EXPIRED": return "red";
      default: return "default";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "ACTIVE": return "Hoạt động";
      case "INACTIVE": return "Tạm dừng";
      case "EXPIRED": return "Hết hạn";
      default: return status;
    }
  };

  const getPromotionType = (promo: Promotion) => {
    if (!promo.isGlobal) return "Theo user";
    if (promo.minOrderValue || promo.minWeight || promo.minOrdersCount || promo.firstTimeUser) return "Điều kiện";
    return "Chung";
  };

  const columns = useMemo(
    () => [
      {
        title: "Mã",
        dataIndex: "code",
        render: (code: string) => (
          <Tag color="blue" style={{ fontFamily: "monospace", fontWeight: "bold" }}>
            {code}
          </Tag>
        ),
      },
      {
        title: "Tiêu đề",
        dataIndex: "title",
        ellipsis: true,
      },
      {
        title: "Loại",
        key: "type",
        render: (_: any, record: Promotion) => (
          <Tag color={record.isGlobal ? "green" : "purple"}>{getPromotionType(record)}</Tag>
        ),
      },
      {
        title: "Giảm giá",
        key: "discount",
        render: (_: any, record: Promotion) => (
          <span>
            {record.discountType === "PERCENTAGE"
              ? `${record.discountValue}%`
              : `${record.discountValue.toLocaleString("vi-VN")}đ`}
          </span>
        ),
      },
      {
        title: "Thời gian",
        key: "dateRange",
        render: (_: any, record: Promotion) => (
          <div>
            <div>{dayjs(record.startDate).format("DD/MM/YYYY")}</div>
            <div>{dayjs(record.endDate).format("DD/MM/YYYY")}</div>
          </div>
        ),
      },
      {
        title: "Sử dụng",
        key: "usage",
        render: (_: any, record: Promotion) => (
          <div>
            {record.usedCount} / {record.usageLimit || "∞"}
          </div>
        ),
      },
      {
        title: "Trạng thái",
        dataIndex: "status",
        render: (status: string) => (
          <Tag color={getStatusColor(status)}>{getStatusText(status)}</Tag>
        ),
      },
      {
        title: "Thao tác",
        render: (_: any, record: Promotion) => (
          <Space>
            <Button size="small" onClick={() => onEdit(record)}>
              Sửa
            </Button>
            <Popconfirm title="Xóa khuyến mãi này?" onConfirm={() => onDelete(record.id)}>
              <Button size="small" danger disabled={record.usedCount > 0}>
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
          Quản lý khuyến mãi
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
              style={{ width: 250 }}
            />
            <Select
              placeholder="Lọc trạng thái"
              allowClear
              style={{ width: 150 }}
              value={query.status}
              onChange={(v) => setQuery({ ...query, page: 1, status: v })}
            >
              {statusOptions.map((opt) => (
                <Select.Option key={opt.value} value={opt.value}>
                  {opt.label}
                </Select.Option>
              ))}
            </Select>
            <Select
              placeholder="Loại khuyến mãi"
              allowClear
              style={{ width: 150 }}
              value={query.isGlobal}
              onChange={(v) => setQuery({ ...query, page: 1, isGlobal: v })}
            >
              <Select.Option value={true}>Chung/Điều kiện</Select.Option>
              <Select.Option value={false}>Theo user</Select.Option>
            </Select>
            <Button type="primary" onClick={onCreate}>
              Thêm khuyến mãi
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
          title={editing ? "Cập nhật khuyến mãi" : "Thêm khuyến mãi"}
          open={open}
          onOk={submit}
          onCancel={() => setOpen(false)}
          destroyOnClose
          width={900}
          style={{ top: 20 }}
        >
          <Form form={form} layout="vertical">
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="code"
                  label="Mã khuyến mãi"
                  rules={[{ required: true, message: "Vui lòng nhập mã" }]}
                >
                  <Input placeholder="VD: SALE20" />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="title" label="Tiêu đề">
                  <Input placeholder="Tiêu đề khuyến mãi" />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="description" label="Mô tả">
              <TextArea rows={2} placeholder="Mô tả" />
            </Form.Item>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item
                  name="discountType"
                  label="Loại giảm giá"
                  rules={[{ required: true }]}
                >
                  <Select>
                    {discountTypeOptions.map((opt) => (
                      <Select.Option key={opt.value} value={opt.value}>
                        {opt.label}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item
                  name="discountValue"
                  label="Giá trị giảm"
                  rules={[{ required: true }]}
                >
                  <InputNumber style={{ width: "100%" }} min={0.01} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="maxDiscountAmount" label="Giảm tối đa (đ)">
                  <InputNumber style={{ width: "100%" }} min={0} />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={12}>
                <Form.Item name="startDate" label="Ngày bắt đầu" rules={[{ required: true }]}>
                  <DatePicker style={{ width: "100%" }} />
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item name="endDate" label="Ngày kết thúc" rules={[{ required: true }]}>
                  <DatePicker style={{ width: "100%" }} />
                </Form.Item>
              </Col>
            </Row>
            <Divider orientation="left">Loại khuyến mãi</Divider>
            <Form.Item name="isGlobal" label="Khuyến mãi chung" valuePropName="checked">
              <Switch
                checked={isGlobal}
                onChange={(checked) => {
                  setIsGlobal(checked);
                  form.setFieldsValue({ isGlobal: checked, userIds: undefined });
                }}
              />
            </Form.Item>
            {!isGlobal && (
              <Form.Item name="userIds" label="Chọn user">
                <Select mode="multiple" placeholder="Chọn user">
                  {users.map((u) => (
                    <Select.Option key={u.id} value={u.id}>
                      {u.firstName} {u.lastName} ({u.email})
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            )}
            <Divider orientation="left">Điều kiện áp dụng</Divider>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="minOrderValue" label="Đơn tối thiểu (đ)">
                  <InputNumber style={{ width: "100%" }} min={0} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="minWeight" label="Trọng lượng từ (kg)">
                  <InputNumber style={{ width: "100%" }} min={0} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="maxWeight" label="Trọng lượng đến (kg)">
                  <InputNumber style={{ width: "100%" }} min={0} />
                </Form.Item>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={8}>
                <Form.Item name="minOrdersCount" label="Số đơn tối thiểu">
                  <InputNumber style={{ width: "100%" }} min={0} />
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="serviceTypeIds" label="Loại dịch vụ">
                  <Select mode="multiple" placeholder="Chọn loại dịch vụ">
                    {serviceTypes.map((st) => (
                      <Select.Option key={st.id} value={st.id}>
                        {st.name}
                      </Select.Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>
              <Col span={8}>
                <Form.Item name="firstTimeUser" label="Chỉ user mới" valuePropName="checked">
                  <Switch />
                </Form.Item>
              </Col>
            </Row>
            <Divider orientation="left">Giới hạn sử dụng</Divider>
            <Row gutter={16}>
              <Col span={6}>
                <Form.Item name="usageLimit" label="Tổng giới hạn">
                  <InputNumber style={{ width: "100%" }} min={1} />
                </Form.Item>
              </Col>
              <Col span={6}>
                <Form.Item name="maxUsagePerUser" label="Mỗi user">
                  <InputNumber style={{ width: "100%" }} min={1} />
                </Form.Item>
              </Col>
              <Col span={6}>
                <Form.Item name="dailyUsageLimitGlobal" label="Giới hạn/ngày (global)">
                  <InputNumber style={{ width: "100%" }} min={1} />
                </Form.Item>
              </Col>
              <Col span={6}>
                <Form.Item name="dailyUsageLimitPerUser" label="Giới hạn/ngày/user">
                  <InputNumber style={{ width: "100%" }} min={1} />
                </Form.Item>
              </Col>
            </Row>
            <Form.Item name="status" label="Trạng thái" initialValue="ACTIVE">
              <Select>
                {statusOptions.map((opt) => (
                  <Select.Option key={opt.value} value={opt.value}>
                    {opt.label}
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

export default AdminPromotions;


