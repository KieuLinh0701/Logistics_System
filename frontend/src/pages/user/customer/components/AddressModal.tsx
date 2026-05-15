import React from 'react';
import { Modal, Form, Input } from 'antd';
import type {RecipientAddressRequest} from "../../../../types/recipientAddress.ts";
import AddressForm from "../../../../components/common/AdressForm.tsx";

interface AddressModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  address: RecipientAddressRequest;
  onOk: () => void;
  onCancel: () => void;
  onAddressChange: (address: RecipientAddressRequest) => void;
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
      </Form>
    </Modal>
  );
};

export default AddressModal;