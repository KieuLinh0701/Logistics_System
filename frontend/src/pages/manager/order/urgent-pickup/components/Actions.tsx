import React from "react";
import {Button, Space} from "antd";
import {CheckCircleOutlined, FileExcelOutlined} from "@ant-design/icons";

interface Props {
  onExport: () => void;
  onConfirm: () => void;
  hasSelected: boolean;
  total: number;
}

const Actions: React.FC<Props> = ({ onExport, total, onConfirm, hasSelected }) => {
  return (
    <Space align="center">
        <Button
            className="primary-button"
            icon={<CheckCircleOutlined />}
            onClick={onConfirm}
            disabled={!hasSelected}
        >
            Xác nhận hàng loạt
        </Button>
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