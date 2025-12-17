import React from "react";
import { Button, Space } from "antd";
import { FileExcelOutlined, ScheduleOutlined } from "@ant-design/icons";

interface Props {
  onExport: () => void;
  onSetSchedule: () => void;
}

const Actions: React.FC<Props> = ({ onExport, onSetSchedule }) => {
  return (
    <Space align="center">
      <Button
        className="primary-button"
        icon={<ScheduleOutlined />}
        onClick={onSetSchedule}
      >
        Đổi lịch đối soát
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