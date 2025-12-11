import React from "react";
import { Space, Button, Upload } from "antd";
import { PlusOutlined, UploadOutlined, DownloadOutlined, PrinterOutlined } from "@ant-design/icons";

interface Props {
  onAdd: () => void;
  onUpload: (file: File) => boolean;
  onDownloadTemplate: () => void;
  onPrint: () => void;
}

const Actions: React.FC<Props> = ({ onAdd, onUpload, onDownloadTemplate, onPrint }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAdd}
      >
        Tạo đơn hàng
      </Button>
      <Button
        className="success-button"
        icon={<PrinterOutlined />}
        onClick={onPrint}>
        In phiếu
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