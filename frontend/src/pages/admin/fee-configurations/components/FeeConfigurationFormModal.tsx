import React from "react";
import { Button, Form, Input, InputNumber, Modal, Select, Switch } from "antd";
import type { AdminServiceType } from "../../../../types/serviceType";
import type { Option } from "./types";

import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";

interface FeeConfigurationFormModalProps {
  open: boolean;
  editing: boolean;
  form: any;
  serviceTypes: AdminServiceType[];
  feeTypeOptions: Option[];
  calculationTypeOptions: Option[];
  canSubmit: boolean;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: () => void;
  onFormChange: () => void;
}

const FeeConfigurationFormModal: React.FC<FeeConfigurationFormModalProps> = ({
  open,
  editing,
  form,
  serviceTypes,
  feeTypeOptions,
  calculationTypeOptions,
  canSubmit,
  submitting,
  onCancel,
  onSubmit,
  onFormChange,
}) => {
  return (
    <Modal
      className="hr-job-posting-modal modal-hide-scrollbar"
      width={720}
      open={open}
      destroyOnClose
      title={
        <div className="modal-title">
          {editing ? "Cập nhật cấu hình phí" : "Thêm cấu hình phí"}
        </div>
      }
      onCancel={onCancel}
      footer={
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
          <Button onClick={onCancel}>Hủy</Button>
          <Button type="primary" onClick={onSubmit} loading={submitting} disabled={!canSubmit}>
            Lưu
          </Button>
        </div>
      }
    >
      <Form
        form={form}
        layout="vertical"
        className="hr-job-posting-form"
        onValuesChange={onFormChange}
      >
        <div className="modal-section">
          <div className="modal-grid">

            {/* Loại dịch vụ */}
            <Form.Item
              name="serviceTypeId"
              label="Loại dịch vụ"
              className="form-item"
              style={{ gridColumn: "1 / 3" }}
            >
              <Select
                allowClear
                className="modal-custom-select"
                placeholder="Để trống = áp dụng cho tất cả"
              >
                {serviceTypes.map((item) => (
                  <Select.Option key={item.id} value={item.id}>
                    {item.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            {/* Loại phí */}
            <Form.Item
              name="feeType"
              label="Loại phí"
              rules={[{ required: true, message: "Vui lòng chọn loại phí" }]}
              className="form-item"
            >
              <Select className="modal-custom-select">
                {feeTypeOptions.map((item) => (
                  <Select.Option key={item.value} value={item.value}>
                    {item.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            {/* Cách tính */}
            <Form.Item
              name="calculationType"
              label="Cách tính"
              rules={[{ required: true, message: "Vui lòng chọn cách tính" }]}
              className="form-item"
            >
              <Select className="modal-custom-select">
                {calculationTypeOptions.map((item) => (
                  <Select.Option key={item.value} value={item.value}>
                    {item.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            {/* Giá trị phí */}
            <Form.Item
              name="feeValue"
              label="Giá trị phí"
              rules={[{ required: true, message: "Vui lòng nhập giá trị phí" }]}
              className="form-item"
              style={{ gridColumn: "1 / 3" }}
            >
              <InputNumber
                min={0}
                className="modal-input"
                style={{ width: "100%" }}
                placeholder="VD: 5 (%) hoặc 5000 (đ)"
              />
            </Form.Item>

            {/* Áp dụng từ */}
            <Form.Item
              name="minOrderFee"
              label="Áp dụng từ (đ)"
              className="form-item"
            >
              <InputNumber
                min={0}
                className="modal-input"
                style={{ width: "100%" }}
                placeholder="Không giới hạn"
              />
            </Form.Item>

            {/* Áp dụng đến */}
            <Form.Item
              name="maxOrderFee"
              label="Áp dụng đến (đ)"
              className="form-item"
            >
              <InputNumber
                min={0}
                className="modal-input"
                style={{ width: "100%" }}
                placeholder="Không giới hạn"
              />
            </Form.Item>

            {/* Hoạt động */}
            <Form.Item
              name="active"
              label="Hoạt động"
              valuePropName="checked"
              initialValue={true}
              className="form-item"
              style={{ gridColumn: "1 / 3" }}
            >
              <Switch />
            </Form.Item>

            {/* Ghi chú */}
            <Form.Item name="notes" label={<span className="modal-lable">Ghi chú</span>} style={{ gridColumn: "1 / 3" }}>
              <Input.TextArea className="modal-textarea" autoSize={{ minRows: 2, maxRows: 5 }} placeholder="Nhập ghi chú (tùy chọn)" />
            </Form.Item>

          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default FeeConfigurationFormModal;