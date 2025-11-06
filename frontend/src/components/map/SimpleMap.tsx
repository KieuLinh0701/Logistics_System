import React from 'react';
import { Card, List, Tag, Space, Typography } from 'antd';
import { EnvironmentOutlined, PhoneOutlined, DollarOutlined } from '@ant-design/icons';

interface DeliveryStop {
  id: number;
  trackingNumber: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  codAmount: number;
  priority: 'normal' | 'urgent';
  serviceType: string;
  estimatedTime: string;
  status: 'pending' | 'in_progress' | 'completed' | 'failed';
  coordinates: {
    lat: number;
    lng: number;
  };
  distance: number;
  travelTime: number;
}

interface SimpleMapProps {
  deliveryStops: DeliveryStop[];
}

const { Text } = Typography;

const SimpleMap: React.FC<SimpleMapProps> = ({ deliveryStops }) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'pending': return 'default';
      case 'in_progress': return 'processing';
      case 'completed': return 'success';
      case 'failed': return 'error';
      default: return 'default';
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'pending': return 'Chờ giao';
      case 'in_progress': return 'Đang giao';
      case 'completed': return 'Hoàn thành';
      case 'failed': return 'Thất bại';
      default: return status;
    }
  };

  const getPriorityColor = (priority: string) => {
    return priority === 'urgent' ? 'red' : 'default';
  };

  const getPriorityText = (priority: string) => {
    return priority === 'urgent' ? 'Ưu tiên' : 'Bình thường';
  };

  const openInGoogleMaps = (address: string) => {
    const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(address)}`;
    window.open(mapsUrl, '_blank');
  };

  const openRouteInGoogleMaps = () => {
    if (deliveryStops.length === 0) return;
    
    const addresses = deliveryStops.map(stop => stop.recipientAddress);
    const routeUrl = `https://www.google.com/maps/dir/${addresses.join('/')}`;
    window.open(routeUrl, '_blank');
  };

  return (
    <Card 
      title="Lộ trình giao hàng" 
      style={{ marginBottom: '24px' }}
      extra={
        <Space>
          <a onClick={openRouteInGoogleMaps} style={{ cursor: 'pointer' }}>
            <EnvironmentOutlined /> Xem lộ trình trên Google Maps
          </a>
        </Space>
      }
    >
      <List
        dataSource={deliveryStops}
        renderItem={(stop, index) => (
          <List.Item
            actions={[
              <a 
                key="map" 
                onClick={() => openInGoogleMaps(stop.recipientAddress)}
                style={{ cursor: 'pointer' }}
              >
                <EnvironmentOutlined /> Chỉ đường
              </a>
            ]}
          >
            <List.Item.Meta
              avatar={
                <div style={{ 
                  width: '40px', 
                  height: '40px', 
                  borderRadius: '50%', 
                  backgroundColor: stop.status === 'completed' ? '#52c41a' : 
                                 stop.status === 'in_progress' ? '#1890ff' : '#faad14',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'white',
                  fontWeight: 'bold',
                  fontSize: '16px'
                }}>
                  {index + 1}
                </div>
              }
              title={
                <Space>
                  <Text strong>{stop.recipientName}</Text>
                  <Tag color={getStatusColor(stop.status)}>
                    {getStatusText(stop.status)}
                  </Tag>
                  <Tag color={getPriorityColor(stop.priority)}>
                    {getPriorityText(stop.priority)}
                  </Tag>
                </Space>
              }
              description={
                <Space direction="vertical" size={0}>
                  <Text>{stop.recipientAddress}</Text>
                  <Space>
                    <PhoneOutlined style={{ color: '#666' }} />
                    <Text style={{ fontSize: '12px' }}>{stop.recipientPhone}</Text>
                    {stop.codAmount > 0 && (
                      <>
                        <DollarOutlined style={{ color: '#f50' }} />
                        <Text style={{ fontSize: '12px', color: '#f50' }}>
                          {stop.codAmount.toLocaleString()}đ
                        </Text>
                      </>
                    )}
                  </Space>
                  <Text type="secondary" style={{ fontSize: '12px' }}>
                    Dịch vụ: {stop.serviceType} | Thời gian ước tính: {stop.estimatedTime}
                  </Text>
                </Space>
              }
            />
          </List.Item>
        )}
      />
    </Card>
  );
};

export default SimpleMap;