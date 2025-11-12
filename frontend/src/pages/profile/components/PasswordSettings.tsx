import React, { useState } from 'react';
import { Form, Input, Button, message, Card } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import userApi from '../../../api/userApi';

const PasswordSettings: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const onFinish = async (values: { oldPassword: string; newPassword: string }) => {
    setLoading(true);
    try {
      const result = await userApi.updatePassword({
        oldPassword: values.oldPassword,
        newPassword: values.newPassword
      });
      if (result.success) {
        message.success('Đổi mật khẩu thành công!');
        form.resetFields();
      } else {
        message.error(result.message || "Đổi mật khẩu thất bại!");
      }
    } catch (error) {
      console.error("Change Password error:", error);
      message.error('Có lỗi xảy ra khi đổi mật khẩu!');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tab-content">
      <Card className="profile-form-card">
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
        >
          <Form.Item
            label="Mật khẩu hiện tại"
            name="oldPassword"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu hiện tại!' },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined className="form-input-prefix" />}
              placeholder="Nhập mật khẩu hiện tại"
              className="form-input"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="Mật khẩu mới"
            name="newPassword"
            rules={[
              { required: true, message: 'Vui lòng nhập mật khẩu mới!' },
              { min: 6, message: 'Mật khẩu ít nhất 6 ký tự!' },
              {
                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.])[A-Za-z\d@$!%*?&.]{6,}$/,
                message: 'Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt!',
              },
            ]}
          >
            <Input.Password
              prefix={<LockOutlined className="form-input-prefix" />}
              placeholder="Nhập mật khẩu mới"
              className="form-input"
              size="large"
            />
          </Form.Item>

          <Form.Item
            label="Xác nhận mật khẩu mới"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Vui lòng xác nhận mật khẩu!' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('newPassword') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error('Mật khẩu xác nhận không khớp!'));
                },
              }),
            ]}
          >
            <Input.Password
              prefix={<LockOutlined className="form-input-prefix" />}
              placeholder="Nhập lại mật khẩu mới"
              className="form-input"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              className="btn-primary"
            >
              Đổi Mật Khẩu
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default PasswordSettings;