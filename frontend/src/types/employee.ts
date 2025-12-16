import type { ManagerShipperAssignment } from "./shipperAssignment";

export interface ManagerEmployee {
  id: number;
  code: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: string;
  email: string;
  hireDate: Date;
  shift: string;
  status: string;
}

export interface ManagerEmployeeSearchRequest {
  page?: number; 
  limit?: number; 
  search?: string; 
  sort?: string;
  status?: string;
  role?: string;
  shift?: string;
  startDate?: string;
  endDate?: string;
}

export interface ManagerEmployeeWithShipperAssignments {
  employee: ManagerEmployee;
  assignments: ManagerShipperAssignment[];
}

export interface ManagerEmployeePerformanceData {
  id: number;
  employeeCode: string;
  employeeName: string;
  employeeRole: string;
  employeePhone: string;
  employeeShift: string;
  employeeStatus: string;
  totalShipments: number;
  totalOrders: number;
  completedOrders: number;
  completionRate: number;
  avgTimePerOrder: number;
}