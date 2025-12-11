export interface ManagerEmployee {
  id: number;
  code: string;
  userFullName: string;
  userPhoneNumber: string;
  userCityCode: number;
  userWardCode: number;
  userDetail: string;
  userRole: string;
  userEmail: string;
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