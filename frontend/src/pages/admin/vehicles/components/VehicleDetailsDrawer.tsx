import React from "react";
import {Button, Descriptions, Drawer} from "antd";
import type {AdminVehicle} from "../../../../types/vehicle";

interface VehicleDetailsDrawerProps {
  open: boolean;
  selectedVehicle: AdminVehicle | null;
  typeText: (value?: string) => string;
  statusText: (value?: string) => string;
  onClose: () => void;
  onViewTrackings: () => void;
}

const VehicleDetailsDrawer: React.FC<VehicleDetailsDrawerProps> = ({
  open,
  selectedVehicle,
  typeText,
  statusText,
  onClose,
  onViewTrackings,
}) => {
  return (
    <Drawer title="Chi tiết phương tiện" placement="right" width={620} open={open} onClose={onClose}>
      {selectedVehicle && (
        <Descriptions column={1} bordered>
          <Descriptions.Item label="Biển số xe">{selectedVehicle.licensePlate}</Descriptions.Item>
          <Descriptions.Item label="Loại xe">{typeText(selectedVehicle.type)}</Descriptions.Item>
          <Descriptions.Item label="Tải trọng">{selectedVehicle.capacity} kg</Descriptions.Item>
          <Descriptions.Item label="Bưu cục">{selectedVehicle.office?.name || "Chưa phân công"}</Descriptions.Item>
          {selectedVehicle.gpsDeviceId && (
            <Descriptions.Item label="Thiết bị GPS">{selectedVehicle.gpsDeviceId}</Descriptions.Item>
          )}
          {(selectedVehicle.lastMaintenanceAt || selectedVehicle.nextMaintenanceDue) && (
            <Descriptions.Item label="Bảo trì">
              <div>
                {selectedVehicle.lastMaintenanceAt && (
                  <div>Lần trước: {new Date(selectedVehicle.lastMaintenanceAt).toLocaleString()}</div>
                )}
                {selectedVehicle.nextMaintenanceDue && (
                  <div>Lần tiếp theo: {new Date(selectedVehicle.nextMaintenanceDue).toLocaleString()}</div>
                )}
              </div>
            </Descriptions.Item>
          )}
          {(selectedVehicle.latitude != null || selectedVehicle.longitude != null) && (
            <Descriptions.Item label="Toạ độ">
              {`${selectedVehicle.latitude ?? "-"} , ${selectedVehicle.longitude ?? "-"}`}
            </Descriptions.Item>
          )}
          <Descriptions.Item label="Hành động">
            <Button size="small" onClick={onViewTrackings}>
              Xem hành trình
            </Button>
          </Descriptions.Item>
          <Descriptions.Item label="Trạng thái">{statusText(selectedVehicle.status)}</Descriptions.Item>
          {selectedVehicle.description && <Descriptions.Item label="Mô tả">{selectedVehicle.description}</Descriptions.Item>}
          <Descriptions.Item label="Ngày tạo">{new Date(selectedVehicle.createdAt).toLocaleString()}</Descriptions.Item>
        </Descriptions>
      )}
    </Drawer>
  );
};

export default VehicleDetailsDrawer;
