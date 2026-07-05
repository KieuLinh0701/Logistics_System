import React from "react";
import { Button } from "antd";
import { ReloadOutlined } from "@ant-design/icons";

interface ShipmentsToolbarProps {
  onReload: () => void;
}

const ShipmentsToolbar: React.FC<ShipmentsToolbarProps> = ({ onReload }) => {
  return (
    <div className="list-page-actions">
      <Button icon={<ReloadOutlined />} onClick={onReload}>
        Tải lại
      </Button>
    </div>
  );
};

export default ShipmentsToolbar;
