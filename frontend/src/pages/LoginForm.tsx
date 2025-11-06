import React, { useEffect } from "react";
import { Form, Input, Button, Card, message, Typography } from "antd";
import { UserOutlined, LockOutlined } from "@ant-design/icons";
import { useNavigate, Link } from "react-router-dom";
import bg1 from "../assets/images/bg-1.jpg";

const { Title, Text } = Typography;

interface LoginFormData {
  email: string;
  password: string;
}

const LoginForm: React.FC = () => {
  const navigate = useNavigate();
  // const { loading, error } = useAppSelector((state) => state.auth);

  // useEffect(() => {
  //   if (error) {
  //     message.error(error);
  //     // dispatch(clearError());
  //   }
  // }, [error, dispatch]);

  const onFinish = async (values: LoginFormData) => {
    // try {
    //   const result = await dispatch(login(values)).unwrap();
    //   if (result.success && result.user) {
    //     message.success("Đăng nhập thành công!");
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
    // } catch (error) {
    //   // handled by slice
    // }
  };

  return (
    <div
      style={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        background: "#1C3D90",
        padding: 20,
      }}
    >
      <Card
        style={{
          width: "100%",
          maxWidth: 1100,
          borderRadius: 20,
          overflow: "hidden",
          boxShadow: "0 10px 25px rgba(0,0,0,0.25)",
        }}
        bodyStyle={{ padding: 0 }}
      >
        <div style={{ display: "flex", minHeight: 550 }}>
          {/* Left Column - Image */}
          <div
            style={{
              flex: 1,
              display: "flex",
              justifyContent: "center",
              alignItems: "center",
              padding: 20,
            }}
          >
            <img
              src={bg1}
              alt="Login"
              style={{
                width: "85%",
                maxWidth: 400,
                height: "auto",
                objectFit: "contain",
              }}
            />
          </div>

          {/* Right Column - Form */}
          <div
            style={{
              flex: 1,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: "40px 50px",
              backgroundColor: "#fff",
            }}
          >
            <div style={{ width: "100%", maxWidth: 400 }}>
              <div style={{ textAlign: "center", marginBottom: 30 }}>
                <Title level={2} style={{ color: "#1C3D90", margin: 0 }}>
                  UTE Logistics
                </Title>
                <Text type="secondary">
                  Đăng nhập để tiếp tục trải nghiệm
                </Text>
              </div>

              <Form
                name="login"
                onFinish={onFinish}
                autoComplete="off"
                layout="vertical"
              >
                <Form.Item
                  name="email"
                  label={<span style={{ color: "#000" }}>Email</span>}
                  rules={[
                    { required: true, message: "Vui lòng nhập email!" },
                    { type: "email", message: "Email không hợp lệ!" },
                  ]}
                >
                  <Input
                    prefix={<UserOutlined style={{ color: "#1C3D90" }} />}
                    placeholder="Nhập email của bạn"
                    size="large"
                    style={{
                      backgroundColor: "#f5f7fb",
                      borderRadius: 8,
                    }}
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  label={<span style={{ color: "#000" }}>Mật khẩu</span>}
                  rules={[
                    { required: true, message: "Vui lòng nhập mật khẩu!" },
                    { min: 6, message: "Mật khẩu phải có ít nhất 6 ký tự!" },
                  ]}
                >
                  <Input.Password
                    prefix={<LockOutlined style={{ color: "#1C3D90" }} />}
                    placeholder="Nhập mật khẩu của bạn"
                    size="large"
                    style={{
                      backgroundColor: "#f5f7fb",
                      borderRadius: 8,
                    }}
                  />
                </Form.Item>

                <div style={{ textAlign: "right", marginBottom: 16 }}>
                  <Link to="/forgot-password" style={{ color: "#1C3D90" }}>
                    Quên mật khẩu?
                  </Link>
                </div>

                <Form.Item>
                  <Button
                    type="primary"
                    htmlType="submit"
                    // loading={loading}
                    size="large"
                    style={{
                      width: "100%",
                      backgroundColor: "#1C3D90",
                      border: "none",
                      borderRadius: 8,
                      fontWeight: 600,
                    }}
                  >
                    Đăng nhập
                  </Button>
                </Form.Item>

                <div style={{ textAlign: "center" }}>
                  <Text>
                    Chưa có tài khoản?{" "}
                    <Link to="/register" style={{ color: "#1C3D90" }}>
                      Đăng ký ngay
                    </Link>
                  </Text>
                </div>
              </Form>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
};

export default LoginForm;