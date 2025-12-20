import React, { useState, useEffect } from 'react';
import { DatePicker, Button, Row, Col, Space, Typography } from 'antd';
import dayjs, { Dayjs } from 'dayjs';
import { ClockCircleOutlined } from '@ant-design/icons';

const { RangePicker } = DatePicker;
const { Text } = Typography;

interface DateFilterProps {
  dateRange: [Dayjs, Dayjs] | null;
  onDateRangeChange: (dates: [Dayjs, Dayjs] | null) => void;
}

const DateFilter: React.FC<DateFilterProps> = ({ dateRange, onDateRangeChange }) => {
  const [now, setNow] = useState(dayjs());
  const [activeQuick, setActiveQuick] = useState<'allTime' | 'today' | '7days' | '30days' | null>('allTime');

  useEffect(() => {
    const timer = setInterval(() => setNow(dayjs()), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    quickSetDate('allTime');
  }, []);

  const handleDateRangeChange = (
    dates: [Dayjs | null, Dayjs | null] | null
  ) => {
    if (dates && dates[0] && dates[1]) {
      onDateRangeChange([dates[0], dates[1]]);
      setActiveQuick(null);
    } else {
      onDateRangeChange(null);
    }
  };

  const quickSetDate = (type: 'allTime' | 'today' | '7days' | '30days') => {
    let range: [Dayjs, Dayjs] | null = null;

    if (type === 'today') {
      range = [dayjs().startOf('day'), dayjs().endOf('day')];
    } else if (type === '7days') {
      range = [
        dayjs().subtract(7, 'day').startOf('day'),
        dayjs().endOf('day'),
      ];
    } else if (type === '30days') {
      range = [
        dayjs().subtract(30, 'day').startOf('day'),
        dayjs().endOf('day'),
      ];
    }

    onDateRangeChange(range);
    setActiveQuick(type);
  };

  return (
    <Row gutter={16} align="middle" className="dashboard-date-filter-container">
      <Col>
        <Space size={8} className="dashboard-date-filter-live">
          <ClockCircleOutlined className="dashboard-date-filter-clock-icon" />
          <Text strong className="dashboard-date-filter-live-text">LIVE:</Text>
          <Text className="dashboard-date-filter-time">{now.format('HH:mm:ss DD/MM/YYYY')}</Text>
        </Space>
      </Col>

      <Col flex="auto" className="dashboard-date-filter-right-section">
        <Space>
          <Button
            className={`dashboard-date-filter-btn ${activeQuick === 'allTime' ? 'dashboard-date-filter-btn-active' : ''}`}
            onClick={() => quickSetDate('allTime')}
          >
            All Time
          </Button>
          <Button
            className={`dashboard-date-filter-btn ${activeQuick === 'today' ? 'dashboard-date-filter-btn-active' : ''}`}
            onClick={() => quickSetDate('today')}
          >
            Hôm nay
          </Button>
          <Button
            className={`dashboard-date-filter-btn ${activeQuick === '7days' ? 'dashboard-date-filter-btn-active' : ''}`}
            onClick={() => quickSetDate('7days')}
          >
            7 ngày trước
          </Button>
          <Button
            className={`dashboard-date-filter-btn ${activeQuick === '30days' ? 'dashboard-date-filter-btn-active' : ''}`}
            onClick={() => quickSetDate('30days')}
          >
            30 ngày trước
          </Button>

          <RangePicker
            value={dateRange}
            onChange={handleDateRangeChange}
            className="dashboard-date-filter-picker"
          />
        </Space>
      </Col>
    </Row>
  );
};

export default DateFilter;