import React from "react";
import { Breadcrumb, Button, Row, Typography } from "antd";
import { FileExcelOutlined } from "@ant-design/icons";

const { Title } = Typography;

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