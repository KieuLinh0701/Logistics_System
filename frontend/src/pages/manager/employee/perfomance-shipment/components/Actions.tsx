import React from "react";
import { Button } from "antd";
import { FileExcelOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
}

const Actions: React.FC<Props> = ({ onExport }) => {
  return (
    <Button
      className="success-button"
      icon={<FileExcelOutlined />}
      onClick={onExport}
    >
      Xuất Excel
    </Button>
  );
};

export default Actions;