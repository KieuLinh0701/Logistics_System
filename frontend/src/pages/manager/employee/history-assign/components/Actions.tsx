import React from 'react';
import { Button, Space } from 'antd';
import { FileExcelOutlined } from '@ant-design/icons';

interface ActionsProps {
  onExport: () => void;
}

const Actions: React.FC<ActionsProps> = ({
  onExport,
}) => {
  return (
    <Space align="center">
      <Button
        className="success-button"
        icon={<FileExcelOutlined />}
        onClick={onExport}
      >
        Xuất Excel
      </Button>
    </Space>
  );
};

export default Actions;