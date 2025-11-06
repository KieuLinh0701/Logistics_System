import React, { useState, useEffect } from "react";
import { Form, Input, Button, Card, message, Typography, Select, Steps, Row, Col } from "antd";
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined, SafetyOutlined } from "@ant-design/icons";
import { useNavigate, Link } from "react-router-dom";
// import { useAppDispatch, useAppSelector } from "../hooks/redux";
// import { register, verifyOTP, clearError } from "../store/authSlice";
// import { RegisterData, VerifyOTPData } from "../types/auth";

// ảnh minh họa (bạn thay bằng đường dẫn ảnh thật)
import bg1 from "../assets/images/bg-1.jpg";
import type { RegisterData } from "../type/auth";

const { Title, Text } = Typography;
const { Option } = Select;

const RegisterForm: React.FC = () => {
  //const dispatch = useAppDispatch();
  const navigate = useNavigate();
  // const { loading, error } = useAppSelector((state) => state.auth);

  const [currentStep, setCurrentStep] = useState(0);
  const [registerData, setRegisterData] = useState<RegisterData | null>(null);

  // useEffect(() => {
  //   if (error) {
  //     message.error(error);
  //     dispatch(clearError());
  //   }
  // }, [error, dispatch]);

  const onFinishStep1 = async (values: RegisterData) => {
    // try {
    //   const result = await dispatch(register(values)).unwrap();
    //   if (result.success) {
    //     setRegisterData(values);
    //     setCurrentStep(1);
    //     message.success("Mã OTP đã được gửi đến email của bạn!");
    //   }
    // } catch {}
  };

  const onFinishStep2 = async (values: { otp: string }) => {
    // if (!registerData) return;
    // try {
    //   const verifyData: VerifyOTPData = { ...registerData, otp: values.otp };
    //   const result = await dispatch(verifyOTP(verifyData)).unwrap();
    //   if (result.success && result.user) {
    //     message.success("Đăng ký thành công!");
    //     switch (result.user.role) {
    //       case "admin":
    //         navigate("/admin/dashboard");
    //         break;
    //       case "manager":
    //         navigate("/manager/dashboard");
    //         break;
    //       default:
    //         navigate("/");
    //     }
    //   }
    // } catch {}
  };

  const steps = [
    {
      title: "Thông tin cá nhân",
      content: (
        <Form name="register" onFinish={onFinishStep1} autoComplete="off" layout="vertical">
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="firstName"
                label="Họ"
                rules={[{ required: true, message: "Vui lòng nhập họ!" }]}
              >
                <Input prefix={<UserOutlined style={{ color: "#1C3D90" }}/>} placeholder="Nhập họ của bạn" size="large" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="lastName"
                label="Tên"
                rules={[{ required: true, message: "Vui lòng nhập tên!" }]}
              >
                <Input prefix={<UserOutlined style={{ color: "#1C3D90" }}/>} placeholder="Nhập tên của bạn" size="large" />
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
                <Input prefix={<MailOutlined style={{ color: "#1C3D90" }}/>} placeholder="Nhập email" size="large" />
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
                <Input prefix={<PhoneOutlined style={{ color: "#1C3D90" }}/>} placeholder="Nhập số điện thoại" size="large" />
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
                ]}
              >
                <Input.Password prefix={<LockOutlined style={{ color: "#1C3D90" }}/>} placeholder="Nhập mật khẩu" size="large" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="role" label="Vai trò" initialValue="user">
                <Select size="large">
                  <Option value="user">Khách hàng</Option>
                  <Option value="shipper">Shipper</Option>
                  <Option value="staff">Nhân viên</Option>
                  <Option value="driver">Tài xế</Option>
                  <Option value="manager">Quản lý</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              // loading={loading}
              size="large"
              style={{ width: "100%", background: "#1C3D90", marginTop: 15 }}
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
        <Form name="verifyOTP" onFinish={onFinishStep2} autoComplete="off" layout="vertical">
          <Form.Item
            name="otp"
            label="Mã OTP"
            rules={[
              { required: true, message: "Vui lòng nhập mã OTP!" },
              { len: 6, message: "Mã OTP phải có 6 ký tự!" },
            ]}
          >
            <Input prefix={<SafetyOutlined style={{ color: "#1C3D90" }}/>} placeholder="Nhập mã OTP 6 số" size="large" maxLength={6} />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              // loading={loading}
              size="large"
              style={{ width: "100%", background: "#1C3D90", marginTop: 15 }}
            >
              Xác thực
            </Button>
          </Form.Item>
        </Form>
      ),
    },
  ];

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#1C3D90",
        padding: 20,
      }}
    >
      <Card
        style={{
          width: "100%",
          maxWidth: 1250,
          borderRadius: 20,
          overflow: "hidden",
          boxShadow: "0 8px 20px rgba(0,0,0,0.25)",
        }}
        bodyStyle={{ padding: 0 }}
      >
        <div style={{ display: "flex", minHeight: 600 }}>
          {/* Left - Image */}
          <div
            style={{
              flex: 1,
              backgroundColor: "#f9f9f9",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: 20,
            }}
          >
            <img
              src={bg1}
              alt="Register"
              style={{ width: "85%", maxWidth: 400, objectFit: "contain" }}
            />
          </div>

          {/* Right - Form */}
          <div
            style={{
              flex: 1,
              padding: "40px 50px",
              display: "flex",
              flexDirection: "column",
              justifyContent: "center",
              background: "#fff",
            }}
          >
            <div style={{ textAlign: "center", marginBottom: 24 }}>
              <Title level={2} style={{ color: "#1C3D90", marginBottom: 8 }}>
                UTE Logistics
              </Title>
              <Text type="secondary">Đăng ký tài khoản mới</Text>
            </div>

            <Steps current={currentStep} style={{ marginBottom: 24 }}>
              {steps.map((item) => (
                <Steps.Step key={item.title} title={item.title} />
              ))}
            </Steps>

            {steps[currentStep].content}

            <div style={{ textAlign: "center", marginTop: 16 }}>
              <Text>
                Đã có tài khoản?{" "}
                <Link to="/login" style={{ color: "#1C3D90" }}>
                  Đăng nhập
                </Link>
              </Text>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default RegisterForm;