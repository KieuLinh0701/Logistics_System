import React, { useState } from "react";
import { Form, Input, Button, Typography, message } from "antd";
import { UserOutlined, LockOutlined } from "@ant-design/icons";
import { Link, useNavigate } from "react-router-dom";
import bg1 from "../assets/images/bg-1.jpg";
import "./Login.css";
import type { LoginData } from "../types/auth";
import authApi from "../api/authApi";
import { getUserRole } from "../utils/authUtils";

const { Title, Text } = Typography;

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);

  const onFinish = async (values: LoginData) => {
    if (!values) return;
    setLoading(true);
    try {
      const result = await authApi.login(values);

      if (result.success) {
        const role = getUserRole();

        if (role) {
          navigate(`/${role}/dashboard`);
        } else {
          console.warn("Token không hợp lệ hoặc chưa lưu");
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
                  UTE Logistics
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
                    label="Email hoặc Số điện thoại"
                    name="identifier"
                    rules={[
                      { required: true, message: "Vui lòng nhập email hoặc số điện thoại!" },
                      {
                        validator: (_, value) => {
                          if (!value) return Promise.resolve();
                          const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                          const phoneRegex = /^[0-9]{10}$/;
                          if (emailRegex.test(value) || phoneRegex.test(value)) {
                            return Promise.resolve();
                          }
                          return Promise.reject(new Error("Vui lòng nhập email hợp lệ hoặc số điện thoại 10 số!"));
                        },
                      },
                    ]}
                  >
                    <Input
                      prefix={<UserOutlined className="form-input-prefix" />}
                      placeholder="Nhập email hoặc số điện thoại"
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
    </div>
  );
};

export default Login;