import React from 'react';
import { Button } from 'antd';
import { PlusOutlined } from '@ant-design/icons';

interface ActionsProps {
  onAdd: () => void;
  total: number;
}

const Actions: React.FC<ActionsProps> = ({
  onAdd, total
}) => {
  return (
    <Button
      className="primary-button"
      icon={<PlusOutlined />}
      onClick={onAdd}
      disabled={total >= 5}
    >
      Thêm tài khoản
    </Button>
  );
};

export default Actions;