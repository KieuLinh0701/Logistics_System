import React, { useEffect, useMemo, useState } from "react";
import { Button, Card, Form, Input, Modal, Popconfirm, Select, Space, Table, Tag, message, Typography } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import userApi from "../../api/userApi";
import { translateRoleName } from "../../utils/roleUtils";
import type { AdminUser } from "../../types/user";


const { Title } = Typography;

type QueryState = { page: number; limit: number; search: string };

const AdminUsers: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<AdminUser[]>([]);
  const [total, setTotal] = useState(0);
  const [query, setQuery] = useState<QueryState>({ page: 1, limit: 10, search: "" });
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<AdminUser | null>(null);
  const [roles, setRoles] = useState<Array<{ id: number; name: string }>>([]);
  const [form] = Form.useForm();

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await userApi.listAdminUsers({ page: query.page, limit: query.limit, search: query.search });
      if (res.success && res.data) {
        setRows(res.data.data || []);
        setTotal(res.data.pagination?.total || 0);
      }
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Tải dữ liệu thất bại");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [query.page, query.limit, query.search]);

  const optionsRoles = [
    { id: 1, name: 'Admin' },
    { id: 2, name: 'Manager' },
    { id: 3, name: 'User' },
    { id: 4, name: 'Shipper' },
    { id: 5, name: 'Driver' },
  ];

  const onCreate = () => {
    setEditing(null);
    form.resetFields();
    setOpen(true);
  };

  const onEdit = (record: AdminUser) => {
    setEditing(record);
    form.setFieldsValue({
      firstName: record.firstName,
      lastName: record.lastName,
      phoneNumber: record.phoneNumber,
      roleId: record.roleId,
      isActive: record.isActive,
    });
    setOpen(true);
  };

  const onDelete = async (id: number) => {
    try {
      await userApi.deleteAdminUser(id);
      message.success("Đã xóa");
      fetchData();
    } catch (e: any) {
      message.error(e?.response?.data?.message || "Xóa thất bại");
    }
  };

  const submit = async () => {
    try {
      const values = await form.validateFields();
      if (editing) {
        await userApi.updateAdminUser(editing.id!, {
          password: values.password,
          firstName: values.firstName,
          lastName: values.lastName,
          phoneNumber: values.phoneNumber,
          roleId: values.roleId,
          isActive: values.isActive,
        });
        message.success("Cập nhật thành công");
      } else {
        await userApi.createAdminUser({
          email: values.email,
          password: values.password,
          firstName: values.firstName,
          lastName: values.lastName,
          phoneNumber: values.phoneNumber,
          roleId: values.roleId,
          isActive: values.isActive ?? true,
        });
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

  const getRoleColor = (role: string) => {
    const colors: Record<string, string> = {
      admin: "red",
      manager: "blue",
      staff: "green",
      driver: "purple",
      user: "gold",
      shipper: "orange",
      employee: "cyan",
    };
    return colors[role] || "default";
  };

  // Nhãn vai trò bằng tiếng Việt để hiển thị trên UI
  const ROLE_LABELS: Record<string, string> = {
    admin: "Quản trị viên",
    manager: "Quản lý bưu cục",
    user: "Cửa hàng",
    employee: "Nhân viên",
    shipper: "Nhân viên giao hàng",
    driver: "Tài xế lái xe",
    staff: "Nhân viên",
  };

  const columns = useMemo(
    () => [
      { title: "Email", dataIndex: "email" },
      { title: "Họ", dataIndex: "lastName" },
      { title: "Tên", dataIndex: "firstName" },
      { title: "SĐT", dataIndex: "phoneNumber" },
      {
        title: "Vai trò",
        dataIndex: "role",
        render: (v: string) => {
          const key = v ? v.toLowerCase() : "";
          return <Tag color={getRoleColor(key)}>{ROLE_LABELS[key] || v}</Tag>;
        },
      },
      {
        title: "Trạng thái",
        dataIndex: "isActive",
        render: (v: boolean) => <Tag color={v ? "green" : "default"}>{v ? "Hoạt động" : "Khóa"}</Tag>,
      },
      {
        title: "Thao tác",
        render: (_: any, record: AdminUser) => (
          <Space>
            <Button size="small" onClick={() => onEdit(record)}>
              Sửa
            </Button>
            <Popconfirm title="Xóa người dùng này?" onConfirm={() => onDelete(record.id)}>
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
          Quản lý người dùng
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
            <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
              Thêm người dùng
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
          title={editing ? "Cập nhật người dùng" : "Tạo người dùng"}
          open={open}
          onOk={submit}
          onCancel={() => setOpen(false)}
          destroyOnClose
          forceRender
        >
          <Form form={form} layout="vertical" preserve={false}>
            {editing ? (
              <Form.Item name="email" label="Email">
                <Input disabled />
              </Form.Item>
            ) : (
              <Form.Item name="email" label="Email" rules={[{ required: true }, { type: "email" }]}>
                <Input />
              </Form.Item>
            )}
            <Form.Item
              name="password"
              label={editing ? "Mật khẩu (để trống nếu không đổi)" : "Mật khẩu"}
              rules={editing ? [] : [{ required: true, min: 6 }]}
            >
              <Input.Password />
            </Form.Item>
            <Form.Item name="lastName" label="Họ" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="firstName" label="Tên" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="phoneNumber" label="Số điện thoại" rules={[{ required: true }]}>
              <Input />
            </Form.Item>
            <Form.Item name="roleId" label="Vai trò" rules={[{ required: true }]}>
              <Select placeholder="Chọn vai trò">
                {optionsRoles.map((r) => (
                  <Select.Option key={r.id} value={r.id}>{translateRoleName(r.name)}</Select.Option>
                ))}
                {/* Nếu đang edit một account có roleId không nằm trong list, hiển thị option động */}
                {editing && editing.roleId && !optionsRoles.map(r=>r.id).includes(editing.roleId) && (
                  <Select.Option value={editing.roleId}>{ROLE_LABELS[(editing.role||"").toLowerCase()] || editing.role || `Role ${editing.roleId}`}</Select.Option>
                )}
              </Select>
            </Form.Item>
            <Form.Item name="isActive" label="Trạng thái" initialValue={true}>
              <Select>
                <Select.Option value={true}>Hoạt động</Select.Option>
                <Select.Option value={false}>Khóa</Select.Option>
              </Select>
            </Form.Item>
          </Form>
        </Modal>
      </Card>
    </div>
  );
};

export default AdminUsers;



