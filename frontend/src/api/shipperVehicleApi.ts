import axiosClient from "./axiosClient";
import type {
  ShipperVehicleSetting,
  ShipperVehicleSettingRequest,
  ShipperVehicleStatusUpdateRequest,
} from "../types/shipperVehicle";

const unwrap = <T>(res: any): T => {
  if (res?.data !== undefined) return res.data as T;
  return res as T;
};

const shipperVehicleApi = {
  getVehicleSetting: async () => {
    const res = await axiosClient.get<any>("/shipper/vehicle-setting");
    return unwrap<ShipperVehicleSetting>(res);
  },

  updateVehicleSetting: async (body: ShipperVehicleSettingRequest) => {
    const res = await axiosClient.put<any>("/shipper/vehicle-setting", body);
    return unwrap<ShipperVehicleSetting>(res);
  },

  updateVehicleStatus: async (body: ShipperVehicleStatusUpdateRequest) => {
    const res = await axiosClient.put<any>("/shipper/vehicle-setting/status", body);
    return unwrap<ShipperVehicleSetting>(res);
  },
};

export default shipperVehicleApi;
