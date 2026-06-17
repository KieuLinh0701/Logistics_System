import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
  total: number;
}

const Actions: React.FC<Props> = ({ onExport, total }) => {
  return (
    <Space align="center">
      <Button
        className="success-button"
        icon={<FileExcelOutlined />}
        onClick={onExport}
        disabled={total === 0}
      >
        Xuất Excel
      </Button>
    </Space>
  );
};

export default Actions;