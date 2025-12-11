import React, { useEffect, useState } from 'react';
import { Col, Form, message, Row, Tag } from 'antd';
import Actions from './components/Actions';
import BankAccountTable from './components/Table';
import AddEditModal from './components/AddEditModal';
import BankNoticeCard from './components/BankNoticeCard';
import bankAccountApi from '../../../api/bankAccountApi';
import type { BankAccount, BankAccountRequest } from '../../../types/bankAccount';
import Title from 'antd/es/typography/Title';
import { BankOutlined } from '@ant-design/icons';
import "../../../styles/ListPage.css";

const UserBankAccounts: React.FC = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [form] = Form.useForm();
  const [bankNames, setBankNames] = useState<string[]>([]);
  const [total, setTotal] = useState<number>(0);
  const [accounts, setAccounts] = useState<BankAccount[]>([]);
  const [loading, setLoading] = useState(false);

  const emptyAccount: BankAccountRequest = {
    bankName: '',
    accountNumber: '',
    accountName: '',
    notes: '',
    isDefault: total === 0,
  };
  const [newBankAccount, setNewBankAccount] = useState<BankAccountRequest>({ ...emptyAccount });

  useEffect(() => {
    const fetchBankNames = async () => {
      try {
        const bankList = await bankAccountApi.getBanks();
        setBankNames(bankList);
      } catch {
        setBankNames([]);
      }
    };
    fetchBankNames();
  }, []);

  const openCreateModal = () => {
    setModalMode('create');
    const defaultAccount = { ...emptyAccount, isDefault: total === 0 };
    setNewBankAccount(defaultAccount);
    form.resetFields();
    form.setFieldsValue(defaultAccount);
    setIsModalOpen(true);
  };

  const openEditModal = (account: BankAccount) => {
    setModalMode('edit');
    setNewBankAccount(account);
    form.setFieldsValue(account);
    setIsModalOpen(true);
  };

  const handleAddBankAccount = async () => {
    setLoading(true);
    await form.validateFields();
    try {
      const result = await bankAccountApi.createUserBankAccount(newBankAccount);
      if (result.success) message.success("Thêm tài khoản thành công!");
      else message.error(result.message || "Có lỗi khi thêm tài khoản");
      fetchBankAccounts();
      setIsModalOpen(false);
      setNewBankAccount({ ...emptyAccount });
      form.resetFields();
    } catch {
      message.error("Có lỗi khi thêm tài khoản");
    } finally {
      setLoading(false);
    }
  };

  const handleEditBankAccount = async () => {
    setLoading(true);
    await form.validateFields();
    try {
      const result = await bankAccountApi.updateUserBankAccount(newBankAccount.id!, newBankAccount);
      if (result.success) message.success("Cập nhật thành công!");
      else message.error(result.message || "Có lỗi khi cập nhật");
      fetchBankAccounts();
      setIsModalOpen(false);
      setNewBankAccount({ ...emptyAccount });
      form.resetFields();
    } catch {
      message.error("Có lỗi khi cập nhật");
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteBankAccount = async (id: number) => {
    try {
      const result = await bankAccountApi.deleteUserBankAccount(id);

      if (result.success) {
        message.success(result.message || 'Xóa tài khoản thành công!');
        fetchBankAccounts();
      } else {
        message.error(result.message || 'Xóa tài khoản thất bại!');
      }
    } catch (error: any) {
      const errorMessage = error?.message || error?.response?.data?.message || 'Có lỗi khi xóa tài khoản!';
      console.log("Lỗi:", errorMessage)
      message.error("Có lỗi khi xóa tài khoản");
    }
  };

  const handleSetDefaultBankAccount = async (id: number) => {
    try {
      const result = await bankAccountApi.setDefaultUserBankAccount(id);

      if (result.success) {
        message.success(result.message || 'Đặt tài khoản mặc định thành công!');
        fetchBankAccounts();
      } else {
        message.error(result.message || 'Cập nhật mặc định thất bại!');
      }
    } catch (error: any) {
      const errorMessage = error?.message || error?.response?.data?.message || 'Có lỗi khi cập nhật mặc định!';
      console.log("Lỗi:", errorMessage)
      message.error("Có lỗi khi cập nhật mặc định!");
    }
  };

  const fetchBankAccounts = async () => {
    try {
      const result = await bankAccountApi.getUserBankAccounts();
      if (result && result.success && result.data) {
        setAccounts(result.data);
        setTotal(result.data.length);
      } else {
        setAccounts([]);
        setTotal(0);
        message.error("Có lỗi khi lấy danh sách tài khoản");
      }
    } catch {
      setAccounts([]);
      setTotal(0);
      message.error("Có lỗi khi lấy danh sách tài khoản");
    }
  };

  useEffect(() => {
    fetchBankAccounts();
  }, []);

  return (
    <div className="list-page-layout">
      <div className="list-page-content">
        <Row justify="space-between" align="middle">
          <Col>
            <Title level={3} className="list-page-title-main">
              <BankOutlined className="title-icon" />
              Danh sách tài khoản</Title>
          </Col>
          <Col>
            <div className="list-page-actions">
              <Actions onAdd={openCreateModal} total={total} />
            </div>
          </Col>
        </Row>

        <BankNoticeCard />
        <Tag className="list-page-tag">Kết quả trả về: {total} tài khoản</Tag>

        <BankAccountTable
          data={accounts}
          onEdit={openEditModal}
          onDelete={handleDeleteBankAccount}
          onSetDefault={handleSetDefaultBankAccount}
        />

        <AddEditModal
          open={isModalOpen}
          mode={modalMode}
          account={newBankAccount}
          onOk={modalMode === 'edit' ? handleEditBankAccount : handleAddBankAccount}
          onCancel={() => setIsModalOpen(false)}
          onBankAccountChange={setNewBankAccount}
          form={form}
          loading={loading}
          total={total}
          bankNames={bankNames}
        />
      </div>
    </div>
  );
};

export default UserBankAccounts;