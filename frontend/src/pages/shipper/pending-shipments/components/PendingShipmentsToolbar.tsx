import React from "react";
import {Button} from "antd";
import {ReloadOutlined} from "@ant-design/icons";

interface PendingShipmentsToolbarProps {
  onRefresh: () => void;
}

const PendingShipmentsToolbar: React.FC<PendingShipmentsToolbarProps> = ({ onRefresh }) => {
  return (
    <div className="shipper-filter-panel">
      <div className="shipper-filter-grow" />
      <div className="shipper-filter-actions">
        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default PendingShipmentsToolbar;
