export interface City {
  code: number;
  name: string;
  wards?: Ward[];
}

export interface Ward {
  code: number;
  name: string;
}