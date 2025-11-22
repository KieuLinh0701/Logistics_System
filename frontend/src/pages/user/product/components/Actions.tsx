import React from 'react';
import { Button, Space, Upload } from 'antd';
import { PlusOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons';

interface ActionsProps {
  onAdd: () => void;
  onImportExcel: (file: File) => boolean | Promise<boolean>;
  onDownloadTemplate: () => void;
}

const Actions: React.FC<ActionsProps> = ({
  onAdd,
  onImportExcel,
  onDownloadTemplate,
}) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAdd}
      >
        Thêm sản phẩm
      </Button>
      <Upload beforeUpload={onImportExcel} showUploadList={false}>
        <Button className="success-button" icon={<UploadOutlined />}>
          Nhập từ Excel
        </Button>
      </Upload>
      <Button
        className="warning-button"
        icon={<DownloadOutlined />}
        onClick={onDownloadTemplate}
      >
        File mẫu
      </Button>
    </Space>
  );
};

export default Actions;