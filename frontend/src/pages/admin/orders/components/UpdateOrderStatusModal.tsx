import React from "react";
import { Form, Modal, Select } from "antd";
import type { FormInstance } from "antd";

interface Option {
  label: string;
  value: string;
}

interface OfficeOption {
  id: number;
  name: string;
}

interface UpdateOrderStatusModalProps {
  open: boolean;
  form: FormInstance;
  statusOptions: Option[];
  offices: OfficeOption[];
  onCancel: () => void;
  onSubmit: () => void;
}

const UpdateOrderStatusModal: React.FC<UpdateOrderStatusModalProps> = ({
  open,
  form,
  statusOptions,
  offices,
  onCancel,
  onSubmit,
}) => {
  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={720}
      bodyStyle={{ minHeight: 220 }}
      title={<div className="modal-title">Cập nhật đơn hàng</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText="Cập nhật"
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" layout="vertical" form={form}>
        <div className="modal-section">
          <div className="modal-grid">
            <Form.Item
              name="status"
              label={<span className="modal-lable">Trạng thái</span>}
              rules={[{ required: true, message: "Vui lòng chọn trạng thái" }]}
              className="form-item"
              style={{ gridColumn: "1 / 2" }}
            >
              <Select className="modal-custom-select" placeholder="Chọn trạng thái">
                {statusOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="fromOfficeId"
              label={<span className="modal-lable">Bưu cục xuất (tùy chọn)</span>}
              className="form-item"
              style={{ gridColumn: "2 / 3" }}
            >
              <Select className="modal-custom-select" allowClear showSearch placeholder="Chọn bưu cục">
                {offices.map((office) => (
                  <Select.Option key={office.id} value={office.id}>
                    {office.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default UpdateOrderStatusModal;
