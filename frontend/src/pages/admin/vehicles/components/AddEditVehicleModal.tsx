import React from "react";
import type {FormInstance} from "antd";
import {Form, Input, InputNumber, Modal, Select} from "antd";
import type {AdminVehicle} from "../../../../types/vehicle";
import "../../../hr/recruitment/job-posting/components/JobPostingComponents.css";

interface Option {
  label: string;
  value: string;
}

interface OfficeOption {
  id: number;
  name: string;
}

interface AddEditVehicleModalProps {
  open: boolean;
  editingVehicle: AdminVehicle | null;
  form: FormInstance;
  submitting: boolean;
  typeOptions: Option[];
  statusOptions: Option[];
  offices: OfficeOption[];
  onCancel: () => void;
  onSubmit: () => void;
  onValuesChange: () => void;
}

const AddEditVehicleModal: React.FC<AddEditVehicleModalProps> = ({
  open,
  editingVehicle,
  form,
  submitting,
  typeOptions,
  statusOptions,
  offices,
  onCancel,
  onSubmit,
  onValuesChange,
}) => {
  return (
    <Modal
      className="modal-hide-scrollbar hr-job-posting-modal"
      width={920}
      bodyStyle={{ minHeight: 340 }}
      title={<div className="modal-title">{editingVehicle ? "Cập nhật phương tiện" : "Thêm phương tiện mới"}</div>}
      open={open}
      onCancel={onCancel}
      onOk={onSubmit}
      okText={editingVehicle ? "Cập nhật" : "Tạo mới"}
      confirmLoading={submitting}
      destroyOnClose
      okButtonProps={{ className: "modal-ok-button" }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
    >
      <Form className="hr-job-posting-form" form={form} layout="vertical" onValuesChange={onValuesChange}>
        <div className="modal-section">
          <div className="modal-grid">
            {!editingVehicle && (
              <Form.Item
                name="licensePlate"
                label={<span className="modal-lable">Biển số xe</span>}
                rules={[{ required: true, message: "Vui lòng nhập biển số xe" }]}
                className="form-item"
                style={{ gridColumn: "1 / 3" }}
              >
                <Input className="modal-input" placeholder="Nhập biển số xe" />
              </Form.Item>
            )}

            <Form.Item
              name="type"
              label={<span className="modal-lable">Loại xe</span>}
              rules={[{ required: true, message: "Vui lòng chọn loại xe" }]}
              className="form-item"
              style={{ gridColumn: "1 / 2" }}
            >
              <Select className="modal-custom-select" placeholder="Chọn loại xe">
                {typeOptions.map((option) => (
                  <Select.Option key={option.value} value={option.value}>
                    {option.label}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="status"
              label={<span className="modal-lable">Trạng thái</span>}
              rules={[{ required: true, message: "Vui lòng chọn trạng thái" }]}
              className="form-item"
              style={{ gridColumn: "2 / 3" }}
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
              name="capacity"
              label={<span className="modal-lable">Tải trọng (kg)</span>}
              rules={[{ required: true, message: "Vui lòng nhập tải trọng" }]}
              className="form-item"
              style={{ gridColumn: "1 / 2" }}
            >
              <InputNumber className="modal-input" min={0} style={{ width: "100%" }} placeholder="Nhập tải trọng" />
            </Form.Item>

            <Form.Item name="officeId" label={<span className="modal-lable">Bưu cục</span>} className="form-item" style={{ gridColumn: "2 / 3" }}>
              <Select className="modal-custom-select" showSearch placeholder="Chọn bưu cục" optionFilterProp="children" allowClear>
                {offices.map((office) => (
                  <Select.Option key={office.id} value={office.id}>
                    {office.name}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            {editingVehicle && (
              <>
                <Form.Item name="gpsDeviceId" label={<span className="modal-lable">Thiết bị GPS</span>} className="form-item" style={{ gridColumn: "1 / 2" }}>
                  <Input className="modal-input" placeholder="ID thiết bị GPS" disabled />
                </Form.Item>

                <Form.Item
                  name="nextMaintenanceDue"
                  label={<span className="modal-lable">Ngày bảo trì tiếp theo</span>}
                  className="form-item"
                  style={{ gridColumn: "2 / 3" }}
                >
                  <Input className="modal-input" placeholder="Ngày bảo trì tiếp theo" disabled />
                </Form.Item>
              </>
            )}

            <Form.Item name="description" label={<span className="modal-lable">Mô tả</span>} style={{ gridColumn: "1 / 3" }}>
              <Input.TextArea className="modal-textarea" autoSize={{ minRows: 2, maxRows: 5 }} placeholder="Nhập mô tả (tùy chọn)" />
            </Form.Item>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default AddEditVehicleModal;
