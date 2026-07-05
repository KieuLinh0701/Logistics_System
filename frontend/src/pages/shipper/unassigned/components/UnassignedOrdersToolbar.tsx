import React from "react";
import { Button } from "antd";
import { ReloadOutlined } from "@ant-design/icons";

interface UnassignedOrdersToolbarProps {
  onRefresh: () => void;
}

const UnassignedOrdersToolbar: React.FC<UnassignedOrdersToolbarProps> = ({ onRefresh }) => {
  return (
    <div className="shipper-filter-panel shipper-filter-panel--end">
      <div className="shipper-filter-actions">
        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
          Làm mới
        </Button>
      </div>
    </div>
  );
};

export default UnassignedOrdersToolbar;
