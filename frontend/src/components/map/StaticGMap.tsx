import React from 'react';
import { Card } from 'antd';

interface StaticGMapProps {
  query: string; 
  height?: number;
  title?: string;
}

const StaticGMap: React.FC<StaticGMapProps> = ({ query, height = 400, title = 'Bản đồ' }) => {
  const q = encodeURIComponent(query);
  const src = `https://www.google.com/maps?q=${q}&output=embed`;

  return (
    <Card title={title} style={{ marginBottom: '24px' }}>
      <div style={{ width: '100%', height }}>
        <iframe
          title={query}
          src={src}
          width="100%"
          height="100%"
          style={{ border: 0 }}
          allowFullScreen
          loading="lazy"
          referrerPolicy="no-referrer-when-downgrade"
        />
      </div>
    </Card>
  );
};

export default StaticGMap;




