import axios from "axios";
import type { City } from "../types/location";

const locationApi = {
  getCities: async (): Promise<City[]> => {
    const res = await axios.get<City[]>("https://provinces.open-api.vn/api/v2/p/");
    return res.data;
  },
};

export default locationApi;