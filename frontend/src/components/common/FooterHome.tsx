import React from 'react';
import { Layout, Row, Col, Typography, Divider } from 'antd';
import { Link } from 'react-router-dom';
import { EnvironmentOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import './FooterHome.css'; 

const { Footer: AntFooter } = Layout;
const { Text, Title } = Typography;

const aboutLinks = [
  { key: 'about', path: '/info/company', text: 'Về chúng tôi' },
  { key: 'contact', path: '/info/contact', text: 'Liên hệ' },
  { key: 'consulting', path: '/consulting', text: 'Tư vấn' },
];

const AboutSection = () => (
  <div>
    <Title level={5} className="footer-section-title">Về Chúng Tôi</Title>
    <div className="footer-link-list">
      {aboutLinks.map(link => (
        <Link key={link.key} to={link.path} className="footer-link">
          {link.text}
        </Link>
      ))}
    </div>
  </div>
);

const ContactSection = () => (
  <div>
    <Title level={5} className="footer-section-title">Liên Hệ</Title>
    <div className="contact-info">
      <Text className="footer-text">
        <EnvironmentOutlined className="footer-icon" /> 
        01 Đ. Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP. Hồ Chí Minh
      </Text>
      <Text className="footer-text">
        <MailOutlined className="footer-icon" /> 
        kieulinh@gmail.com
      </Text>
      <Text className="footer-text">
        <PhoneOutlined className="footer-icon" /> 
        +84 123 4556 789
      </Text>
    </div>
  </div>
);

const FooterHome: React.FC = () => {
  return (
    <AntFooter className="footer" role="contentinfo">
      <div className="footer-container">
        <Row>
          <Col>
            <Title level={2} className="footer-title">UTE Logistics</Title>
          </Col>
        </Row>

        <Divider className="footer-divider" />

        <Row justify="start" gutter={[48, 32]} align="top">
          <Col xs={24} md={10}>
            <Text className="footer-description">
              Chúng tôi cung cấp dịch vụ vận chuyển chất lượng cao, uy tín và tận tâm, 
              luôn mang lại giá trị tốt nhất cho khách hàng với đội ngũ chuyên nghiệp 
              và hệ thống hiện đại.
            </Text>
          </Col>

          <Col xs={24} md={6}>
            <AboutSection />
          </Col>

          <Col xs={24} md={8}>
            <ContactSection />
          </Col>
        </Row>

        <Row justify="center" className="footer-bottom">
          <Text className="footer-copyright">
            © 2025 UTE Logistics. All rights reserved.
          </Text>
        </Row>
      </div>
    </AntFooter>
  );
};

export default FooterHome;