import React from 'react';
import { Button, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

interface ActionsProps {
  onAdd: () => void;
}

const Actions: React.FC<ActionsProps> = ({
  onAdd,
}) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAdd}
      >
        Tạo phân công
      </Button>
    </Space>
  );
};

export default Actions;