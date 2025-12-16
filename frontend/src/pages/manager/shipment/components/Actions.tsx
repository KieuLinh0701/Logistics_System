import React from 'react';
import { Button, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

interface ActionsProps {
  onAddRequest: () => void;
}

const Actions: React.FC<ActionsProps> = ({
  onAddRequest,
}) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAddRequest}
      >
        Tạo chuyến hàng
      </Button>
    </Space>
  );
};

export default Actions;