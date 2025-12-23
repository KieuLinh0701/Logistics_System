import React, { useEffect, useState } from 'react';
import { Modal, Form, Select, message, Button } from 'antd';
import dayjs from 'dayjs';
import type { ManagerShipment, ManagerShipmentAddEditRequest } from '../../../../types/shipment';
import type { Vehicle } from '../../../../types/vehicle';
import type { ManagerEmployee } from '../../../../types/employee';
import type { Office } from '../../../../types/office';
import vehicleApi from '../../../../api/vehicleApi';
import { translateVehicleType } from '../../../../utils/vehicleUtils';
import OfficeSelectModal from './OfficeSelectModal';
import { DeleteOutlined } from '@ant-design/icons';
import { SHIPMENT_TYPES, translateShipmentType } from '../../../../utils/shipmentUtils';
import SelectEmployeeModal from './SelectEmployeeModal';
import shipmentApi from '../../../../api/shipmentApi';

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

  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);
  const [selectedOffice, setSelectedOffice] = useState<Office | null>(null);
  const [officeModalOpen, setOfficeModalOpen] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState<ManagerEmployee | null>(null);
  const [employeeModalOpen, setEmployeeModalOpen] = useState(false);
  const [vehicleDropdownOpen, setVehicleDropdownOpen] = useState(false);

  const selectedType = Form.useWatch('type', form);

  useEffect(() => {
    if (!selectedType) return;

    setSelectedVehicle(null);
    form.setFieldsValue({ vehicleId: null });

    if (mode !== 'edit') {
      setSelectedEmployee(null);
      form.setFieldsValue({ employeeId: null });

      setSelectedOffice(null);
      form.setFieldsValue({ toOfficeId: null });
    }

    console.log("selectedType", selectedType);
  }, [selectedType, mode]);

  useEffect(() => {
    if (!open) return;

    if (mode === 'edit' && shipment) {
      // Set form values trực tiếp
      form.setFieldsValue({
        type: shipment.type || undefined,
        code: shipment.code,
        vehicleId: shipment.vehicle?.id || null,
        startTime: shipment.startTime ? dayjs(shipment.startTime) : null,
        endTime: shipment.endTime ? dayjs(shipment.endTime) : null,
        fromOfficeId: shipment.fromOffice?.id || null,
        toOfficeId: shipment.toOffice?.id || null,
        employeeId: shipment.employee?.id || null,
      });

      setSelectedVehicle(shipment.vehicle || null);
      setSelectedEmployee(shipment.employee || null);
      setSelectedOffice(shipment.toOffice || null);
    } else {
      // Tạo mới
      form.resetFields();
      setSelectedVehicle(null);
      setSelectedEmployee(null);
      setSelectedOffice(null);
    }

    fetchSelectData();
  }, [open, shipment, mode, form]);

  useEffect(() => {
    if (mode === 'edit' && shipment && vehicles.length > 0) {
      const vehicle = vehicles.find(v => v.id === shipment.vehicle?.id) || null;
      setSelectedVehicle(vehicle);
      form.setFieldsValue({ vehicleId: vehicle?.id || null });
    }
  }, [vehicles, shipment, mode, form]);

  const fetchSelectData = async () => {
    try {
      const vehicleRes = await vehicleApi.getAvailableVehicles();

      setVehicles(vehicleRes.data || []);
    } catch (error: any) {
      message.error(error.message || 'Lấy dữ liệu chọn thất bại!');
    }
  };

  const handleSubmit = async () => {
    try {
      setLoading(true);

      const values = await form.validateFields();

      if (values.type === "DELIVERY" && selectedOffice) {
        message.warning("Chuyến giao hàng của shipper không cần chọn bưu cục");
        return;
      }

      const param: ManagerShipmentAddEditRequest = {
        type: values.type,

        vehicleId: selectedVehicle
          ? selectedVehicle.id
          : undefined,

        toOfficeId: selectedOffice
          ? selectedOffice.id
          : undefined,

        employeeId: selectedEmployee
          ? selectedEmployee.id
          : undefined,
      };

      console.log("shipment", param);

      let result = null;
      let mess = null;
      let err = null;
      if (mode === "create") {
        result = await shipmentApi.createManagerShipment(param);
        mess = "Tạo chuyến hàng thành công";
        err = "Tạo chuyến hàng thất bại";
      } else {
        result = await shipmentApi.updateManagerShipment(shipment!.id!, param);
        mess = "Cập nhật chuyến hàng thành công";
        err = "Cập nhật chuyến hàng thất bại";
      }

      if (result.success && result.data) {
        message.success(result.message || mess)
        onSuccess();
        onCancel();
      } else {
        message.error(result.message || err);
      }

    } catch (error: any) {
      message.error(error.message || "Có lỗi xảy ra vui lòng thử lại sau!");
    } finally {
      setLoading(false);
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

        <Form.Item
          name="type"
          label={<span className="modal-lable">Loại</span>}
          rules={[{ required: true, message: 'Chọn loại chuyến giao hàng!' }]}>
          <Select
            className="modal-custom-select"
            placeholder="Chọn loại..."
          >
            {SHIPMENT_TYPES.map((t) => (
              <Select.Option key={t} value={t}>
                {translateShipmentType(t)}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

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
            open={vehicleDropdownOpen}
            onOpenChange={(open) => {
              if (open) {
                if (!selectedType) {
                  message.warning("Vui lòng chọn loại chuyến trước");
                  setVehicleDropdownOpen(false);
                } else if (selectedType === "DELIVERY") {
                  message.warning("Chuyến giao hàng không cần chọn phương tiện");
                  setVehicleDropdownOpen(false);
                } else {
                  setVehicleDropdownOpen(true);
                }
              } else {
                setVehicleDropdownOpen(false);
              }
            }}
          >
            {vehicles.map((v) => (
              <Select.Option key={v.id} value={v.id} label={v.licensePlate}>
                <div className="shipment-add-edit-select-contain">
                  <span className="shipment-add-edit-select-name">{v.licensePlate}</span>
                  <span className="shipment-add-edit-select-extra"><strong>Sức chứa:</strong> {v.capacity} Kg</span>
                  <span className="shipment-add-edit-select-extra"><strong>Loại:</strong> {translateVehicleType(v.type)}</span>
                </div>
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item label={<span className="modal-lable">Bưu cục đến</span>}>
          <div className='shipment-add-edit-display-item-selected'>
            <Button
              className='modal-cancel-button'
              onClick={() => {
                if (selectedType === "DELIVERY") {
                  message.warning("Chuyến giao hàng của nhân viên giao hàng không cần chọn bưu cục");
                  return;
                }

                if (!selectedType) {
                  message.warning("Vui lòng chọn loại chuyến trước");
                  return;
                }
                setOfficeModalOpen(true);
              }}
            >
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
                    Giờ làm việc: {selectedOffice.openingTime || "Unknown"} - {selectedOffice.closingTime || "Unknown"}
                  </span>
                </div>
              </div>
            )}
          </div>
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Nhân viên giao hàng</span>}
        >
          <div className='shipment-add-edit-display-item-selected'>
            <Button
              className='modal-cancel-button'
              onClick={() => {
                if (!selectedType) {
                  message.warning("Vui lòng chọn loại chuyến trước");
                  return;
                }
                setEmployeeModalOpen(true);
              }}
            >
              {selectedEmployee ? "Đổi nhân viên" : "Chọn nhân viên"}
            </Button>

            {selectedEmployee && (
              <div className="shipment-add-edit-selected-office">
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
                      onClick={() => {
                        setSelectedEmployee(null);
                        form.setFieldsValue({ employeeId: null });
                      }}
                      icon={<DeleteOutlined />}
                    />
                  </div>

                  <span className="shipment-add-edit-select-extra">
                    Mã NV: {selectedEmployee.code}
                  </span>
                  <span className="shipment-add-edit-select-extra">
                    Email: {selectedEmployee.email || "N/A"}
                  </span>
                  <span className="shipment-add-edit-select-extra">
                    SĐT: {selectedEmployee.phoneNumber || "N/A"}
                  </span>
                </div>
              </div>
            )}
          </div>
        </Form.Item>
      </Form>

      <OfficeSelectModal
        open={officeModalOpen}
        value={selectedOffice}
        onSelect={(office) => {
          setSelectedOffice(office);
          form.setFieldsValue({ toOfficeId: office.id });
          setOfficeModalOpen(false);
        }}
        onCancel={() => setOfficeModalOpen(false)}
      />

      <SelectEmployeeModal
        open={employeeModalOpen}
        role={selectedType}
        value={selectedEmployee}
        onSelect={(employee) => {
          setSelectedEmployee(employee);
          form.setFieldsValue({ employeeId: employee.id });
          setEmployeeModalOpen(false);
        }}
        onCancel={() => setEmployeeModalOpen(false)}
      />

    </Modal>
  );
};

export default AddEditShipmentModal;