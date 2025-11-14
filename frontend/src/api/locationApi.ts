import axios from "axios";
import type { City, Ward } from "../types/location";

const locationApi = {
  getCities: async (): Promise<City[]> => {
    const res = await axios.get<City[]>("https://provinces.open-api.vn/api/v2/p/");
    return res.data;
  },

  getWardsByCity: async (provinceCode: number): Promise<Ward[]> => {
    const res = await axios.get<City>(
      `https://provinces.open-api.vn/api/v2/p/${provinceCode}?depth=2`
    );
    return res.data.wards || [];
  },

  getCityNameByCode: async (code: number): Promise<string | null> => {
    try {
      const res = await axios.get<City[]>("https://provinces.open-api.vn/api/v2/p/");
      const city = res.data.find(c => c.code === code);
      return city ? city.name : null;
    } catch (error) {
      console.error("Error getting city name:", error);
      return null;
    }
  },

  getWardNameByCode: async (cityCode: number, wardCode: number): Promise<string | null> => {
    try {
      const res = await axios.get<City>(
        `https://provinces.open-api.vn/api/v2/p/${cityCode}?depth=2`
      );
      const ward = res.data.wards?.find(w => w.code === wardCode);
      return ward ? ward.name : null;
    } catch (error) {
      console.error("Error getting ward name:", error);
      return null;
    }
  },
};

export default locationApi;