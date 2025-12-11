import React, { useState, useEffect } from 'react';
import { Card, Button, message } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { Form } from 'antd';
import type { Address } from './components/AddressTable';
import type { AddressRequest } from '../../../../../types/address';
import AddressTable from './components/AddressTable';
import AddressModal from './components/AddressModal';
import addressApi from '../../../../../api/addressApi';

const AddressSettingsUser: React.FC = () => {
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [editingAddress, setEditingAddress] = useState<AddressRequest | null>(null);
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
    const [modalKey, setModalKey] = useState(0);
    const [total, setTotal] = useState(0);

    const fetchAddresses = async () => {
        try {
            setLoading(true);
            const response = await addressApi.getUserAddresses();
            if (response.success && response.data) {
                setAddresses(response.data);
                setTotal(response.data.length);
            }
        } catch (error) {
            console.error("Error fetching Addresses:", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchAddresses();
    }, []);

    const showModal = (mode: 'create' | 'edit', address?: Address) => {
        setModalMode(mode);

        if (mode === 'edit' && address) {
            setEditingAddress(address);
            form.resetFields();
            form.setFieldsValue({
                ...address,
                address: {
                    cityCode: address.cityCode || undefined,
                    wardCode: address.wardCode || undefined,
                    detail: address.detail || ''
                }
            });
        } else {
            const emptyAddress = {
                name: '',
                phoneNumber: '',
                detail: '',
                wardCode: 0,
                cityCode: 0,
                isDefault: addresses.length === 0
            };
            setEditingAddress(emptyAddress);
            form.resetFields();
            form.setFieldsValue({
                ...emptyAddress,
                address: { cityCode: undefined, wardCode: undefined, detail: '' }
            });
        }

        setModalKey(prev => prev + 1);
        setIsModalVisible(true);
    };

    const handleCancel = () => {
        setIsModalVisible(false);
        setEditingAddress(null);
        form.resetFields();
        form.setFieldsValue({
            address: { cityCode: undefined, wardCode: undefined, detail: 'Hello' }
        });
    };

    const handleSaveAddress = async () => {
        try {
            setLoading(true);

            const values = await form.validateFields();

            const payload: AddressRequest = {
                name: values.name,
                phoneNumber: values.phoneNumber,
                cityCode: values.address.cityCode,
                wardCode: values.address.wardCode,
                detail: values.address.detail,
                isDefault: values.isDefault,
            };

            console.log("Payload to save:", payload);

            if (modalMode === 'edit' && editingAddress?.id) {
                const response = await addressApi.updateUserAddress(editingAddress.id, payload);

                if (response.success && response.data) {
                    fetchAddresses();
                    message.success('Cập nhật địa chỉ thành công!');
                } else {
                    message.error(response.message || "Cập nhật địa chỉ thất bại");
                }
            } else {
                const response = await addressApi.createUserAddress(payload);

                if (response.success && response.data) {
                    fetchAddresses();
                    message.success('Thêm địa chỉ thành công!');
                } else {
                    message.error(response.message || "Thêm địa chỉ thất bại");
                }
            }

            handleCancel();
        } catch (error) {
            console.error("Error saving address:", error);
            message.error('Vui lòng kiểm tra lại thông tin!');
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteAddress = async (addressId: number) => {
        try {
            setLoading(true);
            const response = await addressApi.deleteUserAddress(addressId);
            if (response.success && response.data) {
                fetchAddresses();
                message.success('Xóa địa chỉ thành công');
            } else {
                message.error(response.message || "Xóa địa chỉ thành công");
            }
        } catch (error) {
            message.error("Có lỗi khi xóa địa chỉ");
            console.error("Error Delete Addresses:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleSetDefault = async (addressId: number) => {
        try {
            setLoading(true);
            const response = await addressApi.setDefaultUserAddress(addressId);
            if (response.success && response.data) {
                fetchAddresses();
                message.success("Đã đặt làm địa chỉ mặc định")
            } else {
                message.error(response.message || "Có lỗi khi đặt địa chỉ làm mặc định");
            }
        } catch (error) {
            message.error("Có lỗi khi đặt địa chỉ làm mặc định");
            console.error("Error Set Default Addresses:", error);
        } finally {
            setLoading(false);
        }
    };

    const handleAddressChange = (address: AddressRequest) => {
        setEditingAddress(address);
    };

    return (
        <div className="tab-content">
            <Card className="profile-form-card">
                <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 16 }}>
                    <Button
                        className="primary-button"
                        icon={<PlusOutlined />}
                        onClick={() => showModal('create')}
                        disabled={total >= 20}
                    >
                        Thêm địa chỉ mới
                    </Button>
                </div>

                <AddressTable
                    data={addresses}
                    onEdit={(address) => showModal('edit', address)}
                    onDelete={handleDeleteAddress}
                    onSetDefault={handleSetDefault}
                />

                <AddressModal
                    key={modalKey}
                    open={isModalVisible}
                    mode={modalMode}
                    address={editingAddress || {
                        name: '',
                        phoneNumber: '',
                        detail: '',
                        wardCode: 0,
                        cityCode: 0,
                        isDefault: addresses.length === 0
                    }}
                    onOk={handleSaveAddress}
                    onCancel={handleCancel}
                    onAddressChange={handleAddressChange}
                    form={form}
                    total={addresses.length}
                    loading={loading}
                />
            </Card>
        </div>
    );
};

export default AddressSettingsUser;