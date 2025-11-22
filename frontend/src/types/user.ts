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
  roleId?: number;
  isActive: boolean;
  isVerified: boolean;
  images?: string;
  createdAt?: string;
  updatedAt?: string;
}