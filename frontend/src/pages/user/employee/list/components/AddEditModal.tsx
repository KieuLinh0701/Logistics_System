import React from 'react';
import {Modal, Form, Input, Select} from 'antd';
import type {Role} from "../../../../../types/role.ts";

interface AddEditModalProps {
    open: boolean;
    mode: 'create' | 'edit';
    onOk: () => void;
    onCancel: () => void;
    form: any;
    roles: Role[];
    loading: boolean;
}

const AddEditModal: React.FC<AddEditModalProps> = ({
                                                       open,
                                                       mode,
                                                       onOk,
                                                       onCancel,
                                                       form,
                                                       loading,
                                                       roles,
                                                   }) => {

    return (
        <Modal
            title={<span
                className="modal-title">{mode === 'edit' ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên mới'}</span>}
            open={open}
            onOk={onOk}
            onCancel={onCancel}
            okText={mode === 'edit' ? 'Cập nhật' : 'Thêm'}
            okButtonProps={{className: 'modal-ok-button', loading}}
            cancelButtonProps={{className: 'modal-cancel-button'}}
            cancelText="Hủy"
            className="modal-hide-scrollbar"
            maskClosable={false}
            keyboard={false}
        >
            <Form form={form} layout="vertical">
                {/* Hàng 1: Last name + First name */}
                <div className="permission-info-grid">
                    <Form.Item
                        label={<span className="modal-lable">Họ</span>}
                        name="lastName"
                        rules={[{required: true, message: 'Nhập họ!'}]}
                    >
                        <Input
                            className="modal-custom-input"
                            placeholder="Nhập họ"
                        />
                    </Form.Item>
                    <Form.Item
                        label={<span className="modal-lable">Tên</span>}
                        name="firstName"
                        rules={[{required: true, message: 'Nhập tên!'}]}
                    >
                        <Input
                            className="modal-custom-input"
                            placeholder="Nhập tên"
                        />
                    </Form.Item>
                </div>

                {/* Hàng 2: Email */}
                <Form.Item
                    label={<span className="modal-lable">Email</span>}
                    name="email"
                    rules={[
                        {required: true, message: 'Nhập email!'},
                        {type: 'email', message: 'Email không hợp lệ!'},
                    ]}
                >
                    <Input
                        className="modal-custom-input"
                        placeholder="Nhập địa chỉ email"
                    />
                </Form.Item>

                {/* Hàng 3: Phone number */}
                <Form.Item
                    label={<span className="modal-lable">Số điện thoại</span>}
                    name="phoneNumber"
                    rules={[{required: true, message: 'Nhập số điện thoại!'}]}
                >
                    <Input
                        className="modal-custom-input"
                        placeholder="Nhập số điện thoại"
                    />
                </Form.Item>

                {/* Hàng 4: Role */}
                {mode === 'create' &&
                    <Form.Item
                        label={<span className="modal-lable">Nhóm quyền</span>}
                        name="roleId"
                        rules={[{required: true, message: 'Chọn nhóm quyền!'}]}
                    >
                        <Select
                            className="modal-custom-select"
                            placeholder="Chọn nhóm quyền"
                            options={roles.map((r) => ({
                                value: r.id,
                                label: r.name,
                            }))}
                        />
                    </Form.Item>
                }
            </Form>
        </Modal>
    );
};

export default AddEditModal;