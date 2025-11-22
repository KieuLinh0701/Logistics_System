import React from "react";
import { Space, Button, Upload } from "antd";
import { PlusOutlined, UploadOutlined, DownloadOutlined } from "@ant-design/icons";

interface Props {
  onAdd: () => void;
  onUpload: (file: File) => boolean;
  onDownloadTemplate: () => void;
}

const Actions: React.FC<Props> = ({ onAdd, onUpload, onDownloadTemplate }) => {
  return (
    <Space align="center"> 
      <Button 
        className="primary-button"
        icon={<PlusOutlined />} 
        onClick={onAdd}
      >
        Tạo đơn hàng
      </Button>
      <Upload beforeUpload={onUpload} showUploadList={false}>
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