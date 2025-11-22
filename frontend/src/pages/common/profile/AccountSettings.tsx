import React, { useState } from 'react';
import { Tabs } from 'antd';
import { UserOutlined, MailOutlined, LockOutlined, ProfileOutlined } from '@ant-design/icons';
import EmailSettings from './components/EmailSettings';
import ProfileSettings from './components/ProfileSettings';
import './AccountSettings.css';
import PasswordSettings from './components/PasswordSettings';
import Title from 'antd/es/typography/Title';

const AccountSettings: React.FC = () => {
  const [activeTab, setActiveTab] = useState('profile');

  const handleTabChange = (key: string) => {
    setActiveTab(key);
  };

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
          items={[
            {
              key: 'profile',
              label: (
                <span className="tab-label">
                  <UserOutlined />
                  Thông Tin Cá Nhân
                </span>
              ),
              children: <ProfileSettings />,
            },
            {
              key: 'email',
              label: (
                <span className="tab-label">
                  <MailOutlined />
                  Cài Đặt Email
                </span>
              ),
              children: <EmailSettings />,
            },
            {
              key: 'password',
              label: (
                <span className="tab-label">
                  <LockOutlined />
                  Đổi Mật Khẩu
                </span>
              ),
              children: <PasswordSettings />,
            },
          ]}
        />
      </div>
    </div>
  );
};

export default AccountSettings;