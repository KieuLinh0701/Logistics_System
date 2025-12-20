import React, { useEffect, useState } from 'react';
import { Modal, Form, Select, DatePicker, message, Button } from 'antd';
import dayjs from 'dayjs';
import type { ManagerShipment } from '../../../../types/shipment';
import type { Vehicle } from '../../../../types/vehicle';
import type { ManagerEmployee } from '../../../../types/employee';
import type { Office } from '../../../../types/office';
import vehicleApi from '../../../../api/vehicleApi';
import { translateVehicleType } from '../../../../utils/vehicleUtils';
import OfficeSelectModal from './OfficeSelectModal';
import { DeleteOutlined } from '@ant-design/icons';

interface AddEditShipmentModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  shipment?: Partial<ManagerShipment>;
  onSuccess: () => void;
  onCancel: () => void;
}

const AddEditShipmentModal: React.FC<AddEditShipmentModalProps> = ({
  open,
  mode,
  shipment,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [employees, setEmployees] = useState<ManagerEmployee[]>([]);
  const [offices, setOffices] = useState<Office[]>([]);

  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<ManagerEmployee | null>(null);
  const [selectedOrders, setSelectedOrders] = useState<number[]>([]);
  const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);
  const [officeModalOpen, setOfficeModalOpen] = useState(false);

  useEffect(() => {
    if (open) {
      form.resetFields();
      fetchSelectData();

      if (mode === 'edit' && shipment) {
        form.setFieldsValue({
          code: shipment.code,
          vehicleId: shipment.vehicle?.id || null,
          startTime: shipment.startTime ? dayjs(shipment.startTime) : null,
          endTime: shipment.endTime ? dayjs(shipment.endTime) : null,
          fromOfficeId: shipment.fromOffice?.id || null,
          toOfficeId: shipment.toOffice?.id || null,
        });
        setSelectedVehicle(shipment.vehicle || null);
        setSelectedEmployee(shipment.employee || null);
        setSelectedOrders(shipment.orders?.map(o => o.id) || []);
      }
    }
  }, [open, shipment, mode, form]);

  const fetchSelectData = async () => {
    try {
      // const [vehicleRes, employeeRes, officeRes] = await Promise.all([
      //   vehicleApi.getVehicles(),
      //   employeeApi.getEmployees(),
      //   officeApi.getOffices(),
      // ]);

      const vehicleRes = await vehicleApi.getAvailableVehicles();

      setVehicles(vehicleRes.data || []);
      // setEmployees(employeeRes.data || []);
      // setOffices(officeRes.data || []);
    } catch (error: any) {
          message.error(error.message || 'Lấy dữ liệu chọn thất bại!');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      // const payload: Partial<ManagerShipment> = {
      //   vehicle: selectedVehicle ? { id: selectedVehicle } : null,
      //   startTime: values.startTime ? values.startTime.toISOString() : null,
      //   endTime: values.endTime ? values.endTime.toISOString() : null,
      //   fromOffice: values.fromOfficeId ? { id: values.fromOfficeId } : null,
      //   toOffice: values.toOfficeId ? { id: values.toOfficeId } : null,
      //   employee: selectedEmployees[0] ? { id: selectedEmployees[0].id } : null,
      //   shipmentOrders: selectedOrders.map(id => ({ orderId: id })),
      // };

      if (mode === 'create') {
        // await shipmentApi.createShipment(payload);
        // message.success('Tạo chuyến thành công');
      } else {
        // await shipmentApi.updateShipment(shipment!.id!, payload);
        // message.success('Cập nhật chuyến thành công');
      }

      onSuccess();
      onCancel();
    } catch (error: any) {
          message.error(error.message || 'Có lỗi xảy ra!');
    }
  };

  return (
    <Modal
      open={open}
      title={
        <span className='modal-title'>
          {mode === 'edit' ? `Chỉnh sửa chuyến #${shipment?.code}` : 'Tạo chuyến hàng mới'}
        </span>
      }
      onOk={handleSubmit}
      onCancel={onCancel}
      okButtonProps={{
        className: "modal-ok-button",
        loading: loading
      }}
      cancelButtonProps={{
        className: "modal-cancel-button"
      }}
      className="modal-hide-scrollbar"
      okText={mode === 'edit' ? 'Cập nhật' : 'Tạo mới'}
    >
      <Form form={form} layout="vertical">

        <Form.Item label={<span className="modal-lable">Chọn Vehicle</span>} name="vehicleId">
          <Select
            className='modal-custom-select'
            placeholder="Chọn vehicle..."
            value={selectedVehicle?.id || undefined}
            onChange={(value) => {
              const v = vehicles.find(v => v.id === value) || null;
              setSelectedVehicle(v);
            }}
            allowClear
            onClear={() => setSelectedVehicle(null)}
            showSearch
            optionLabelProp="label"
            filterOption={(input, option) =>
              (option?.label as string)?.toLowerCase().includes(input.toLowerCase())
            }
          >
            {vehicles.map((v) => (
              <Select.Option
                key={v.id}
                value={v.id}
                label={v.licensePlate}
              >
                <div className="shipment-add-edit-select-contain">
                  <span className="shipment-add-edit-select-name">{v.licensePlate}</span>
                  <span className="shipment-add-edit-select-extra"><strong>Sức chứa:</strong> {v.capacity}</span>
                  <span className="shipment-add-edit-select-extra"><strong>Loại:</strong> {translateVehicleType(v.type)}</span>
                </div>
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Thời gian bắt đầu</span>}
          name="startTime">
          <DatePicker
            className="modal-custom-date-picker"
            showTime />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Thời gian kết thúc</span>}
          name="endTime">
          <DatePicker
            showTime
            className="modal-custom-date-picker" />
        </Form.Item>

        <Form.Item label={<span className="modal-lable">Bưu cục đến</span>}>
          <div className='shipment-add-edit-display-item-selected'>
            <Button
              className='modal-cancel-button'
              onClick={() => setOfficeModalOpen(true)}>
              {selectedOffice ? "Đổi bưu cục" : "Chọn bưu cục"}
            </Button>

            {selectedOffice && (
              <div className="shipment-add-edit-selected-office">
                <div className="shipment-add-edit-select-contain">
                  <div className="shipment-add-edit-selected-header">
                    <span className="shipment-add-edit-select-name">{selectedOffice.name}</span>
                    <Button
                      type="text"
                      danger
                      size="small"
                      className="shipment-add-edit-remove-btn"
                      onClick={() => setSelectedOffice(null)}
                      icon={<DeleteOutlined />}
                    />
                  </div>
                  <span className="shipment-add-edit-select-extra">Địa chỉ: {selectedOffice.detail}</span>
                  <span className="shipment-add-edit-select-extra">
                    Liên hệ: {selectedOffice.email || "Unknown"} - {selectedOffice.phoneNumber || "Unknown"}
                  </span>
                  <span className="shipment-add-edit-select-extra">
                    Giờ làm việc: {selectedOffice.openingTime} - {selectedOffice.closingTime}
                  </span>
                </div>
              </div>
            )}
          </div>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Nhân viên giao hàng</span>}
          name="employeeId"
          rules={[{ required: true, message: 'Chọn nhân viên giao hàng!' }]}
        >
          <Select
            className='modal-custom-select'
            placeholder="Chọn nhân viên..."
            allowClear
            onChange={setSelectedEmployee}
            onClear={() => setSelectedEmployee(null)}
          >
            {employees.map(emp => (
              <Select.Option key={emp.id} value={emp.id}>
                {emp.lastName} {emp.firstName}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        {/* <Form.Item
          label={<span className="modal-lable">Đơn hàng gán vào</span>}
        >
          {selectedOrders.map(orderId => (
            <div key={orderId} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span>Đơn #{orderId}</span>
              <Button size="small" onClick={() => setSelectedOrders(prev => prev.filter(id => id !== orderId))}>
                Xóa
              </Button>
            </div>
          ))}
          <Button onClick={() => message.info('Mở form chọn đơn hàng')}>Thêm đơn hàng</Button>
        </Form.Item> */}
      </Form>

      <OfficeSelectModal
        open={officeModalOpen}
        onSelect={(office) => {
          setSelectedOffice(office);
          form.setFieldsValue({ toOfficeId: office.id });
          setOfficeModalOpen(false);
        }}
        onCancel={() => setOfficeModalOpen(false)}
      />
    </Modal>
  );
};

export default AddEditShipmentModal;