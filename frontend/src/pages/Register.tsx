import React, { useState } from "react";
import { Form, Input, Button, Card, Typography, Steps, Row, Col } from "antd";
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined } from "@ant-design/icons";
import { useNavigate, Link } from "react-router-dom";
import bg1 from "../assets/images/bg-1.jpg";
import type { RegisterData, VerifyOTPData } from "../types/auth";
import "./Register.css";
import authApi from "../api/authApi";
import OtpInput from "../components/common/input/OtpInput";
import { message } from "antd";
import { getUserRole } from "../utils/authUtils";

const { Title, Text } = Typography;

const Register: React.FC = () => {
  const navigate = useNavigate();
  const [currentStep, setCurrentStep] = useState(0);
  const [registerData, setRegisterData] = useState<RegisterData | null>(null);
  const [loading, setLoading] = useState(false);
  const [form] = Form.useForm();

  const onFinishStep1 = async (values: RegisterData) => {
    setLoading(true);
    try {
      const result = await authApi.register(values);
      console.log(result);
      console.log(result.success)
      console.log(result.message)
      if (result.success) {
        setRegisterData(values);
        setCurrentStep(1);
        message.success("Mã OTP đã được gửi đến email của bạn!");
      } else {
        message.error(result.message || "Đăng ký thất bại!");
      }
    } catch (error) {
      console.error("Register error:", error);
      message.error("Có lỗi xảy ra khi đăng ký!");
    } finally {
      setLoading(false);
    }
  };

  const onFinishStep2 = async (values: { otp: string }) => {
    if (!registerData) return;
    setLoading(true);
    try {
      const verifyData: VerifyOTPData = { ...registerData, otp: values.otp };
      const result = await authApi.verifyOTP(verifyData);

      if (result.success) {
        message.success("Đăng ký thành công!");

        const role = getUserRole();

        if (role) {
          navigate(`/${role}/dashboard`);
        } else {
          console.warn("Token không hợp lệ hoặc chưa lưu");
        }
      } else {
        message.error(result.message || "Đăng ký thất bại!");
      }
    } catch (err) {
      console.error(err);
      message.error("Có lỗi xảy ra, vui lòng thử lại!");
    } finally {
      setLoading(false);
    }
  };

  const steps = [
    {
      title: "Thông tin cá nhân",
      content: (
        <Form name="register" onFinish={onFinishStep1} autoComplete="off" layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="lastName"
                label="Họ"
                rules={[{ required: true, message: "Vui lòng nhập họ!" }]}
              >
                <Input
                  prefix={<UserOutlined className="form-input-prefix" />}
                  placeholder="Nhập họ của bạn"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="firstName"
                label="Tên"
                rules={[{ required: true, message: "Vui lòng nhập tên!" }]}
              >
                <Input
                  prefix={<UserOutlined className="form-input-prefix" />}
                  placeholder="Nhập tên của bạn"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="email"
                label="Email"
                rules={[
                  { required: true, message: "Vui lòng nhập email!" },
                  { type: "email", message: "Email không hợp lệ!" },
                ]}
              >
                <Input
                  prefix={<MailOutlined className="form-input-prefix" />}
                  placeholder="Nhập email"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="phoneNumber"
                label="Số điện thoại"
                rules={[
                  { required: true, message: "Vui lòng nhập số điện thoại!" },
                  { pattern: /^[0-9]{10,11}$/, message: "Số điện thoại không hợp lệ!" },
                ]}
              >
                <Input
                  prefix={<PhoneOutlined className="form-input-prefix" />}
                  placeholder="Nhập số điện thoại"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="password"
                label="Mật khẩu"
                rules={[
                  { required: true, message: "Vui lòng nhập mật khẩu!" },
                  { min: 6, message: "Mật khẩu ít nhất 6 ký tự!" },
                  {
                    pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.])[A-Za-z\d@$!%*?&.]{6,}$/,
                    message: "Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt!",
                  },
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined className="form-input-prefix" />}
                  placeholder="Nhập mật khẩu"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                name="confirmPassword"
                label="Xác nhận mật khẩu"
                dependencies={["password"]}
                rules={[
                  { required: true, message: "Vui lòng xác nhận mật khẩu!" },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue("password") === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error("Mật khẩu xác nhận không khớp!"));
                    },
                  }),
                ]}
              >
                <Input.Password
                  prefix={<LockOutlined className="form-input-prefix" />}
                  placeholder="Nhập lại mật khẩu"
                  size="large"
                  className="form-input"
                />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              className="register-button"
              loading={loading}
            >
              Tiếp tục
            </Button>
          </Form.Item>
        </Form>
      ),
    },
    {
      title: "Xác thực OTP",
      content: (
        <Form form={form} name="verifyOTP" onFinish={onFinishStep2} autoComplete="off" layout="vertical">
          <Form.Item
            name="otp"
            label="Mã OTP"
            rules={[
              { required: true, message: "Vui lòng nhập mã OTP!" },
              { len: 6, message: "Mã OTP phải có 6 ký tự!" },
            ]}
          >
            <OtpInput length={6} onChange={(val) => form.setFieldsValue({ otp: val })} />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              className="register-button"
              loading={loading}
            >
              Xác thực
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <div className="register-container">
      <Card className="register-card" styles={{ body: { padding: 0 } }}>
        <div className="register-layout">
          {/* Left - Image */}
          <div className="register-image-section">
            <img
              src={bg1}
              alt="Register"
              className="register-image"
            />
          </div>

          {/* Right - Form */}
          <div className="register-form-section">
            <div className="register-form-wrapper">
              <div className="register-header">
                <Title level={2} className="register-title">
                  UTE Logistics
                </Title>
                <Text className="register-subtitle">
                  Đăng ký tài khoản mới
                </Text>
              </div>

              <div className="steps-container">
                <Steps current={currentStep}>
                  {steps.map((item) => (
                    <Steps.Step key={item.title} title={item.title} />
                  ))}
                </Steps>
              </div>

              {steps[currentStep].content}

              <div className="login-link">
                <Text>
                  Đã có tài khoản?{" "}
                  <Link to="/login" className="login-link-text">
                    Đăng nhập
                  </Link>
                </Text>
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default Register;