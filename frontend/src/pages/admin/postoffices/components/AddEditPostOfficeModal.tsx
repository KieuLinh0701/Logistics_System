import React from "react";
import { Form, Input, InputNumber, Modal, Select } from "antd";
import type { FormInstance } from "antd";
import type { AdminOffice } from "../../../../types/office";
import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";

interface LocationOption {
  code: number;
  name: string;
}

interface Option {
  label: string;
  value: string;
}

interface AddEditPostOfficeModalProps {
  open: boolean;
  editingOffice: AdminOffice | null;
  form: FormInstance;
  submitting: boolean;
  officeTypeOptions: Option[];
  officeStatusOptions: Option[];
  cities: LocationOption[];
  wards: LocationOption[];
  onCancel: () => void;
  onSubmit: () => void;
  onValuesChange: () => void;
  onCityChange: (value?: number) => void;
}

const AddEditPostOfficeModal: React.FC<AddEditPostOfficeModalProps> = ({
  open,
  editingOffice,
  form,
  submitting,
  officeTypeOptions,
  officeStatusOptions,
  cities,
  wards,
  onCancel,
  onSubmit,
  onValuesChange,
  onCityChange,
}) => {
  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={980}
      bodyStyle={{ minHeight: 420 }}
      title={<div className="modal-title">{editingOffice ? "Cập nhật bưu cục" : "Thêm bưu cục"}</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={editingOffice ? "Cập nhật" : "Tạo mới"}
      confirmLoading={submitting}
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" layout="vertical" form={form} onValuesChange={onValuesChange}>
        <div className="modal-section">
          <div className="modal-grid">
            <Form.Item name="code" label={<span className="modal-lable">Mã bưu cục</span>} rules={[{ required: !editingOffice }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <Input className="modal-input" placeholder="Nhập mã" disabled={!!editingOffice} />
            </Form.Item>

            <Form.Item name="name" label={<span className="modal-lable">Tên bưu cục</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Input className="modal-input" placeholder="Nhập tên" />
            </Form.Item>

            <Form.Item name="type" label={<span className="modal-lable">Loại bưu cục</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <Select className="modal-custom-select" placeholder="Chọn loại bưu cục">
                {officeTypeOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="status" label={<span className="modal-lable">Trạng thái</span>} initialValue="ACTIVE" className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Select className="modal-custom-select" placeholder="Chọn trạng thái">
                {officeStatusOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="phoneNumber" label={<span className="modal-lable">Số điện thoại</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <Input className="modal-input" placeholder="Nhập số điện thoại" />
            </Form.Item>

            <Form.Item name="email" label={<span className="modal-lable">Email</span>} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Input className="modal-input" type="email" placeholder="contact@gmail.com" />
            </Form.Item>

            <Form.Item name="openingTime" label={<span className="modal-lable">Giờ mở cửa</span>} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <Input className="modal-input" placeholder="07:00:00" />
            </Form.Item>

            <Form.Item name="closingTime" label={<span className="modal-lable">Giờ đóng cửa</span>} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Input className="modal-input" placeholder="17:00:00" />
            </Form.Item>

            <Form.Item name="capacity" label={<span className="modal-lable">Sức chứa</span>} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <InputNumber className="modal-input" min={0} style={{ width: "100%" }} placeholder="Nhập sức chứa" />
            </Form.Item>

            <Form.Item name="postalCode" label={<span className="modal-lable">Mã bưu chính</span>} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Input className="modal-input" placeholder="Ví dụ: 700000" />
            </Form.Item>

            <Form.Item name="cityCode" label={<span className="modal-lable">Tỉnh/Thành phố</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <Select
                className="modal-custom-select"
                showSearch
                placeholder="Chọn tỉnh/thành"
                optionFilterProp="children"
                onChange={(val: number) => onCityChange(val)}
                filterOption={(input, option) => (option?.children as unknown as string).toLowerCase().includes(input.toLowerCase())}
              >
                {cities.map((c) => (
                  <Select.Option key={c.code} value={c.code}>
                    {c.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="wardCode" label={<span className="modal-lable">Phường/xã</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Select
                className="modal-custom-select"
                showSearch
                placeholder="Chọn phường/xã"
                optionFilterProp="children"
                filterOption={(input, option) => (option?.children as unknown as string).toLowerCase().includes(input.toLowerCase())}
              >
                {wards.map((w) => (
                  <Select.Option key={w.code} value={w.code}>
                    {w.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="detailAddress" label={<span className="modal-lable">Địa chỉ chi tiết</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 3" }}>
              <Input className="modal-input" placeholder="Số nhà, đường..." />
            </Form.Item>

            <Form.Item name="latitude" label={<span className="modal-lable">Vĩ độ</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "1 / 2" }}>
              <InputNumber className="modal-input" style={{ width: "100%" }} placeholder="Ví dụ: 10.762622" />
            </Form.Item>

            <Form.Item name="longitude" label={<span className="modal-lable">Kinh độ</span>} rules={[{ required: true }]} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <InputNumber className="modal-input" style={{ width: "100%" }} placeholder="Ví dụ: 106.660172" />
            </Form.Item>

            <Form.Item name="notes" label={<span className="modal-lable">Ghi chú</span>} style={{ gridColumn: "1 / 3" }}>
              <Input.TextArea className="modal-textarea" autoSize={{ minRows: 2, maxRows: 5 }} placeholder="Nhập ghi chú (tùy chọn)" />
            </Form.Item>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default AddEditPostOfficeModal;
