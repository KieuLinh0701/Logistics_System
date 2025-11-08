export interface User {
  id: number;
  fullName: string;
  images: string;
}

export interface Account {
  email: string;
  phoneNumber: string;
}

export interface LoginData {
  identifier: string;
  password: string;
}

export interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
}

export interface VerifyOTPData {
  email: string;
  otp: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
}

export interface AuthResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
}