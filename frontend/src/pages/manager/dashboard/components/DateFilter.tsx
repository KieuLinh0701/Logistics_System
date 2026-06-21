import React, {useEffect, useState} from 'react';
import {Col, Row, Space, Typography} from 'antd';
import dayjs from 'dayjs';
import {ClockCircleOutlined} from '@ant-design/icons';

const { Text } = Typography;

const DateFilter: React.FC = () => {
  const [now, setNow] = useState(dayjs());

  useEffect(() => {
    const timer = setInterval(() => setNow(dayjs()), 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <Row gutter={16} align="middle" className="manager-dashboard-date-filter-container">
      <Col>
        <Space size={8} className="manager-dashboard-date-filter-live">
          <ClockCircleOutlined className="manager-dashboard-date-filter-clock-icon" />
          <Text strong className="manager-dashboard-date-filter-live-text">LIVE:</Text>
          <Text className="manager-dashboard-date-filter-time">{now.format('HH:mm:ss DD/MM/YYYY')}</Text>
        </Space>
      </Col>
      </Row>
  );
};

export default DateFilter;