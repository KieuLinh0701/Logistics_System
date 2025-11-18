import axiosClient from "./axiosClient";

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: string;
  roleId?: number;
  isActive: boolean;
  isVerified: boolean;
  images?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface OfficeAddress {
  id?: number;
  wardCode?: number;
  cityCode?: number;
  detail?: string;
}

export interface Office {
  id: number;
  code: string;
  postalCode?: string;
  name: string;
  email?: string;
  phoneNumber?: string;
  type?: string;
  status?: string;
  latitude?: number;
  longitude?: number;
  openingTime?: string;
  closingTime?: string;
  capacity?: number;
  notes?: string;
  address?: OfficeAddress | null;
  createdAt: string;
}

export interface Vehicle {
  id: number;
  licensePlate: string;
  type: string;
  capacity: number;
  status: string;
  description?: string;
  officeId?: number;
  office?: {
    id: number;
    name: string;
  };
  createdAt: string;
}

export interface Order {
  id: number;
  trackingNumber: string;
  senderName: string;
  recipientName: string;
  status: string;
  totalFee: number;
  createdAt: string;
}

export interface Pagination {
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface ListResponse<T> {
  data: T[];
  pagination: Pagination;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface CreateOfficePayload {
  code: string;
  postalCode?: string;
  name: string;
  latitude: number;
  longitude: number;
  email?: string;
  phoneNumber?: string;
  openingTime?: string;
  closingTime?: string;
  type?: string;
  status?: string;
  capacity?: number;
  notes?: string;
  wardCode: number;
  cityCode: number;
  detailAddress: string;
}

export interface UpdateOfficePayload extends Partial<CreateOfficePayload> {}

const adminApi = {
  // USER MANAGEMENT
  listUsers: async (params: { page?: number; limit?: number; search?: string }) => {
    const response = await axiosClient.get<ApiResponse<ListResponse<User>>>(
      "/admin/users",
      { params }
    );
    return response;
  },

  getUserById: async (id: number) => {
    const response = await axiosClient.get<ApiResponse<User>>(
      `/admin/users/${id}`
    );
    return response;
  },

  createUser: async (data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    roleId: number;
    isActive?: boolean;
  }) => {
    const response = await axiosClient.post<ApiResponse<User>>(
      "/admin/users",
      data
    );
    return response;
  },

  updateUser: async (id: number, data: {
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
    password?: string;
    roleId?: number;
    isActive?: boolean;
  }) => {
    const response = await axiosClient.put<ApiResponse<User>>(
      `/admin/users/${id}`,
      data
    );
    return response;
  },

  deleteUser: async (id: number) => {
    const response = await axiosClient.delete<ApiResponse<null>>(
      `/admin/users/${id}`
    );
    return response;
  },

  // OFFICE MANAGEMENT
  listOffices: async (params: { page?: number; limit?: number; search?: string }) => {
    const response = await axiosClient.get<ApiResponse<ListResponse<Office>>>(
      "/admin/offices",
      { params }
    );
    return response;
  },

  getOfficeById: async (id: number) => {
    const response = await axiosClient.get<ApiResponse<Office>>(`/admin/offices/${id}`);
    return response;
  },

  createOffice: async (data: CreateOfficePayload) => {
    const response = await axiosClient.post<ApiResponse<Office>>(
      "/admin/offices",
      data
    );
    return response;
  },

  updateOffice: async (id: number, data: UpdateOfficePayload) => {
    const response = await axiosClient.put<ApiResponse<Office>>(
      `/admin/offices/${id}`,
      data
    );
    return response;
  },

  deleteOffice: async (id: number) => {
    const response = await axiosClient.delete<ApiResponse<null>>(`/admin/offices/${id}`);
    return response;
  },

  // VEHICLE MANAGEMENT
  listVehicles: async (params: { page?: number; limit?: number; search?: string }) => {
    const response = await axiosClient.get<ApiResponse<ListResponse<Vehicle>>>(
      "/admin/vehicles",
      { params }
    );
    return response;
  },

  createVehicle: async (data: {
    licensePlate: string;
    type: string;
    capacity: number;
    status: string;
    description?: string;
    officeId: number;
  }) => {
    const response = await axiosClient.post<ApiResponse<Vehicle>>(
      "/admin/vehicles",
      data
    );
    return response;
  },

  updateVehicle: async (id: number, data: {
    type?: string;
    capacity?: number;
    status?: string;
    description?: string;
    officeId?: number;
  }) => {
    const response = await axiosClient.put<ApiResponse<Vehicle>>(
      `/admin/vehicles/${id}`,
      data
    );
    return response;
  },

  deleteVehicle: async (id: number) => {
    const response = await axiosClient.delete<ApiResponse<null>>(
      `/admin/vehicles/${id}`
    );
    return response;
  },

  // ORDER MANAGEMENT
  listOrders: async (params: { page?: number; limit?: number; search?: string; status?: string }) => {
    const response = await axiosClient.get<ApiResponse<ListResponse<Order>>>(
      "/admin/orders",
      { params }
    );
    return response;
  },

  updateOrderStatus: async (id: number, status: string) => {
    const response = await axiosClient.put<ApiResponse<Order>>(
      `/admin/orders/${id}/status`,
      { status }
    );
    return response;
  },

  deleteOrder: async (id: number) => {
    const response = await axiosClient.delete<ApiResponse<null>>(
      `/admin/orders/${id}`
    );
    return response;
  },
};

export default adminApi;


