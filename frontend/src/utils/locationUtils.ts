import locationApi from "../api/locationApi";

export const formatAddress = async (detail: string, wardCode: number, cityCode: number) => {
    try {
      const cityName = (await locationApi.getCityNameByCode(cityCode)) || "";
      const wardName = (await locationApi.getWardNameByCode(cityCode, wardCode)) || "";
      return [detail, wardName, cityName].filter(Boolean).join(", ");
    } catch (error) {
      console.error("Error formatting address:", error);
      return detail || "";
    }
  };