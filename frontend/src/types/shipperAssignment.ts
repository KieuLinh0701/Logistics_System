import type { ManagerEmployee } from "./employee";

export interface ManagerShipperAssignment {
  id: number;
  wardCode: number;
  cityCode: number;
  startAt: string;
  endAt: string;
  notes: string;
  createdAt: string;
  updatedAt: string;
  employee: ManagerEmployee;
}

export interface ManagerShipperAssignmentEditRequest {
  selectedEmployee?: number;
  wardCode?: number;
  startAt?: string | null;
  endAt?: string | null;
  notes?: string | null;
}

export interface ManagerShipperAssignmentSearchRequest {
  page: number;
  limit: number;
  search?: string;
  sort?: string;
  wardCode?: number;
  startDate?: string;
  endDate?: string;
}