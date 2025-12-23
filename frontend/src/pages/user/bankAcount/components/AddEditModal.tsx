import React from 'react';
import { Modal, Form, Input, Switch, AutoComplete, message } from 'antd';
import type { BankAccountRequest } from '../../../../types/bankAccount';

interface AddEditModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  account: BankAccountRequest;
  onOk: () => void;
  onCancel: () => void;
  onBankAccountChange: (account: BankAccountRequest) => void;
  form: any;
  bankNames: string[];
  total: number;
  loading: boolean;
}

const AddEditModal: React.FC<AddEditModalProps> = ({
  open,
  mode,
  account,
  onOk,
  onCancel,
  onBankAccountChange,
  form,
  bankNames,
  total,
  loading,
}) => (
  <Modal
    title={
      <span className='modal-title'>
        {mode === 'edit' ? 'Chỉnh sửa tài khoản' : 'Thêm tài khoản mới'}
      </span>}
    open={open}
    onOk={onOk}
    onCancel={onCancel}
    okText={mode === 'edit' ? 'Cập nhật' : 'Thêm'}
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
        label={<span className="modal-lable">Tên ngân hàng</span>}
        name="bankName"
        rules={[{ required: true, message: 'Nhập tên ngân hàng!' }]}
      >
        <AutoComplete
          className="modal-custom-select"
          options={bankNames.map((name) => ({ value: name }))}
          placeholder="Chọn tên ngân hàng"
          value={account.bankName}
          filterOption={(inputValue, option) =>
            option!.value.toUpperCase().includes(inputValue.toUpperCase())
          }
          onChange={(value) => onBankAccountChange({ ...account, bankName: value })}
        />
      </Form.Item>

      <Form.Item
        label={<span className="modal-lable">Số tài khoản</span>}
        name="accountNumber"
        rules={[
          { required: true, message: 'Nhập số tài khoản!' },
          {
            validator: (_, value) => {
              if (!value || /^[0-9]+$/.test(value)) return Promise.resolve();
              return Promise.reject(new Error('Số tài khoản chỉ được chứa số!'));
            },
          },
        ]}
      >
        <Input
          className="modal-custom-input"
          placeholder="Nhập số tài khoản"
          onChange={(e) => onBankAccountChange({ ...account, accountNumber: e.target.value })}
        />
      </Form.Item>

      <Form.Item
        label={<span className="modal-lable">Tên chủ tài khoản</span>}
        name="accountName"
        rules={[{ required: true, message: 'Nhập tên chủ tài khoản!' }]}
      >
        <Input
          className="modal-custom-input"
          placeholder="Nhập tên chủ tài khoản"
          onChange={(e) => {
            const upper = e.target.value.toLocaleUpperCase('vi-VN');
            form.setFieldsValue({ accountName: upper });
            onBankAccountChange({ ...account, accountName: upper });
          }}
        />
      </Form.Item>

      <Form.Item
        label={<span className="modal-lable">Đặt mặc định</span>}
        name="isDefault" valuePropName="checked">
        <Switch
          className="custom-switch"
          disabled={(mode === 'edit' && account.isDefault) || total === 0}
          checked={total === 0 ? true : account.isDefault}
          onChange={(val) => {
            if ((mode === 'edit' && account.isDefault && !val) || total === 0) {
              message.warning('Vui lòng chọn 1 tài khoản mặc định khác');
              return;
            }
            onBankAccountChange({ ...account, isDefault: val });
          }}
        />
      </Form.Item>

      <Form.Item
        label={<span className="modal-lable">Ghi chú</span>}
        name="notes">
        <Input.TextArea
          className="modal-custom-input-textarea"
          rows={3}
          placeholder="Nhập ghi chú"
          onChange={(e) => onBankAccountChange({ ...account, notes: e.target.value })}
        />
      </Form.Item>
    </Form>
  </Modal>
);

export default AddEditModal;