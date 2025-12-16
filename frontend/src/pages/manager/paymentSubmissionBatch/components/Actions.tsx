import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined, PlusOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
  onAdd: () => void;
}

const Actions: React.FC<Props> = ({ onExport, onAdd }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<PlusOutlined />}
        onClick={onAdd}
      >
        Tạo phiên đối soát
      </Button>
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