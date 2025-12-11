import React, { useState, useEffect } from 'react';
import { Form, Input, Button, message, Card, Typography } from 'antd';
import { LockOutlined, MailOutlined } from '@ant-design/icons';
import OtpInput from '../../../../components/common/input/OtpInput';
import { getCurrentAccount } from '../../../../utils/authUtils';
import userApi from '../../../../api/userApi';

const { Text } = Typography;

const maskEmail = (email: string): string => {
    if (!email) return '';
    const [name, domain] = email.split('@');
    if (!domain) return email;
    const maskedName = name.length > 1 ? `${name[0]}***` : `${name}***`;
    return `${maskedName}@${domain}`;
};

const AddressSettings: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [otpLoading, setOtpLoading] = useState(false);
    const account = getCurrentAccount();
    const [currentEmail, setCurrentEmail] = useState<string>(account?.email || '');
    const [pendingEmail, setPendingEmail] = useState<string>(''); // email đang chờ xác thực
    const [step, setStep] = useState(0);
    const [otpCountdown, setOtpCountdown] = useState(0);
    const [canResend, setCanResend] = useState(false);
    const [form] = Form.useForm();

    // Đếm ngược thời gian OTP
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

    // Gửi OTP xác thực đổi email
    const handleSendOtp = async (values: { newEmail: string; password: string }) => {
        setLoading(true);
        try {
            const result = await userApi.sendEmailUpdateOTP(values);

            if (result.success) {
                setPendingEmail(values.newEmail);
                setStep(1);
                setOtpCountdown(300);
                setCanResend(false);
                message.success('Mã OTP đã được gửi đến email mới!');
            } else {
                message.error(result.message || 'Gửi mã OTP đến email mới thất bại!');
            }
        } catch (error) {
            console.error('Update Email error:', error);
            message.error('Có lỗi xảy ra khi gửi OTP!');
        } finally {
            setLoading(false);
        }
    };

    // Xác thực OTP và đổi email
    const handleVerifyOtp = async (values: { otp: string }) => {
        setOtpLoading(true);
        try {
            const result = await userApi.verifyEmailUpdateOTP({
                newEmail: pendingEmail,
                otp: values.otp,
            });

            if (result.success) {
                message.success('Cập nhật email thành công!');
                setCurrentEmail(pendingEmail);
                setPendingEmail('');
                setStep(0);
                form.resetFields();
            } else {
                message.error(result.message || 'Cập nhật email thất bại!');
            }
        } catch (err) {
            console.error(err);
            message.error('Có lỗi xảy ra, vui lòng thử lại!');
        } finally {
            setOtpLoading(false);
        }
    };

    // Gửi lại OTP
    const handleResendOtp = async () => {
        if (!pendingEmail) {
            message.error('Không có email mới để gửi lại OTP!');
            return;
        }

        setLoading(true);
        try {
            const result = await userApi.sendEmailUpdateOTP({
                newEmail: pendingEmail,
                password: form.getFieldValue('password'),
            });

            if (result.success) {
                setOtpCountdown(300);
                setCanResend(false);
                message.success('Mã OTP đã được gửi lại đến email mới!');
            } else {
                message.error(result.message || 'Gửi lại mã OTP thất bại!');
            }
        } catch (error) {
            console.error('Resend OTP error:', error);
            message.error('Có lỗi xảy ra khi gửi lại OTP!');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="tab-content">
            <Card className="profile-form-card">
                {step === 0 ? (
                    <Form form={form} layout="vertical" onFinish={handleSendOtp}>
                        <Form.Item label="Email hiện tại">
                            <Input
                                value={maskEmail(currentEmail)}
                                disabled
                                className="form-input"
                                size="large"
                            />
                        </Form.Item>

                        <Form.Item
                            label="Email mới"
                            name="newEmail"
                            rules={[
                                { required: true, message: 'Vui lòng nhập email mới!' },
                                { type: 'email', message: 'Email không hợp lệ!' },
                            ]}
                        >
                            <Input
                                prefix={<MailOutlined className="form-input-prefix" />}
                                placeholder="Nhập email mới"
                                className="form-input"
                                size="large"
                            />
                        </Form.Item>

                        <Form.Item
                            label="Mật khẩu hiện tại"
                            name="password"
                            rules={[{ required: true, message: 'Vui lòng nhập mật khẩu hiện tại!' }]}
                        >
                            <Input.Password
                                prefix={<LockOutlined className="form-input-prefix" />}
                                placeholder="Nhập mật khẩu hiện tại"
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
                                Gửi Mã Xác Thực
                            </Button>
                        </Form.Item>
                    </Form>
                ) : (
                    <Form layout="vertical" onFinish={handleVerifyOtp}>
                        <div className="success-message">
                            <Text strong>Mã OTP đã được gửi đến: {maskEmail(pendingEmail)}</Text>
                        </div>

                        <Form.Item
                            label="Mã OTP"
                            name="otp"
                            rules={[
                                { required: true, message: 'Vui lòng nhập mã OTP!' },
                                { len: 6, message: 'Mã OTP phải có 6 ký tự!' },
                            ]}
                        >
                            <OtpInput
                                length={6}
                                onChange={(val) => form.setFieldsValue({ otp: val })}
                            />
                        </Form.Item>

                        <Form.Item>
                            <Button
                                type="primary"
                                htmlType="submit"
                                loading={otpLoading}
                                className="btn-primary"
                            >
                                Xác Thực Email
                            </Button>
                        </Form.Item>

                        <div className="otp-timer">
                            <Text>
                                Mã OTP hết hạn sau {Math.floor(otpCountdown / 60)}:
                                {String(otpCountdown % 60).padStart(2, '0')}
                            </Text>

                            {canResend ? (
                                <Button type="link" className="btn-link" onClick={handleResendOtp}>
                                    Gửi lại OTP
                                </Button>
                            ) : (
                                <span className="text-disabled">Gửi lại OTP</span>
                            )}
                        </div>
                    </Form>
                )}
            </Card>
        </div>
    );
};

export default AddressSettings;