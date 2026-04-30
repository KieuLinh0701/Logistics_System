import React from "react";
import { Form, Input, Modal, Select } from "antd";
import type { FormInstance } from "antd";
import type { AdminUser } from "../../../../types/user";
import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";
import { translateRoleName } from "../../../../utils/roleUtils";

interface AddEditUserModalProps {
  open: boolean;
  editingUser: AdminUser | null;
  actionLoading: boolean;
  form: FormInstance;
  onCancel: () => void;
  onSubmit: () => void;
  roles?: Array<{ id: number; name: string }>;
}

const AddEditUserModal: React.FC<AddEditUserModalProps> = ({ open, editingUser, actionLoading, form, onCancel, onSubmit, roles }) => {
  React.useEffect(() => {
    if (open) {
      if (editingUser) {
        setTimeout(() => {
          try {
            const prefillRoleIds = editingUser.rolesIds && editingUser.rolesIds.length ? editingUser.rolesIds : (editingUser.roleId ? [editingUser.roleId] : []);
            
            form.setFieldsValue({
              email: editingUser.email,
              firstName: editingUser.firstName,
              lastName: editingUser.lastName,
              phoneNumber: editingUser.phoneNumber,
              roleIds: prefillRoleIds,
              isActive: editingUser.isActive,
            });
          } catch (e) {
            // ignore
          }
        }, 0);
      } else {
        form.resetFields();
      }
    }
  }, [open, editingUser, form]);

  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={940}
      bodyStyle={{ minHeight: 360 }}
      title={<div className="modal-title">{editingUser ? "Cập nhật người dùng" : "Tạo người dùng"}</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={editingUser ? "Cập nhật" : "Tạo mới"}
      confirmLoading={actionLoading}
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" form={form} layout="vertical">
        <div className="modal-section">
          <div className="modal-grid">
            {/* Row 1: Email (full width) */}
            <Form.Item
              name="email"
              label={<span className="modal-lable">Email</span>}
              rules={[{ required: true, message: "Vui lòng nhập email" }, { type: 'email' }]}
              className="form-item title-item"
              style={{ gridColumn: '1 / 3' }}
            >
              <Input className="modal-input" placeholder="Nhập email..." disabled={!!editingUser} />
            </Form.Item>

            {/* Row 2: Họ | Mật khẩu */}
            <Form.Item
              name="lastName"
              label={<span className="modal-lable">Họ</span>}
              rules={[{ required: true, message: "Vui lòng nhập họ" }]}
              className="form-item"
              style={{ gridColumn: '1 / 2' }}
            >
              <Input className="modal-input" placeholder="Nhập họ..." />
            </Form.Item>

            <Form.Item
              name="password"
              label={<span className="modal-lable">Mật khẩu</span>}
              rules={editingUser ? [] : [{ required: true, min: 6 }]}
              className="form-item"
              style={{ gridColumn: '2 / 3' }}
            >
              <Input.Password className="modal-input" placeholder={editingUser ? 'Để trống nếu không đổi' : 'Nhập mật khẩu...'} />
            </Form.Item>

            {/* Row 3: Tên | Vai trò */}
            <Form.Item
              name="firstName"
              label={<span className="modal-lable">Tên</span>}
              rules={[{ required: true, message: "Vui lòng nhập tên" }]}
              className="form-item"
              style={{ gridColumn: '1 / 2' }}
            >
              <Input className="modal-input" placeholder="Nhập tên..." />
            </Form.Item>

            <Form.Item
              name="roleIds"
              label={<span className="modal-lable">Vai trò</span>}
              rules={[{ required: true, type: 'array', min: 1, message: "Vui lòng chọn ít nhất 1 vai trò" }]}
              className="form-item"
              style={{ gridColumn: '2 / 3' }}
            >
              <Select
                className="modal-custom-select"
                placeholder={roles && roles.length ? "Chọn vai trò..." : "Danh sách vai trò chưa có"}
                mode="multiple"
                disabled={!(roles && roles.length)}
              >
                {roles && roles.length && roles.map(r => (
                  <Select.Option key={r.id} value={r.id}>{translateRoleName(r.name)}</Select.Option>
                ))}
              </Select>
            </Form.Item>

            {/* Row 4: Số điện thoại | Trạng thái */}
            <Form.Item
              name="phoneNumber"
              label={<span className="modal-lable">Số điện thoại</span>}
              rules={[{ required: true, message: "Vui lòng nhập số điện thoại" }]}
              className="form-item"
              style={{ gridColumn: '1 / 2' }}
            >
              <Input className="modal-input" placeholder="Nhập số điện thoại..." />
            </Form.Item>

            <Form.Item
              name="isActive"
              label={<span className="modal-lable">Trạng thái</span>}
              rules={[]}
              className="form-item"
              style={{ gridColumn: '2 / 3' }}
            >
              <Select className="modal-custom-select" defaultValue={true}>
                <Select.Option value={true}>Hoạt động</Select.Option>
                <Select.Option value={false}>Khóa</Select.Option>
              </Select>
            </Form.Item>

          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default AddEditUserModal;
