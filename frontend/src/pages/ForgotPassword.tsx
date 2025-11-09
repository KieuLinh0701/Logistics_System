import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Card, message, Typography, Steps } from 'antd';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate, Link } from 'react-router-dom';
import bg1 from "../assets/images/bg-1.jpg";
import './ForgotPassword.css';
import OtpInput from '../components/common/input/OtpInput';
import authApi from '../api/authApi';

const { Title, Text } = Typography;

const ForgotPassword: React.FC = () => {
    const navigate = useNavigate();

    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm();
    const [currentStep, setCurrentStep] = useState(0);
    const [identifier, setIdentifier] = useState('');
    const [otpCountdown, setOtpCountdown] = useState(300);
    const [canResend, setCanResend] = useState(false);

    useEffect(() => {
        let interval: ReturnType<typeof setInterval>;
        if (!canResend && otpCountdown > 0) {
            interval = setInterval(() => {
                setOtpCountdown(prev => {
                    if (prev <= 1) {
                        clearInterval(interval);
                        setCanResend(true);
                        return 0;
                    }
                    return prev - 1;
                });
            }, 1000);
        }
        return () => clearInterval(interval);
    }, [otpCountdown, canResend]);

    const handleEmailSubmit = async (values: { identifier: string }) => {
        setLoading(true);
        try {
            const result = await authApi.forgotPassword(values);
            if (result.success) {
                setIdentifier(values.identifier);
                setCurrentStep(1);
                message.success('Mã OTP đã được gửi đến email của bạn!');
                setOtpCountdown(300);
                setCanResend(false);
            } else {
                message.error(result.message || "Đặt lại mật khẩu thất bại!");
            }
        } catch (error) {
            console.error("Forgot Password error:", error);
            message.error("Có lỗi xảy ra khi xác nhận email đặt lại mật khẩu!");
        } finally {
            setLoading(false);
        }
    };

    const handleOtpSubmit = async (values: { otp: string }) => {
        if (!identifier) return;
        setLoading(true);
        try {
            const result = await authApi.verifyResetOtp({ identifier, otp: values.otp });
            if (result.success) {
                setCurrentStep(2);
                message.success('Xác thực OTP thành công!');
            } else {
                message.error(result.message || "Đặt lại mật khẩu thất bại!");
            }
        } catch (error) {
            console.error("Forgot Password error:", error);
            message.error("Có lỗi xảy ra khi xác thực OTP đặt lại mật khẩu!");
        } finally {
            setLoading(false);
        }
    };

    const handleResendOTP = async () => {
        if (!identifier) return;
        setLoading(true);
        try {
            const result = await authApi.forgotPassword({ identifier });
            if (result.success) {
                message.success('Mã OTP mới đã được gửi đến email của bạn!');
                setOtpCountdown(300);
                setCanResend(false);
            } else {
                message.error(result.message || "Đặt lại mật khẩu thất bại!");
            }
        } catch (error) {
            console.error("Forgot Password error:", error);
            message.error("Có lỗi xảy ra khi gửi OTP đặt lại mật khẩu!");
        } finally {
            setLoading(false);
        }
    };

    const handleResetPassword = async (values: { newPassword: string; confirmPassword: string }) => {
        if (!identifier) return;
        setLoading(true);
        try {
            const result = await authApi.resetPassword({
                identifier,
                newPassword: values.newPassword 
            });
            if (result.success) {
                message.success('Đặt lại mật khẩu thành công!');
                navigate('/login');
            } else {
                message.error(result.message || "Đặt lại mật khẩu thất bại!");
            }
        } catch (error) {
            console.error("Forgot Password error:", error);
            message.error("Có lỗi xảy ra khi đặt lại mật khẩu!");
        } finally {
            setLoading(false);
        }
    };

    const steps = [
        {
            title: 'Xác thực tài khoản',
            content: (
                <Form layout="vertical" onFinish={handleEmailSubmit} autoComplete="off">
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
                    <Form.Item>
                        <Button
                            type="primary"
                            htmlType="submit"
                            loading={loading}
                            className="btn-primary"
                        >
                            Tiếp tục
                        </Button>
                    </Form.Item>
                </Form>
            ),
        },
        {
            title: 'Xác thực OTP',
            content: (
                <Form form={form} layout="vertical" onFinish={handleOtpSubmit} autoComplete="off">
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
                            loading={loading}
                            className="btn-primary"
                        >
                            Xác thực
                        </Button>
                    </Form.Item>
                    <div className="otp-timer">
                        <Text>
                            Mã OTP hết hạn sau {Math.floor(otpCountdown / 60)}:
                            {String(otpCountdown % 60).padStart(2, '0')}
                        </Text>

                        {canResend ? (
                            <Button type="link" className="btn-link" onClick={handleResendOTP}>
                                Gửi lại
                            </Button>
                        ) : (
                            <span className="text-disabled">Gửi lại</span>
                        )}
                    </div>
                </Form>
            ),
        },
        {
            title: 'Đặt mật khẩu mới',
            content: (
                <Form layout="vertical" onFinish={handleResetPassword} autoComplete="off">
                    <Form.Item
                        name="newPassword"
                        label="Mật khẩu mới"
                        rules={[
                            { required: true, message: "Vui lòng nhập mật khẩu mới!" },
                            { min: 6, message: "Mật khẩu ít nhất 6 ký tự!" },
                            {
                                pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&.])[A-Za-z\d@$!%*?&.]{6,}$/,
                                message: "Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt!",
                            },
                        ]}
                    >
                        <Input.Password
                            prefix={<LockOutlined className="icon-primary" />}
                            placeholder="Nhập mật khẩu mới"
                            size="large"
                            className="input-primary"
                        />
                    </Form.Item>
                    <Form.Item
                        name="confirmPassword"
                        label="Xác nhận mật khẩu"
                        dependencies={["newPassword"]}
                        rules={[
                            { required: true, message: "Vui lòng xác nhận mật khẩu!" },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue("newPassword") === value) {
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
                    <Form.Item>
                        <Button
                            type="primary"
                            htmlType="submit"
                            loading={loading}
                            className="btn-primary"
                        >
                            Đặt mật khẩu
                        </Button>
                    </Form.Item>
                </Form>
            ),
        },
    ];

    return (
        <div className="forgot-container">
            <Card className="forgot-card" styles={{ body: { padding: 0 } }}>
                <div className="forgot-inner">
                    <div className="forgot-left">
                        <img src={bg1} alt="Forgot password" className="forgot-image" />
                    </div>

                    <div className="forgot-right">
                        <div className="forgot-form">
                            <div className="forgot-header">
                                <Title level={2} className="title-primary">
                                    UTE Logistics
                                </Title>
                                <Text type="secondary">
                                    Làm theo các bước để đặt lại mật khẩu
                                </Text>
                            </div>

                            <Steps current={currentStep} className="forgot-steps">
                                {steps.map((item) => (
                                    <Steps.Step key={item.title} title={item.title} />
                                ))}
                            </Steps>

                            {steps[currentStep].content}

                            <div className="forgot-footer">
                                <Text>
                                    Nhớ mật khẩu?{' '}
                                    <Link to="/login" className="link-primary">
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

export default ForgotPassword;