import React, { useState, useEffect, useRef } from 'react';
import { Form, Input, Button, message, Card, Avatar } from 'antd';
import { UserOutlined, CameraOutlined, EditOutlined, PhoneOutlined } from '@ant-design/icons';
import { getCurrentUser } from '../../../../utils/authUtils';
import userApi from '../../../../api/userApi';
import type { User } from '../../../../types/auth';

const ProfileSettings: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();
  const [user, setUser] = useState<User | null>(null);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | undefined>(undefined);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const currentUser = getCurrentUser();
    console.log("user", currentUser);
    setUser(currentUser);

    if (currentUser) {
      form.setFieldsValue({
        firstName: currentUser.firstName || '',
        lastName: currentUser.lastName || '',
        phoneNumber: currentUser.phoneNumber || '',
      });
      if (currentUser.images) {
        setPreviewUrl(currentUser.images);
      }
    }
  }, [form]);

  useEffect(() => {
    return () => {
      if (previewUrl && previewUrl.startsWith('blob:')) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (previewUrl && previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrl);
    }

    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    setAvatarFile(file);
  };

  const triggerFileSelect = () => {
    fileInputRef.current?.click();
  };

  const onFinish = async (values: any) => {
    if (!user) return;

    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('firstName', values.firstName);
      formData.append('lastName', values.lastName);
      formData.append('phoneNumber', values.phoneNumber);
      if (avatarFile) {
        formData.append('avatarFile', avatarFile);
        console.log('avatarFile:', avatarFile);
      }

      const result = await userApi.updateProfile(formData);

      if (result.success) {
        message.success('Cập nhật thông tin thành công!');

        const updatedUser = { ...user, ...values, images: result.data };

        sessionStorage.setItem('user', JSON.stringify(updatedUser));

        window.dispatchEvent(new CustomEvent('userUpdated', { detail: updatedUser }));
      } else {
        message.error(result.message || 'Cập nhật thông tin thất bại!');
      }
    } catch (error) {
      console.error(error);
      message.error('Có lỗi xảy ra khi cập nhật thông tin!');
    } finally {
      setLoading(false);
    }
  };

  const renderAvatar = () => (
    <div className="avatar-with-edit" onClick={triggerFileSelect} style={{ cursor: 'pointer' }}>
      <Avatar size={120} src={previewUrl || undefined} icon={<UserOutlined />} className="profile-avatar" />
      <div className="avatar-edit-overlay">
        <CameraOutlined className="edit-icon" />
      </div>
    </div>
  );

  return (
    <div className="tab-content">
      <Card className="profile-form-card">
        {/* Avatar Section */}
        <div className="avatar-section">
          <div className="avatar-container">
            {renderAvatar()}
            <input
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              ref={fileInputRef}
              onChange={handleFileChange}
            />
            <div className="avatar-info">
              <h3 className="avatar-title">Ảnh đại diện</h3>
              <div className="avatar-notes">
                <span className="note-item">• Kích thước: 200 x 200 px</span>
                <span className="note-item">• Định dạng: JPG, PNG</span>
                <span className="note-item">• Dung lượng: ≤ 5MB</span>
              </div>
            </div>
          </div>
        </div>

        {/* Form Section */}
        <Form form={form} layout="vertical" onFinish={onFinish} className="profile-form">
          <div className="form-section">
            <div className="name-row">
              <Form.Item
                label="Họ"
                name="lastName"
                className="name-item"
                rules={[
                  { required: true, message: 'Vui lòng nhập họ!' },
                  { min: 1, message: 'Họ ít nhất 1 ký tự!' },
                ]}
              >
                <Input
                  prefix={<UserOutlined className="form-input-prefix" />}
                  placeholder="Nhập họ của bạn"
                  size="large"
                  className="form-input"
                />
              </Form.Item>

              <Form.Item
                label="Tên"
                name="firstName"
                className="name-item"
                rules={[
                  { required: true, message: 'Vui lòng nhập tên!' },
                  { min: 1, message: 'Tên ít nhất 1 ký tự!' },
                ]}
              >
                <Input
                  prefix={<UserOutlined className="form-input-prefix" />}
                  placeholder="Nhập tên của bạn"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </div>

            <Form.Item
              name="phoneNumber"
              label="Số điện thoại"
              rules={[
                { required: true, message: 'Vui lòng nhập số điện thoại!' },
                { pattern: /^[0-9]{10,11}$/, message: 'Số điện thoại không hợp lệ!' },
              ]}
            >
              <Input
                prefix={<PhoneOutlined className="form-input-prefix" />}
                placeholder="Nhập số điện thoại"
                size="large"
                className="form-input"
              />
            </Form.Item>
          </div>

          <div className="form-actions">
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              className="btn-primary"
              icon={<EditOutlined />}
            >
              Cập Nhật Thông Tin
            </Button>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default ProfileSettings;