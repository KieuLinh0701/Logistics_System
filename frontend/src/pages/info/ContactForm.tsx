import React, { useEffect, useState } from "react";
import { Form, Input, Button, Typography, Card, Row, Col, message, Select } from "antd";
import { SendOutlined, PhoneOutlined, MailOutlined, EnvironmentOutlined } from "@ant-design/icons";
import axios from "axios";
import "./ContactForm.css";
import contactFormImage from "../../assets/images/contactForm.jpg";
import HeaderHome from "../../components/common/HeaderHome";
import FooterHome from "../../components/common/FooterHome";
import type { Office } from "../../types/office";
import officeApi from "../../api/officeApi";
import locationApi from "../../api/locationApi";
import Paragraph from "antd/es/typography/Paragraph";

const { Option } = Select;
const { Title, Text } = Typography;
const { TextArea } = Input;

interface ContactFormData {
  name: string;
  email: string;
  phone: string;
  subject: string;
  message: string;
}

const ContactForm: React.FC = () => {
  const [form] = Form.useForm();
  const [office, setOffice] = useState<Office | null>(null);
  const [loading, setLoading] = useState(true);
  const [address, setAddress] = useState("");

  useEffect(() => {
    const fetchAddress = async () => {
      if (!office) return;
      try {
        const city = await locationApi.getCityNameByCode(office.cityCode);
        const ward = await locationApi.getWardNameByCode(office?.cityCode, office?.wardCode);

        const parts = [office.detail, ward, city].filter(Boolean);
        setAddress(parts.join(", "));
      } catch (error) {
        console.error("Error getting address:", error);
        setAddress(office.detail);
      }
    };

    fetchAddress();
  }, [office]);

  useEffect(() => {
    const fetchCompanyInfo = async () => {
      try {
        const result = await officeApi.getHeadOffice();
        if (result.success) {
          setOffice(result.data);
        }
      } catch (error) {
        message.error("Không thể tải thông tin công ty");
      } finally {
        setLoading(false);
      }
    };

    fetchCompanyInfo();
  }, []);

  const handleSubmit = async (values: ContactFormData) => {
    setLoading(true);
    try {
      const response = await axios.post('/api/public/contact', values);
      if ((response.data as any).success) {
        message.success((response.data as any).message);
        form.resetFields();
      } else {
        message.error("Có lỗi xảy ra khi gửi liên hệ");
      }
    } catch (error) {
      message.error("Có lỗi xảy ra khi gửi liên hệ");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="contact-form-page">
      <HeaderHome />

      {/* Hero Section */}
      <div
        className="contact-form-hero"
        style={{
          backgroundImage: `url(${contactFormImage})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
        }}
      >
        <div className="contact-form-hero-overlay">
          <div className="contact-form-hero-content">
            <Title level={2} className="contact-form-hero-title">
              Liên hệ & Hỗ trợ
            </Title>
            <Paragraph className="contact-form-hero-subtitle">
              Liên hệ với chúng tôi hoặc tham khảo các câu hỏi thường gặp để được hỗ trợ.
            </Paragraph>
          </div>
        </div>
      </div>

      <div className="contact-form-container">
        <div className="contact-form-header">
          <Title level={2}>Liên hệ & hỗ trợ</Title>
        </div>

        <div className="contact-form-content">
          <Row gutter={[32, 32]}>
            {/* Left Side - Contact Form */}
            <Col xs={24} lg={16}>
              <Card className="contact-form-card" title="Gửi yêu cầu hỗ trợ">
                <Form form={form} layout="vertical" onFinish={handleSubmit} className="contact-form-form">
                  <Row gutter={16}>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="name"
                        label="Họ và tên"
                        rules={[{ required: true, message: "Vui lòng nhập họ tên!" }]}
                      >
                        <Input size="large" placeholder="Nhập họ và tên" />
                      </Form.Item>
                    </Col>

                    <Col xs={24} md={12}>
                      <Form.Item
                        name="email"
                        label="Email"
                        rules={[
                          { required: true, message: "Vui lòng nhập email!" },
                          { type: "email", message: "Email không hợp lệ!" }
                        ]}
                      >
                        <Input size="large" placeholder="Nhập email" />
                      </Form.Item>
                    </Col>
                  </Row>

                  <Row gutter={16}>
                    <Col xs={24} md={12}>
                      <Form.Item
                        name="phone"
                        label="Số điện thoại"
                        rules={[{ required: true, message: "Vui lòng nhập số điện thoại!" }]}
                      >
                        <Input size="large" placeholder="Nhập số điện thoại" />
                      </Form.Item>
                    </Col>

                    <Col xs={24} md={12}>
                      <Form.Item
                        name="subject"
                        label="Chủ đề"
                        rules={[{ required: true, message: "Vui lòng chọn chủ đề!" }]}
                      >
                        <Select size="large" placeholder="Chọn chủ đề">
                          <Option value="general">Thông tin chung</Option>
                          <Option value="shipping">Vận chuyển</Option>
                          <Option value="billing">Thanh toán</Option>
                          <Option value="complaint">Khiếu nại</Option>
                          <Option value="support">Hỗ trợ kỹ thuật</Option>
                          <Option value="other">Khác</Option>
                        </Select>
                      </Form.Item>
                    </Col>
                  </Row>

                  <Form.Item
                    name="message"
                    label="Nội dung"
                    rules={[{ required: true, message: "Vui lòng nhập nội dung!" }]}
                  >
                    <TextArea
                      rows={6}
                      placeholder="Nhập nội dung chi tiết..."
                      className="contact-form-textarea"
                    />
                  </Form.Item>

                  <Form.Item>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      size="large"
                      icon={<SendOutlined />}
                      className="contact-submit-button"
                    >
                      Gửi yêu cầu
                    </Button>
                  </Form.Item>
                </Form>
              </Card>
            </Col>

            {/* Right Side - Contact Information */}
            <Col xs={24} lg={8}>
              <Card className="contact-info-card" title="Thông tin liên hệ">
                {/* Hotline */}
                <div className="contact-info-item">
                  <div className="contact-info-content">
                    <div className="contact-info-icon-wrapper">
                      <PhoneOutlined className="contact-info-icon" />
                    </div>
                    <div className="contact-info-text">
                      <Text className="contact-info-label">Hotline</Text>
                      <Text className="contact-info-value">{office?.phoneNumber}</Text>
                    </div>
                  </div>
                  <Button
                    className="contact-action-button"
                    onClick={() => window.open(`tel:${office?.phoneNumber}`)}>
                    Gọi ngay
                  </Button>
                </div>

                {/* Email */}
                <div className="contact-info-item">
                  <div className="contact-info-content">
                    <div className="contact-info-icon-wrapper">
                      <MailOutlined className="contact-info-icon" />
                    </div>
                    <div className="contact-info-text">
                      <Text className="contact-info-label">Email</Text>
                      <Text className="contact-info-value">{office?.email}</Text>
                    </div>
                  </div>
                  <Button
                    className="contact-action-button"
                    onClick={() => window.open(`mailto:${office?.email}`)}>
                    Gửi email
                  </Button>
                </div>

                {/* Address */}
                <div className="contact-info-item-no-button">
                  <div className="contact-info-content">
                    <div className="contact-info-icon-wrapper">
                      <EnvironmentOutlined className="contact-info-icon" />
                    </div>
                    <div className="contact-info-text">
                      <Text className="contact-info-label">Địa chỉ</Text>
                      <Text className="contact-info-value">{address}</Text>
                    </div>
                  </div>
                </div>

                {/* Working Hours */}
                <div className="working-hours-section">
                  <Title level={5} className="working-hours-title">Giờ làm việc</Title>
                  <Text className="working-hours-text">Tất cả các ngày trong tuần</Text>
                  <Text className="working-hours-text">{office?.openingTime} - {office?.closingTime}</Text>
                </div>
              </Card>
            </Col>
          </Row>
        </div>

        {/* FAQ Section */}
        <Card className="faq-card" title="Câu hỏi thường gặp">
          <Row gutter={[24, 24]}>
            <Col xs={24} md={12}>
              <div className="faq-item">
                <Title level={5} className="faq-question">Làm thế nào để tra cứu đơn hàng?</Title>
                <Text className="faq-answer">
                  Bạn có thể tra cứu đơn hàng bằng mã vận đơn bằng cách sử dụng tính năng{' '}
                  <Button type="link" className="faq-link" onClick={() => window.location.href = '/tracking/order-tracking'}>
                    "Tra cứu đơn hàng"
                  </Button>.
                </Text>
              </div>
            </Col>

            <Col xs={24} md={12}>
              <div className="faq-item">
                <Title level={5} className="faq-question">Thời gian giao hàng là bao lâu?</Title>
                <Text className="faq-answer">
                  Thời gian giao hàng phụ thuộc vào loại dịch vụ và khoảng cách.{' '}
                  <Button type="link" className="faq-link" onClick={() => window.location.href = '/info/services'}>
                    Xem chi tiết dịch vụ
                  </Button>{' '}
                  để biết thêm thông tin.
                </Text>
              </div>
            </Col>

            <Col xs={24} md={12}>
              <div className="faq-item">
                <Title level={5} className="faq-question">Có thể hủy đơn hàng không?</Title>
                <Text className="faq-answer">
                  Bạn có thể hủy đơn hàng trong vòng 2 giờ sau khi tạo đơn.
                  Liên hệ hotline để được hỗ trợ.
                </Text>
              </div>
            </Col>

            <Col xs={24} md={12}>
              <div className="faq-item">
                <Title level={5} className="faq-question">Phí vận chuyển được tính như thế nào?</Title>
                <Text className="faq-answer">
                  Phí vận chuyển được tính dựa trên khối lượng, khoảng cách và
                  loại dịch vụ.{' '}
                  <Button type="link" className="faq-link" onClick={() => window.location.href = '/tracking/shipping-fee'}>
                    Sử dụng công cụ tính phí
                  </Button>{' '}
                  để biết chi tiết.
                </Text>
              </div>
            </Col>
          </Row>
        </Card>
      </div>

      <FooterHome />
    </div>
  );
};

export default ContactForm;