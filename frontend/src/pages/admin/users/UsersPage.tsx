import React, { useState, useEffect, useRef } from "react";
import { Form, message } from "antd";
import { PlusOutlined, UsergroupAddOutlined } from "@ant-design/icons";
import UsersToolbar from "./components/UsersToolbar.tsx";
import UsersTable from "./components/UsersTable";
import AddEditUserModal from "./components/AddEditUserModal";
import userApi from "../../../api/userApi";
import roleApi from "../../../api/roleApi";
import "../../hr/recruitment/components/RecruitmentShared.css";
import "../../../styles/ListPage.css";
import "./UsersTable.css";
import type { AdminUser as AU } from "../../../types/user";

const UsersPage: React.FC = () => {
  const latestRequestRef = useRef(0);
  const [editing, setEditing] = useState<AU | null>(null);
  const [openForm, setOpenForm] = useState(false);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [roles, setRoles] = useState<Array<{ id: number; name: string }>>([]);

  const [data, setData] = useState<AU[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [total, setTotal] = useState<number>(0);

  const [searchText, setSearchText] = useState<string>("");
  const [filterStatus, setFilterStatus] = useState<string | undefined>(undefined);
  const [filterRole, setFilterRole] = useState<number | undefined>(undefined);
  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);

  const handleRefreshAll = () => {
    setSearchText("");
    setFilterStatus(undefined);
    setFilterRole(undefined);
    setPage(1);
  };
  const handleAdd = () => { setEditing(null); form.resetFields(); setOpenForm(true); };
  const handleEdit = async (u: AU) => {
    // ensure roles loaded so Select has matching options when pre-filling
    try {
      if (!roles || roles.length === 0) {
        const res = await roleApi.getAdminRoles();
        if (res && res.success && res.data) {
          setRoles(res.data || []);
        }
      }
    } catch (e) {
      // ignore
    }
    setEditing(u);
    form.setFieldsValue({
      email: u.email,
      firstName: u.firstName,
      lastName: u.lastName,
      phoneNumber: u.phoneNumber,
      roleIds: u.rolesIds || (u.roleId ? [u.roleId] : []),
      isActive: u.isActive
    });
    setOpenForm(true);
  };

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const values = await form.validateFields();
      if (editing) {
        const payload: any = {
          firstName: values.firstName,
          lastName: values.lastName,
          phoneNumber: values.phoneNumber,
          roleIds: values.roleIds,
          isActive: values.isActive,
        };
        if (values.password) payload.password = values.password;
        await userApi.updateAdminUser(editing.id, payload as any);
        message.success("Cập nhật thành công");
      } else {
        const payload: any = {
          email: values.email,
          password: values.password,
          firstName: values.firstName,
          lastName: values.lastName,
          phoneNumber: values.phoneNumber,
          roleIds: values.roleIds,
          isActive: values.isActive,
        };
        await userApi.createAdminUser(payload as any);
        message.success("Tạo mới thành công");
      }
      setOpenForm(false);
    } catch (e: any) {
      message.error(e?.message || "Lưu thất bại");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      setLoading(true);
      const res = await userApi.deleteAdminUser(id);
      if (res && res.success) {
        message.success('Đã xóa');
        fetchUsers();
      } else {
        message.error(res.message || 'Xóa thất bại');
      }
    } catch (e: any) {
      message.error(e?.message || 'Xóa thất bại');
    } finally {
      setLoading(false);
    }
  };

  // fetch roles for selects
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const res = await roleApi.getAdminRoles();
        if (mounted && res && res.success && res.data) {
          setRoles(res.data || []);
        }
      } catch (e) {
        // ignore
      }
    })();
    return () => { mounted = false; };
  }, []);

  const fetchUsers = async (p: number = page) => {
    try {
      setLoading(true);
      const requestId = ++latestRequestRef.current;
      const params: any = {
        page: p,
        limit: pageSize,
      };
      if (searchText) params.search = searchText;
      if (filterStatus) params.status = filterStatus;
      if (typeof filterRole === 'number' && Number.isFinite(filterRole)) {
        const roleObj = roles.find(r => r.id === filterRole);
        if (roleObj && roleObj.name) params.role = roleObj.name;
        else params.roleId = filterRole;
      }

      const res = await userApi.listAdminUsers(params);
      if (requestId !== latestRequestRef.current) return;
      if (res && res.success && res.data) {
        setData(res.data.data || []);
        setTotal(res.data.pagination?.total || (res.data.data || []).length);
      } else {
        message.error(res.message || 'Lỗi khi lấy danh sách người dùng');
      }
    } catch (e: any) {
      message.error(e?.message || 'Lỗi khi lấy danh sách người dùng');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers(page);
  }, [page, pageSize, searchText, filterStatus, filterRole]);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <style>{'.hr-recruitment-filter-actions .ant-btn-primary{display:none !important}'} </style>

        <UsersToolbar
          searchValue={searchText}
          statusValue={filterStatus}
          roleValue={filterRole}
          onSearchChange={(q) => { setSearchText(q || ""); setPage(1); }}
          onRefresh={handleRefreshAll}
          onCreate={handleAdd}
          onStatusChange={(s) => { setFilterStatus(s); setPage(1); }}
          onRoleChange={(r) => { setFilterRole(r); setPage(1); }}
          roles={roles}
        />

        <div className="list-page-header" style={{ marginTop: 12 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <UsergroupAddOutlined className="title-icon" />
              <h3 className="list-page-title-main">Quản lý người dùng</h3>
            </div>
            <div style={{ marginTop: 8 }}>
              <div className="list-page-tag">Tổng số người dùng: {total}</div>
            </div>
          </div>
          <div className="list-page-actions">
            <button className="primary-button" onClick={handleAdd} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <PlusOutlined />
              Thêm người dùng
            </button>
          </div>
        </div>

        <div className="list-page-table">
          <UsersTable
            data={data}
            loading={loading}
            page={page}
            pageSize={pageSize}
            total={total}
            onPageChange={(p, ps) => { setPage(p); if (ps) setPageSize(ps); }}
            roles={roles}
            onEdit={handleEdit}
            onDelete={handleDelete}
          />
        </div>

          <AddEditUserModal open={openForm} editingUser={editing} actionLoading={submitting} form={form} onCancel={() => setOpenForm(false)} onSubmit={handleSubmit} roles={roles} />
      </div>
    </div>
  );
};

export default UsersPage;
