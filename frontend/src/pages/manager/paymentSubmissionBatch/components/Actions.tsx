import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
}

const Actions: React.FC<Props> = ({ onExport }) => {
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