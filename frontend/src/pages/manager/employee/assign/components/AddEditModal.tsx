import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, DatePicker, Button } from 'antd';
import type { ManagerShipperAssignment } from '../../../../../types/shipperAssignment';
import locationApi from '../../../../../api/locationApi';
import dayjs from 'dayjs';
import { DeleteOutlined, UserOutlined } from '@ant-design/icons';
import type { ManagerEmployee } from '../../../../../types/employee';

const { Option } = Select;

interface AddEditModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  assign: Partial<ManagerShipperAssignment>;
  onOk: (values: Partial<ManagerShipperAssignment>) => void;
  onCancel: () => void;
  loading: boolean;
  cityCode: number;
  selectedEmployee: ManagerEmployee | null;
  onSelectEmployee: () => void;
  onClearEmployee: () => void;
}

const AddEditModal: React.FC<AddEditModalProps> = ({
  open,
  mode,
  assign,
  onOk,
  onCancel,
  loading,
  cityCode,
  selectedEmployee,
  onSelectEmployee,
  onClearEmployee
}) => {
  const [wards, setWards] = useState<{ value: number; label: string }[]>([]);
  const [form] = Form.useForm();

  useEffect(() => {
    if (open) {
      form.setFieldsValue({
        wardCode: assign.wardCode,
        startAt: assign.startAt ? dayjs(assign.startAt) : undefined,
        endAt: assign.endAt ? dayjs(assign.endAt) : undefined,
        notes: assign.notes,
      });
    }
  }, [open, assign]);

  useEffect(() => {
    const fetchWards = async () => {
      if (!cityCode) return;
      try {
        const wardList = await locationApi.getWardsByCity(cityCode);
        setWards(wardList.map((w: any) => ({ value: w.code, label: w.name })));
      } catch (err) {
        console.error("Lỗi lấy phường/xã:", err);
      }
    };
    fetchWards();
  }, [cityCode]);

  const handleOk = async () => {
    const values = await form.validateFields();
    onOk(values);
  };

  const canEditStartTime =
    mode === 'create' ||
    (
      assign?.startAt &&
      dayjs(assign.startAt).isValid() &&
      dayjs(assign.startAt).isAfter(dayjs())
    );

  return (
    <Modal
      open={open}
      onOk={handleOk}
      onCancel={onCancel}
      okButtonProps={{ className: "modal-ok-button", loading }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      okText={mode === "edit" ? "Cập nhật" : "Thêm phân công"}
      title={
        <span className='modal-title'>
          {mode === 'edit' ? `Chỉnh sửa phân công nhân viên` : 'Thêm phân công mới'}
        </span>
      }
      width={700}
      className="modal-hide-scrollbar"
      zIndex={1000}
    >
      <Form form={form} layout="vertical">
        {canEditStartTime && (
          <Form.Item
            label={<span className="modal-lable">Nhân viên giao hàng</span>}
            name="selectedEmployee"
            rules={[
              {
                validator: () => {
                  if (selectedEmployee) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error("Chọn nhân viên giao hàng!"));
                },
              },
            ]}
          >
            <div className='manager-shipper-assigns-contain-select'>
              <Button
                icon={<UserOutlined />}
                className="modal-cancel-button"
                onClick={onSelectEmployee}
              >
                Chọn nhân viên
              </Button>

              {selectedEmployee && (
                <div className="shipment-add-edit-selected-office" style={{ flex: 1 }}>
                  <div className="shipment-add-edit-select-contain">
                    <div className="shipment-add-edit-selected-header">
                      <span className="shipment-add-edit-select-name">
                        {selectedEmployee.lastName} {selectedEmployee.firstName}
                      </span>
                      <Button
                        type="text"
                        danger
                        size="small"
                        className="shipment-add-edit-remove-btn"
                        onClick={() => onClearEmployee()}
                        icon={<DeleteOutlined />}
                      />
                    </div>
                    <span>Số điện thoại: {selectedEmployee.phoneNumber || "Unknown"}</span>
                    <span>Email: {selectedEmployee.email || "Unknown"}</span>
                  </div>
                </div>
              )}
            </div>
          </Form.Item>
        )}

        {canEditStartTime && (
          <Form.Item
            label={<span className="modal-lable">Phường/Xã</span>}
            name="wardCode"
            rules={[{ required: true, message: "Chọn phường/xã!" }]}
          >
            <Select
              className="modal-custom-select"
              placeholder="Chọn phường/xã"
              showSearch
              optionFilterProp="label"
              filterOption={(input, option) =>
                (option?.label as string).toLowerCase().includes(input.toLowerCase())
              }
            >
              {wards.map((w) => (
                <Option key={w.value} value={w.value} label={w.label}>
                  {w.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
        )}

        {canEditStartTime && (
          <Form.Item
            label={<span className="modal-lable">Thời gian bắt đầu</span>}
            name="startAt"
            rules={[{ required: true, message: "Chọn thời gian bắt đầu!" }]}
          >
            <DatePicker
              showTime
              className="modal-custom-date-picker"
              placeholder="Chọn thời gian bắt đầu..."
            />
          </Form.Item>
        )}

        <Form.Item
          label={<span className="modal-lable">Thời gian kết thúc</span>}
          name="endAt">
          <DatePicker
            showTime
            className="modal-custom-date-picker"
            style={{ width: "100%" }}
            placeholder="Chọn thời gian kết thúc..."
          />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Ghi chú</span>}
          name="notes">
          <Input.TextArea
            className="modal-custom-input-textarea"
            placeholder="Nhập ghi chú..."
            autoSize={{ minRows: 2, maxRows: 6 }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AddEditModal;