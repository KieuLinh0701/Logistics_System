import React, { useState } from 'react';
import { Tabs } from 'antd';
import { UserOutlined, MailOutlined, LockOutlined, ProfileOutlined } from '@ant-design/icons';
import EmailSettings from './components/EmailSettings';
import ProfileSettings from './components/ProfileSettings';
import PasswordSettings from './components/PasswordSettings';
import Title from 'antd/es/typography/Title';
import './AccountSettings.css';
import { getUserRole } from '../../../utils/authUtils';
import AddressSettingsUser from './components/userAddress/AddressSettingsUser';

const AccountSettings: React.FC = () => {
  const role = getUserRole();

  const getTabFromUrl = () => {
    const params = new URLSearchParams(window.location.search);
    return params.get("tab") || "profile";
  };

  const [activeTab, setActiveTab] = useState(getTabFromUrl());

  const handleTabChange = (key: string) => {
    setActiveTab(key);
    const params = new URLSearchParams(window.location.search);
    params.set("tab", key);
    window.history.replaceState({}, "", `${window.location.pathname}?${params.toString()}`);
  };

  const tabs = [
    {
      key: 'profile',
      label: (
        <span className="tab-label">
          <UserOutlined /> Thông Tin Cá Nhân
        </span>
      ),
      children: <ProfileSettings />,
    },
    {
      key: 'email',
      label: (
        <span className="tab-label">
          <MailOutlined /> Cài Đặt Email
        </span>
      ),
      children: <EmailSettings />,
    },
    {
      key: 'password',
      label: (
        <span className="tab-label">
          <LockOutlined /> Đổi Mật Khẩu
        </span>
      ),
      children: <PasswordSettings />,
    },
  ];

  if (role === "user") {
    tabs.push({
      key: 'address',
      label: (
        <span className="tab-label">
          <LockOutlined /> Cài Đặt Địa Chỉ
        </span>
      ),
      children: <AddressSettingsUser />,
    });
  }

  return (
    <div className="profile-settings-container">
      <div className="profile-settings-content">
        <div className="profile-settings-header">
          <div className="header-left">
            <ProfileOutlined className="profile-icon" />
            <Title level={2} className="profile-title">Cài Đặt Tài Khoản</Title>
          </div>
          <div className="header-right">
            <p className="profile-subtitle">Quản lý thông tin cá nhân và bảo mật tài khoản</p>
          </div>
        </div>

        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          className="profile-tabs"
          items={tabs}
        />
      </div>
    </div>
  );
};

export default AccountSettings;