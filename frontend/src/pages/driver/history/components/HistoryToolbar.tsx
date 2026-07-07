import React from "react";
import {Button} from "antd";
import {ReloadOutlined} from "@ant-design/icons";

interface HistoryToolbarProps {
  onReload: () => void;
}

const HistoryToolbar: React.FC<HistoryToolbarProps> = ({ onReload }) => {
  return (
    <div className="list-page-actions">
      <Button icon={<ReloadOutlined />} onClick={onReload}>
        Tải lại
      </Button>
    </div>
  );
};

export default HistoryToolbar;
