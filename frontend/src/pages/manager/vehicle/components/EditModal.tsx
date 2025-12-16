import React from 'react';
import { Modal, Form, Input, Select, DatePicker } from 'antd';
import type { Vehicle } from '../../../../types/vehicle';
import { translateVehicleStatus, VEHICLE_STATUSES } from '../../../../utils/vehicleUtils';

const { TextArea } = Input;

interface EditModalProps {
  open: boolean;
  vehicle: Partial<Vehicle>;
  onOk: () => void;
  onCancel: () => void;
  loading: boolean;
  onVehicleChange: (vehicle: Partial<Vehicle>) => void;
  form: any;
}

const EditModal: React.FC<EditModalProps> = ({
  open,
  vehicle,
  onOk,
  onCancel,
  loading,
  onVehicleChange,
  form,
}) => {

  return (
    <Modal
      title={
        <span className='modal-title'>
          Chỉnh sửa phương tiện
        </span>
      }
      open={open}
      onOk={onOk}
      onCancel={onCancel}
      okText={'Cập nhật'}
      okButtonProps={{
        className: "modal-ok-button",
        loading: loading
      }}
      cancelButtonProps={{
        className: "modal-cancel-button"
      }}
      cancelText="Hủy"
      className="modal-hide-scrollbar"
    >
      <Form form={form} layout="vertical">

        <Form.Item
          label={<span className="modal-lable">Trạng thái</span>}
          name="status"
        >
          <Select
            className='modal-custom-select'
            value={vehicle.status}
            onChange={(val) => onVehicleChange({
              ...vehicle,
              status: val
            })}
            placeholder="Chọn trạng thái..."
          >
            {VEHICLE_STATUSES.map((s) => (
              <Select.Option key={s} value={s}>
                {translateVehicleStatus(s)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Mô tả</span>}
          name="description">
          <TextArea
            className='modal-custom-input-textarea'
            rows={3}
            value={vehicle.description}
            onChange={(e) => onVehicleChange({ ...vehicle, description: e.target.value })}
            placeholder="Ghi chú về phương tiện..."
          />
        </Form.Item>

        <Form.Item
          label="Ngày bảo trì tiếp theo"
          name="nextMaintenanceDue"
        >
          <DatePicker
            className="modal-custom-date-picker"
            showTime
            format="HH:mm:ss DD-MM-YYYY"
            placeholder="Chọn ngày và giờ bảo trì..."
          />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Mã thiết bị GPS</span>}
          name="gpsDeviceId">
          <Input
            className='modal-custom-input'
            value={vehicle.gpsDeviceId}
            onChange={(e) => onVehicleChange({ ...vehicle, gpsDeviceId: e.target.value })}
            placeholder="Mã thiết bị GPS..."
          />
        </Form.Item>

      </Form>
    </Modal>
  );
};

export default EditModal;