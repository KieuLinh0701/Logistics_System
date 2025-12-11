import React from 'react';
import { Modal, Form, Input, Switch, message } from 'antd';
import type { AddressRequest } from '../../../../../../types/address';
import AddressForm from '../../../../../../components/common/AdressForm';

interface AddressModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  address: AddressRequest;
  onOk: () => void;
  onCancel: () => void;
  onAddressChange: (address: AddressRequest) => void;
  form: any;
  total: number;
  loading: boolean;
}

const AddressModal: React.FC<AddressModalProps> = ({
  open,
  mode,
  address,
  onOk,
  onCancel,
  onAddressChange,
  form,
  total,
  loading,
}) => {

  return (
    <Modal
      title={<span className='modal-title'>{mode === 'edit' ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới'}</span>}
      open={open}
      onOk={onOk}
      onCancel={onCancel}
      okText={mode === 'edit' ? 'Cập nhật' : 'Thêm'}
      okButtonProps={{ className: "modal-ok-button", loading }}
      cancelButtonProps={{ className: "modal-cancel-button" }}
      cancelText="Hủy"
      className="modal-hide-scrollbar"
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          name: address.name,
          phoneNumber: address.phoneNumber,
          address: {
            cityCode: address.cityCode !== 0 ? address.cityCode : undefined,
            wardCode: address.wardCode !== 0 ? address.wardCode : undefined,
            detail: address.detail,
          },
          isDefault: address.isDefault,
        }}
      >
        <Form.Item
          label={<span className="modal-lable">Tên</span>}
          name="name"
          rules={[{ required: true, message: 'Vui lòng nhập tên!' }]}
        >
          <Input
            className="modal-custom-input"
            placeholder="Nhập tên"
            onChange={(e) => onAddressChange({ ...address, name: e.target.value })}
          />
        </Form.Item>

        <Form.Item
          label={<span className="modal-lable">Số điện thoại</span>}
          name="phoneNumber"
          rules={[
            { required: true, message: 'Vui lòng nhập số điện thoại!' },
            { pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại không hợp lệ!' },
          ]}
        >
          <Input
            className="modal-custom-input"
            placeholder="Nhập số điện thoại"
            onChange={(e) => onAddressChange({ ...address, phoneNumber: e.target.value })}
          />
        </Form.Item>

        <AddressForm
          form={form}
          prefix='address'
        />

        <Form.Item
          label={<span className="modal-lable">Đặt mặc định</span>}
          name="isDefault"
          valuePropName="checked"
        >
          <Switch
            className="custom-switch"
            disabled={(mode === 'edit' && address.isDefault) || total === 0}
            checked={total === 0 ? true : address.isDefault}
            onChange={(val) => {
              if ((mode === 'edit' && address.isDefault && !val) || total === 0) {
                message.warning('Vui lòng chọn 1 địa chỉ mặc định khác');
                return;
              }
              onAddressChange({ ...address, isDefault: val });
            }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default AddressModal;