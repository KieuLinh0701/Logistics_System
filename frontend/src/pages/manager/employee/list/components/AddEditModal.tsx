import React from 'react';
import { Modal, Form, Input, Select, Row, Col, DatePicker } from 'antd';
import type { ManagerEmployee } from '../../../../../types/employee';
import { OFFICE_MANAGER_ADDABLE_ROLES, translateRoleName } from '../../../../../utils/roleUtils';
import { EMPLOYEE_SHIFTS, EMPLOYEE_STATUSES, translateEmployeeShift, translateEmployeeStatus } from '../../../../../utils/employeeUtils';

interface AddEditModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  employee: Partial<ManagerEmployee>;
  onOk: () => void;
  onCancel: () => void;
  loading: boolean;
  form: any;
}

const AddEditModal: React.FC<AddEditModalProps> = ({
  open,
  mode,
  employee,
  onOk,
  onCancel,
  loading,
  form
}) => {

  return (
    <>
      <Modal
        open={open}
        onOk={() => {
          form.validateFields().then(() => {
            onOk();
          });
        }}
        className="modal-hide-scrollbar"
        okButtonProps={{ className: "modal-ok-button", loading }}
        cancelButtonProps={{ className: "modal-cancel-button" }}
        onCancel={onCancel}
        okText={mode === "edit" ? "Cập nhật" : "Thêm mới"}
        title={<span className='modal-title'>{mode === 'edit' ?
          `Chỉnh sửa thông tin nhân viên #${employee.code}`
          : "Thêm nhân viên mới"}</span>}
        width={700}
      >
        <Form form={form} layout="vertical">
          {mode !== "edit" && (
            <>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Họ và tên đệm"
                    name="lastName"
                    rules={[{ required: true, message: "Nhập họ nhân viên!" }]}
                    style={{ marginBottom: 12 }}
                  >
                    <Input
                      className="modal-custom-input"
                      placeholder="Nhập họ..."
                    />
                  </Form.Item>
                </Col>

                <Col span={12}>
                  <Form.Item
                    label="Tên"
                    name="firstName"
                    rules={[{ required: true, message: "Nhập tên nhân viên!" }]}
                    style={{ marginBottom: 12 }}
                  >
                    <Input
                      className="modal-custom-input"
                      placeholder="Nhập tên..."
                    />
                  </Form.Item>
                </Col>
              </Row>

              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    label="Số điện thoại"
                    name="phoneNumber"
                    rules={[
                      { required: true, message: "Nhập số điện thoại!" },
                      {
                        pattern: /^[0-9]{10,11}$/,
                        message: "Số điện thoại không hợp lệ (10-11 chữ số)",
                      },
                    ]} 
                    style={{ marginBottom: 12 }}
                  >
                    <Input
                      className="modal-custom-input"
                      placeholder="Nhập số điện thoại..."
                    />
                  </Form.Item>
                </Col>

                <Col span={12}>
                  <Form.Item
                    label="Email"
                    name="email"
                    rules={[
                      { required: true, message: "Nhập email nhân viên!" },
                      { type: "email", message: "Email không hợp lệ!" },
                    ]}
                    style={{ marginBottom: 12 }}
                  >
                    <Input
                      className="modal-custom-input"
                      placeholder="Nhập email..."
                    />
                  </Form.Item>
                </Col>
              </Row>
            </>
          )}

          {/* 🔹 Thông tin công việc - luôn hiển thị */}
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label="Chức vụ"
                name="role"
                rules={[{ required: true, message: "Chọn chức vụ!" }]}
                style={{ marginBottom: 12 }}
              >
                <Select
                  className="modal-custom-select"
                  placeholder="Chọn chức vụ..."
                >
                  {OFFICE_MANAGER_ADDABLE_ROLES?.map((item) => (
                    <Select.Option key={item} value={item}>
                      {translateRoleName(item)}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item label="Ca làm" name="shift">
                <Select
                  className="modal-custom-select"
                  placeholder="Chọn ca làm..."
                >
                  {EMPLOYEE_SHIFTS?.map((item) => (
                    <Select.Option key={item} value={item}>
                      {translateEmployeeShift(item)}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item label="Ngày tuyển dụng" name="hireDate">
                <DatePicker
                  className="modal-custom-date-picker"
                  style={{ width: "100%" }}
                  placeholder="Chọn ngày thuê..."
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item label="Trạng thái" name="status">
                <Select
                  className="modal-custom-select"
                  placeholder="Chọn trạng thái..."
                >
                  {EMPLOYEE_STATUSES
                    ?.filter(item => mode !== 'create' || item !== 'LEAVE')
                    .map((item) => (
                      <Select.Option key={item} value={item}>
                        {translateEmployeeStatus(item)}
                      </Select.Option>
                    ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </>
  );
};

export default AddEditModal;