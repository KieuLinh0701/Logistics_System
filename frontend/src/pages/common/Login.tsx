import React, { useState } from "react";
import { Form, Input, Button, Typography, message, Modal, Radio, Card } from "antd";
import { UserOutlined, LockOutlined, CheckCircleOutlined } from "@ant-design/icons";
import { Link, useNavigate } from "react-router-dom";
import bg1 from "../../assets/images/bg-1.jpg";
import "./Login.css";
import type { LoginData } from "../../types/auth";
import authApi from "../../api/authApi";
import { getUserRole } from "../../utils/authUtils";
import { translateRoleName } from "../../utils/roleUtils";

const { Title, Text } = Typography;

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const [isRoleModalVisible, setRoleModalVisible] = useState(false);
  const [roles, setRoles] = useState<string[]>([]);
  const [selectedRole, setSelectedRole] = useState<string | null>(null);
  const [tempToken, setTempToken] = useState<string | null>(null);

  const onFinish = async (values: LoginData) => {
    if (!values) return;
    setLoading(true);
    try {
      const result = await authApi.login(values);

      if (result.success) {
        if (result.data && result.data.roles && result.data.tempToken) {
          setRoles(result.data.roles);
          setTempToken(result.data.tempToken);
          setRoleModalVisible(true);
        } else {
          navigate(`/dashboard`);
        }
      } else {
        message.error(result.message || "Đăng nhập thất bại!");
      }
    } catch (err) {
      console.error(err);
      message.error("Có lỗi xảy ra, vui lòng thử lại!");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-layout">
          {/* Left Column - Image */}
          <div className="login-image-section">
            <img
              src={bg1}
              alt="Login"
              className="login-image"
            />
          </div>

          {/* Right Column - Form */}
          <div className="login-form-section">
            <div className="login-form-wrapper">
              <div className="login-header">
                <Title level={2} className="login-title">
                  <Link to="/home" className="login-title-link">
                    UTE Logistics
                  </Link>
                </Title>
                <Text className="login-subtitle">
                  Đăng nhập để tiếp tục trải nghiệm
                </Text>
              </div>

              <Form
                name="login"
                onFinish={onFinish}
                autoComplete="off"
                layout="vertical"
              >
                <div className="form-item">
                  <Form.Item
                    label="Email"
                    name="email"
                    rules={[
                      { required: true, message: "Vui lòng nhập email!" },
                      {
                        validator: (_, value) => {
                          if (!value) return Promise.resolve();
                          const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                          if (emailRegex.test(value)) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error("Vui lòng nhập email hợp lệ!"));
                        },
                      },
                    ]}
                  >
                    <Input
                      prefix={<UserOutlined className="form-input-prefix" />}
                      placeholder="Nhập email"
                      className="form-input"
                      size="large"
                    />
                  </Form.Item>
                </div>

                <div className="form-item">
                  <Form.Item
                    name="password"
                    label="Mật khẩu"
                    rules={[
                      { required: true, message: "Vui lòng nhập mật khẩu!" },
                    ]}
                  >
                    <Input.Password
                      prefix={<LockOutlined className="form-input-prefix" />}
                      placeholder="Nhập mật khẩu"
                      size="large"
                      className="form-input"
                    />
                  </Form.Item>
                </div>

                <div style={{ textAlign: "right", marginBottom: 24 }}>
                  <Link to="/forgot-password" className="forgot-password-link">
                    Quên mật khẩu?
                  </Link>
                </div>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    size="large"
                    className="login-button"
                    loading={loading}
                  >
                    Đăng nhập
                  </Button>
                </Form.Item>

                <div className="register-text">
                  <Text>
                    Chưa có tài khoản?{" "}
                    <Link to="/register" className="register-link">
                      Đăng ký ngay
                    </Link>
                  </Text>
                </div>
              </Form>
            </div>
          </div>
        </div>
      </div>

      <Modal
        title={
          <div className="role-modal-header">
            <CheckCircleOutlined className="role-modal-icon" />
            <span className="role-modal-title">Chọn Vai Trò</span>
          </div>
        }
        open={isRoleModalVisible}
        closable={false}
        maskClosable={false}
        footer={null}
        width={420}
        centered
        className="role-select-modal"
        styles={{
          mask: {
            backdropFilter: 'blur(5px)',
            backgroundColor: 'rgba(0, 0, 0, 0.5)'
          }
        }}
      >
        <div className="role-modal-content">
          <Text className="role-modal-description">
            Chọn vai trò để đăng nhập
          </Text>

          <div className="role-options-container">
            <Radio.Group
              onChange={(e) => setSelectedRole(e.target.value)}
              value={selectedRole}
              className="role-radio-group"
            >
              <div className="role-list">
                {roles.map((role) => (
                  <div
                    key={role}
                    className={`role-option ${selectedRole === role ? 'role-option-selected' : ''}`}
                    onClick={() => setSelectedRole(role)}
                  >
                    <Radio value={role} className="role-radio-button">
                      <div className="role-text">
                        {translateRoleName(role)}
                      </div>
                    </Radio>
                  </div>
                ))}
              </div>
            </Radio.Group>
          </div>

          <div className="role-modal-footer">
            <Button
              type="primary"
              size="large"
              block
              disabled={!selectedRole}
              onClick={async () => {
                if (!selectedRole || !tempToken) return;
                try {
                  const result = await authApi.chooseRole({
                    roleName: selectedRole,
                    tempToken
                  });

                  if (result.success) {
                    const role = getUserRole();
                    if (role) {
                      navigate(`/dashboard`);
                    }
                  } else { 
                    message.error("Có lỗi xảy ra khi chọn role đăng nhập hoặc phiên đăng nhập của bạn đã hết hạn!");
                  }
                } catch (err) {
                  message.error("Có lỗi xảy ra khi chọn role đăng nhập!");
                }
              }}
              className="role-confirm-button"
            >
              Tiếp tục
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );

};

export default Login;