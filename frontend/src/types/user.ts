export interface UpadtePasswordData {
  oldPassword: string;
  newPassword: string;
}

export interface UpadteEmailData {
  newEmail: string;
  password: string;
}

export interface VerifyEmailUpdateOTPData {
  newEmail: string;
  otp: string;
}

export interface AdminUser {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  role?: string;
  roles?: string[];
  rolesIds?: number[];
  roleId?: number;
  isActive: boolean;
  isVerified: boolean;
  images?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface User {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  fullName?: string;
  phoneNumber?: string;
  role?: string;
  roleId?: number;
  isActive?: boolean;
  isVerified?: boolean;
  images?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserEmployeeSearchRequest {
    page: number;
    limit: number;
    search?: string;
    startDate?: string;
    endDate?: string;
}

export interface UserEmployeeSearchRequest {
    page: number;
    limit: number;
    search?: string;
    active?: boolean;
    sort?: string;
    startDate?: string;
    endDate?: string;
}

export interface UserFormValues {
    email?: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
    roleId?: number;
}