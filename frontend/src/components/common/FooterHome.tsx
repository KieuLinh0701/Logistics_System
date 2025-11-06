import React from 'react';
import { Layout, Row, Col, Typography, Divider } from 'antd';
import { Link } from 'react-router-dom';
import { EnvironmentOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { aboutLinks, styles } from '../../style/FooterStyles';

const { Footer: AntFooter } = Layout;
const { Text, Title } = Typography;

// === Component: About ===
const AboutSection = () => (
  <div>
    <Title level={5} style={styles.sectionTitle}>About</Title>
    <div style={styles.linkList}>
      {aboutLinks.map(link => (
        <Link key={link.key} to={link.path} style={styles.link}>
          {link.text}
        </Link>
      ))}
    </div>
  </div>
);

// === Component: Contact ===
const ContactSection = () => (
  <div>
    <Title level={5} style={styles.sectionTitle}>Contact</Title>
    <Text style={styles.text}>
      <EnvironmentOutlined style={styles.icon} /> 01 Đ. Võ Văn Ngân, Linh Chiểu, Thủ Đức, TP. Hồ Chí Minh, Việt Nam
    </Text><br />
    <Text style={styles.text}>
      <MailOutlined style={styles.icon} /> kieulinh@gmail.com
    </Text><br />
    <Text style={styles.text}>
      <PhoneOutlined style={styles.icon} /> +84 123 4556 789
    </Text>
  </div>
);

// === Component: main ===
const FooterHome: React.FC = () => {
  return (
    <AntFooter style={styles.footer} role="contentinfo">
      {/* Title trên cùng */}
      <Row>
        <Col>
          <Title level={3} style={styles.title}>UTE Logistics</Title>
        </Col>
      </Row>

      {/* Divider */}
      <Divider style={styles.divider} />

      {/* 3 cột nội dung */}
      <Row justify="start" gutter={[48, 32]} align="top">
        {/* Mô tả */}
        <Col xs={24} sm={24} md={10}>
          <Text style={styles.text}>
            Chúng tôi cung cấp dịch vụ chất lượng cao, uy tín và<br />
            tận tâm, luôn mang lại giá trị tốt nhất cho khách hàng.
          </Text>
        </Col>

        {/* About */}
        <Col xs={24} sm={24} md={6}>
          <AboutSection />
        </Col>

        {/* Contact */}
        <Col xs={24} sm={24} md={6}>
          <ContactSection />
        </Col>
      </Row>

      {/* Footer nhỏ */}
      <Row justify="center" style={{ marginTop: '40px' }}>
        <Text style={styles.subText}>© 2025 My Website. All rights reserved.</Text>
      </Row>
    </AntFooter>
  );
};

FooterHome.displayName = 'FooterHome';
export default FooterHome;