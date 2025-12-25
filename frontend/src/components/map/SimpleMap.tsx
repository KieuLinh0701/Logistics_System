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
  title?: string;
  deliverOffice?: { id?: number; name?: string; address?: string; latitude?: number; longitude?: number } | null;
}

const { Text } = Typography;

const SimpleMap: React.FC<SimpleMapProps> = ({ deliveryStops, title, deliverOffice = null, }: SimpleMapProps & { showReceive?: boolean }) => {
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

    const destination = deliveryStops[0].recipientAddress || '';

    const tryOpenWithCurrent = async () => {
      try {
        const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
          if (!navigator.geolocation) return reject(new Error('no-geolocation'));
          navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 });
        });
        const origin = `${pos.coords.latitude},${pos.coords.longitude}`;
        const routeUrl = `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`;
        window.open(routeUrl, '_blank');
      } catch (e) {
        // fallback: open route by addresses list
        const addresses = deliveryStops.map(stop => stop.recipientAddress);
        const routeUrl = `https://www.google.com/maps/dir/${addresses.join('/')}`;
        window.open(routeUrl, '_blank');
      }
    };

    tryOpenWithCurrent();
  };

  return (
    <>
      { (typeof (title) !== 'undefined') || (deliveryStops && deliveryStops.length > 0) ? (
        <Card 
          title={title || "Lộ trình nhận hàng"} 
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
                <Text strong>{stop.recipientName}</Text>
              }
              description={
                <Space direction="vertical" size={0}>
                  <Text>{stop.recipientAddress}</Text>
                  <Space>
                    <PhoneOutlined style={{ color: '#666' }} />
                    <Text style={{ fontSize: '12px' }}>{stop.recipientPhone}</Text>
                  </Space>
                </Space>
              }
            />
          </List.Item>
        )}
      />
        </Card>
      ) : null}

      {deliverOffice && (
        <Card
          title="Lộ trình nộp hàng"
          style={{ marginBottom: '24px' }}
          extra={
            <Space>
              <a
                onClick={async () => {
                  // open route from current position to office address
                  const destination = deliverOffice.address || deliverOffice.name || '';
                  try {
                    const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
                      if (!navigator.geolocation) return reject(new Error('no-geolocation'));
                      navigator.geolocation.getCurrentPosition(resolve, reject, { timeout: 5000 });
                    });
                    const origin = `${pos.coords.latitude},${pos.coords.longitude}`;
                    const routeUrl = `https://www.google.com/maps/dir/?api=1&origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`;
                    window.open(routeUrl, '_blank');
                  } catch (e) {
                    const routeUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(destination)}`;
                    window.open(routeUrl, '_blank');
                  }
                }}
                style={{ cursor: 'pointer' }}
              >
                <EnvironmentOutlined /> Xem lộ trình nộp hàng trên Google Maps
              </a>
            </Space>
          }
        >
          <List
            dataSource={[{ ...deliverOffice, id: deliverOffice.id || 0, recipientName: deliverOffice.name || '', recipientPhone: '', recipientAddress: deliverOffice.address || '', codAmount: 0, priority: 'normal', serviceType: '', estimatedTime: '', status: 'pending', coordinates: { lat: deliverOffice.latitude || 0, lng: deliverOffice.longitude || 0 }, distance: 0, travelTime: 0 }]}
            renderItem={(stop) => (
              <List.Item
                actions={[
                  <a
                    key="map"
                    onClick={() => {
                      const addr = stop.recipientAddress || stop.recipientName || '';
                      const mapsUrl = `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(addr)}`;
                      window.open(mapsUrl, '_blank');
                    }}
                    style={{ cursor: 'pointer' }}
                  >
                    <EnvironmentOutlined /> Chỉ đường
                  </a>
                ]}
              >
                <List.Item.Meta
                  avatar={<div style={{ width: '40px', height: '40px', borderRadius: '50%', backgroundColor: '#faad14', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontWeight: 'bold', fontSize: '16px' }}>B</div>}
                  title={<Space><Text strong>{stop.recipientName}</Text></Space>}
                  description={<Space direction="vertical" size={0}><Text>{stop.recipientAddress}</Text><Text type="secondary" style={{ fontSize: 12 }}>{stop.recipientPhone}</Text></Space>}
                />
              </List.Item>
            )}
          />
        </Card>
      )}
    </>
  );
};

export default SimpleMap;