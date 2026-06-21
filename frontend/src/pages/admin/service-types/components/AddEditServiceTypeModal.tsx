import React from "react";
import type {FormInstance} from "antd";
import {Form, Input, InputNumber, Modal, Select} from "antd";
import type {AdminServiceType} from "../../../../types/serviceType";
import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";

interface Option {
  label: string;
  value: string;
}

interface AddEditServiceTypeModalProps {
  open: boolean;
  editing: AdminServiceType | null;
  form: FormInstance;
  submitting: boolean;
  statusOptions: Option[];
  timeUnitOptions: Option[];
  onCancel: () => void;
  onSubmit: () => void;
}

const AddEditServiceTypeModal: React.FC<AddEditServiceTypeModalProps> = ({
  open,
  editing,
  form,
  submitting,
  statusOptions,
  timeUnitOptions,
  onCancel,
  onSubmit,
}) => {
  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={900}
      bodyStyle={{ minHeight: 320 }}
      title={<div className="modal-title">{editing ? "Cập nhật loại dịch vụ" : "Thêm loại dịch vụ"}</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={editing ? "Cập nhật" : "Tạo mới"}
      confirmLoading={submitting}
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" form={form} layout="vertical">
        <div className="modal-section">
          <div className="modal-grid">
            <Form.Item name="name" label={<span className="modal-lable">Tên dịch vụ</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 3" }}>
              <Input className="modal-input" placeholder="Nhập tên dịch vụ" />
            </Form.Item>

            <Form.Item name="timeFrom" label={<span className="modal-lable">Thời gian từ</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <InputNumber className="modal-input" min={0} style={{ width: "100%" }} placeholder="Ví dụ: 5" />
            </Form.Item>

            <Form.Item name="timeTo" label={<span className="modal-lable">Thời gian đến</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <InputNumber className="modal-input" min={0} style={{ width: "100%" }} placeholder="Ví dụ: 7" />
            </Form.Item>

            <Form.Item name="timeUnit" label={<span className="modal-lable">Đơn vị</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <Select className="modal-custom-select" placeholder="Chọn đơn vị">
                {timeUnitOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="status" label={<span className="modal-lable">Trạng thái</span>} initialValue="ACTIVE" className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Select className="modal-custom-select">
                {statusOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="description" label={<span className="modal-lable">Mô tả</span>} style={{ gridColumn: "1 / 3" }}>
              <Input.TextArea className="modal-textarea" autoSize={{ minRows: 2, maxRows: 5 }} placeholder="Nhập mô tả (tùy chọn)" />
            </Form.Item>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default AddEditServiceTypeModal;
